// app/src/main/java/com/google/ai/edge/gallery/rag/MediapipeImageEmbedder.kt
package com.google.ai.edge.gallery.rag

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedderResult
import com.google.mediapipe.tasks.components.containers.EmbeddingResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * MobileNetV4 분류 TFLite를 지원하기 위해:
 * 1) MediaPipe ImageEmbedder로 먼저 시도
 * 2) 실패 시 TFLite Interpreter로 폴백하여 1000D 로짓을 임베딩으로 사용(L2 정규화)
 */
class MediapipeImageEmbedder(
    private val context: Context,
    private val modelAssetPath: String = "mobilenetv4_conv_small_e2400_r224_in1k_float32.tflite"
) : ImageEmbedderApi, AutoCloseable {

    // --- MediaPipe 경로 ---
    private var mpEmbedder: ImageEmbedder? = null

    // --- TFLite 폴백 경로 ---
    private var tflite: Interpreter? = null
    private var inH = 224
    private var inW = 224
    private var inC = 3
    private var isNHWC = true
    private var outDim = 1000
    private var outTensorIndex = 0

    private fun tryInitMediapipe() {
        if (mpEmbedder != null) return
        runCatching {
            val base = BaseOptions.builder()
                .setModelAssetPath(modelAssetPath)
                .build()
            val options = ImageEmbedder.ImageEmbedderOptions.builder()
                .setBaseOptions(base)
                .setL2Normalize(true)   // 코사인=내적 바로 사용
                .setQuantize(false)
                .build()
            mpEmbedder = ImageEmbedder.createFromOptions(context, options)
        }.onFailure {
            mpEmbedder = null
        }
    }

    private fun tryInitTflite() {
        if (tflite != null) return
        val model = FileUtil.loadMappedFile(context, modelAssetPath)
        val opt = Interpreter.Options().apply { setNumThreads(4) }
        tflite = Interpreter(model, opt)

        // 입력/출력 shape 파악
        val inShape = tflite!!.getInputTensor(0).shape() // 보통 [1,224,224,3]
        isNHWC = (inShape.size == 4 && inShape[1] > 1 && inShape[2] > 1)
        if (isNHWC) {
            inH = inShape[1]; inW = inShape[2]; inC = inShape[3]
        } else {
            inH = inShape[2]; inW = inShape[3]; inC = inShape[1]
        }
        outTensorIndex = 0
        val outShape = tflite!!.getOutputTensor(outTensorIndex).shape() // 보통 [1,1000]
        outDim = outShape.fold(1) { acc, v -> acc * v }
    }

    override fun embed(image: Bitmap): FloatArray {
        // 1) Mediapipe 시도
        tryInitMediapipe()
        mpEmbedder?.let { emb ->
            val mpImage = BitmapImageBuilder(image).build()
            val result: ImageEmbedderResult = emb.embed(mpImage)
            val er: EmbeddingResult? = result.embeddingResult()
            val list = er?.embeddings() ?: emptyList()
            if (list.isNotEmpty()) {
                val e = list[0]
                e.floatEmbedding()?.let { return it }   // 이미 L2 정규화됨
                e.quantizedEmbedding()?.let { q ->
                    val out = FloatArray(q.size) { i -> q[i] / 127.0f }
                    l2NormalizeInPlace(out)
                    return out
                }
            }
            // fallthrough → TFLite
        }

        // 2) TFLite 폴백
        tryInitTflite()
        val bmp = Bitmap.createScaledBitmap(image, inW, inH, true)

        // [-1,1] 정규화, NHWC 버퍼 구성
        val nhwc = FloatArray(inH * inW * inC)
        var idx = 0
        val px = IntArray(inW * inH)
        bmp.getPixels(px, 0, inW, 0, 0, inW, inH)
        for (p in px) {
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            nhwc[idx++] = r * 2f - 1f
            nhwc[idx++] = g * 2f - 1f
            nhwc[idx++] = b * 2f - 1f
        }

        val input: Any = if (isNHWC) {
            // (1,H,W,C)
            Array(1) { Array(inH) { Array(inW) { FloatArray(inC) } } }.also { buf ->
                var k = 0
                for (y in 0 until inH) for (x in 0 until inW) {
                    buf[0][y][x][0] = nhwc[k++]
                    buf[0][y][x][1] = nhwc[k++]
                    buf[0][y][x][2] = nhwc[k++]
                }
            }
        } else {
            // (1,C,H,W)
            Array(1) { Array(inC) { Array(inH) { FloatArray(inW) } } }.also { buf ->
                var k = 0
                for (y in 0 until inH) for (x in 0 until inW) {
                    val r = nhwc[k++]; val g = nhwc[k++]; val b = nhwc[k++]
                    buf[0][0][y][x] = r
                    buf[0][1][y][x] = g
                    buf[0][2][y][x] = b
                }
            }
        }

        val out = Array(1) { FloatArray(outDim) }
        tflite!!.run(input, out)
        val v = out[0]
        l2NormalizeInPlace(v)
        return v // 1000-D, L2 정규화 완료
    }

    private fun l2NormalizeInPlace(v: FloatArray) {
        var s = 0.0
        for (x in v) s += x * x
        val inv = (1.0 / sqrt(max(s, 1e-12))).toFloat()
        for (i in v.indices) v[i] *= inv
    }

    override fun close() {
        runCatching { mpEmbedder?.close() }
        runCatching { tflite?.close() }
        mpEmbedder = null
        tflite = null
    }
}
