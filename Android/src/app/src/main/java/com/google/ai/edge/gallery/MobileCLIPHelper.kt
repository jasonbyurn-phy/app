// app/src/main/java/com/google/ai/edge/gallery/MobileCLIPHelper.kt
package com.google.ai.edge.gallery

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.gallery.rag.MediapipeImageEmbedder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * MobileNetV4 기반 "이미지 전용" 임베딩 헬퍼.
 * - 입력: Bitmap
 * - 출력: 1000차원 L2 정규화 벡터 (FloatArray)
 * - 사전 임베딩: assets/embeddings/all_embeddings_img.{index.txt,bin}
 *
 * 주요 사용:
 *   val helper = MobileCLIPHelper(context)
 *   val q = helper.getImageEmbedding(bitmap)
 *   val top = helper.searchSimilarImages(bitmap, topK = 5)
 *   helper.close()
 */
class MobileCLIPHelper(private val context: Context) : AutoCloseable {

    companion object {
        @Volatile
        private var INSTANCE: MobileCLIPHelper? = null

        fun getInstance(context: Context): MobileCLIPHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobileCLIPHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val TAG = "MobileCLIP"
        private const val MODEL_ASSET = "mobilenetv4_conv_small_e2400_r224_in1k_float32.tflite"

        // 파이썬 스크립트(TARGET_DIM=1000)와 동일해야 함
        private const val IMAGE_EMBED_DIM = 1000

        // 사전 임베딩(.index/.bin) 경로
        private const val INDEX_ASSET = "embeddings/all_embeddings_img.index.txt"
        private const val BIN_ASSET   = "embeddings/all_embeddings_img.bin"


    }

    // MediaPipe → 실패 시 TFLite 폴백을 내부에서 처리
    private val imgEmbedder by lazy {
        MediapipeImageEmbedder(
            context = context,
            modelAssetPath = MODEL_ASSET
        )
    }

    // id → 사전 계산된 임베딩(1000D, L2 정규화)
    private val precomputedEmbeddings: Map<String, FloatArray> by lazy {
        loadPrecomputedEmbeddings()
    }

    /** Bitmap → 1000D L2 정규화 임베딩 */
    fun getImageEmbedding(bitmap: Bitmap): FloatArray {
        return try {
            imgEmbedder.embed(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 임베딩 실패: ${e.message}", e)
            FloatArray(IMAGE_EMBED_DIM) // zero-vector fallback
        }
    }

    /** 내적(=코사인, 벡터는 L2 정규화 가정) */
    fun computeSimilarity(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var s = 0f
        for (i in 0 until n) s += a[i] * b[i]
        return s
    }

    /** 사전 임베딩을 읽어서 맵으로 반환 (키 순서 = index.txt 순서) */
    private fun loadPrecomputedEmbeddings(): Map<String, FloatArray> {
        Log.d(TAG, "📂 사전 임베딩 로드 시작: $INDEX_ASSET / $BIN_ASSET")
        val map = LinkedHashMap<String, FloatArray>()
        try {
            // 1) 인덱스(키) 로드
            val keys = context.assets.open(INDEX_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .useLines { lines ->
                    lines.filter { it.isNotBlank() }
                        .map { it.trim() }
                        .toList()
                }

            // 2) 벡터 바이너리 로드 (float32, little endian)
            val bytes = context.assets.open(BIN_ASSET).readBytes()
            val bb: ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // 3) 행 단위 복원 + 안전 L2 정규화
            for ((i, key) in keys.withIndex()) {
                val v = FloatArray(IMAGE_EMBED_DIM)
                for (d in 0 until IMAGE_EMBED_DIM) {
                    if (bb.remaining() >= 4) {
                        v[d] = bb.float
                    } else {
                        // 파일 손상 방지용: 모자라면 0으로 채움
                        v[d] = 0f
                    }
                }
                l2NormalizeInPlace(v)
                map[key] = v

                if (i < 3) {
                    Log.d(TAG, "[$i] $key first5=${v.take(5)}")
                }
            }

            Log.d(TAG, "🎉 사전 임베딩 로드 완료: ${map.size}개, dim=$IMAGE_EMBED_DIM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 사전 임베딩 로드 실패: ${e.message}", e)
        }
        return map
    }

    /** 질의 이미지로 상위 유사 이미지 검색 (id, score) 내림차순 */
    fun searchSimilarImages(queryBitmap: Bitmap, topK: Int = 5): List<Pair<String, Float>> {
        if (precomputedEmbeddings.isEmpty()) return emptyList()

        val q = getImageEmbedding(queryBitmap) // 1000D L2 normalized
        val scored = ArrayList<Pair<String, Float>>(precomputedEmbeddings.size)
        for ((id, v) in precomputedEmbeddings) {
            val s = computeSimilarity(q, v) // 내적 = 코사인
            scored.add(id to s)
        }
        scored.sortByDescending { it.second }
        return scored.take(topK)
    }

    /** id → 프리컴풋 벡터 (없으면 null) */
    fun getPrecomputedVector(id: String): FloatArray? = precomputedEmbeddings[id]

    /** 전체 프리컴풋 맵 접근(읽기 전용) */
    fun getAllPrecomputed(): Map<String, FloatArray> = precomputedEmbeddings

    private fun l2NormalizeInPlace(v: FloatArray) {
        var s = 0.0
        for (x in v) s += x * x
        val inv = (1.0 / sqrt(max(s, 1e-12))).toFloat()
        for (i in v.indices) v[i] *= inv
    }

    override fun close() {
        runCatching { imgEmbedder.close() }
    }
}
