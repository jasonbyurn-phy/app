package com.google.ai.edge.gallery.rag

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.edge.gallery.MobileCLIPHelper
import java.util.Locale
import kotlin.math.sqrt

/** 임베더 추상화: 어떤 라이브러리든 여기만 구현하면 됨 */
interface ImageEmbedderApi {
    /** 입력 Bitmap -> L2 정규화된 임베딩 (예: 512D/1024D) */
    fun embed(image: Bitmap): FloatArray
}

data class ImageHit(
    val id: String,             // 고유ID (asset path 등)
    val score: Float,           // cosine
    val extra: Map<String, Any?> = emptyMap()
)

class ImageRagIndex(
    private val context: Context,
    private val embedder: ImageEmbedderApi
) {
    private data class Item(
        val id: String,
        val assetPath: String?,
        val vec: FloatArray
    )

    private val items = mutableListOf<Item>()


    /** ⭐ 기존 함수: 개별 이미지 추론 (필요할 때만 사용) */
    suspend fun indexAssetsImages(folder: String = "image_knowledge", maxDim: Int = 512) {
        val am = context.assets
        val names = try {
            am.list(folder) ?: emptyArray()
        } catch (e: Exception) {
            Log.e("ImageRagIndex", "❌ 폴더 읽기 실패: $folder - ${e.message}")
            emptyArray()
        }

        Log.d("ImageRagIndex", "📁 인덱싱 시작: $folder (${names.size}개 항목)")

        for (name in names) {
            val path = "$folder/$name"

            if (!name.contains(".")) {
                Log.d("ImageRagIndex", "📂 하위 폴더 재귀: $path")
                indexAssetsImages(path, maxDim)
                continue
            }

            val lower = name.lowercase(Locale.ROOT)
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
                continue
            }

            try {
                val bmp = am.open(path).use { ins ->
                    decodeDownscaled(ins.readBytes(), maxDim)
                } ?: continue

                val vec = embedder.embed(bmp)

                Log.d("ImageRagIndex", "🖼️ 인덱싱: $path")
                Log.d("ImageRagIndex", "  - 원본 임베딩 처음 3개: ${vec.take(3).joinToString()}")

                val normalized = normalize(vec)

                items += Item(id = path, assetPath = path, vec = normalized)

            } catch (e: Exception) {
                Log.e("ImageRagIndex", "❌ $path 처리 실패: ${e.message}")
            }
        }

        Log.d("ImageRagIndex", "✅ 인덱싱 완료: $folder - 총 ${items.size}개")
    }

    fun addImage(id: String, bitmap: Bitmap): Boolean {
        val v = embedder.embed(bitmap)
        items += Item(id = id, assetPath = null, vec = normalize(v))
        return true
    }

    fun topKByImage(query: Bitmap, k: Int = 5): List<ImageHit> {
        if (items.isEmpty()) {
            Log.w("ImageRagIndex", "⚠️ 인덱스가 비어있음!")
            return emptyList()
        }

        val qv = normalize(embedder.embed(query))

        Log.d("ImageRagIndex", "🔍 검색 시작 - 인덱스 크기: ${items.size}")
        Log.d("ImageRagIndex", "  - 쿼리 임베딩 처음 3개: ${qv.take(3).joinToString()}")

        val pairs = ArrayList<Pair<Item, Float>>(items.size)

        for (it in items) {
//            val s = dot(qv, it.vec)
            val s = cosineSimilarity(qv, it.vec)   // ✅ 수정됨

            if (s > 0.999f) {
                Log.w("ImageRagIndex", "⚠️ 비정상적으로 높은 유사도: ${it.id} = $s")
            }

            pairs.add(Pair(it, s))
        }

        pairs.sortWith { a, b -> b.second.compareTo(a.second) }

        val limit = minOf(k, pairs.size)
        val out = ArrayList<ImageHit>(limit)

        Log.d("ImageRagIndex", "🏆 Top $limit 결과 (Cosine Similarity):")
        for (i in 0 until limit) {
            val p = pairs[i]
            val id = p.first.id
            val score = p.second

            val formattedScore = String.format(Locale.US, "%.3f", score)
            Log.d("ImageRagIndex", "  [$i] $id → $formattedScore")

            when {
                score > 0.99f -> Log.w("ImageRagIndex", "⚠️ 너무 높은 유사도 (중복 또는 동일 이미지 가능): $id = $formattedScore")
                score < 0.2f -> Log.w("ImageRagIndex", "⚠️ 너무 낮은 유사도 (비정상): $id = $formattedScore")
            }

            out.add(ImageHit(id = id, score = score))
        }

        return out
    }
    fun setPrecomputedEmbeddings(precomputed: Map<String, FloatArray>) {
        this.items.clear()
        var added = 0
        for ((id, embedding) in precomputed) {
            if (embedding.any { !it.isFinite() }) {
                Log.w("ImageRagIndex", "⚠️ NaN/Inf embedding skip: $id")
                continue
            }
            val vec = normalize(embedding)
            this.items.add(Item(id = id, assetPath = id, vec = vec))
            added++
        }
        Log.d("ImageRagIndex", "✅ 사전 임베딩 인덱스 구축 완료: $added/${precomputed.size}")
    }

    // ── helpers ─────────────────────────────────────────────
    private fun decodeDownscaled(bytes: ByteArray, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        var maxSide = maxOf(w, h)
        while (maxSide / sample > maxDim) sample *= 2
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts2) ?: return null
        return ensureArgb8888(bmp)
    }

    private fun ensureArgb8888(src: Bitmap): Bitmap =
        if (src.config == Bitmap.Config.ARGB_8888) src else src.copy(Bitmap.Config.ARGB_8888, false)

    private fun normalize(v: FloatArray): FloatArray {
        var s = 0.0
        for (x in v) s += (x * x).toDouble()
        val n = sqrt(s).toFloat().coerceAtLeast(1e-6f)
        val out = FloatArray(v.size)
        for (i in v.indices) out[i] = v[i] / n
        return out
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var s = 0f
        for (i in 0 until n) s += a[i] * b[i]
        return s
    }
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 1e-6f) dot / denom else 0f
    }

}
