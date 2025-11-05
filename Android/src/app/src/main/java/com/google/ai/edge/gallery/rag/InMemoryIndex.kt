package com.google.ai.edge.gallery.rag

data class RagDoc(val id: String, val text: String, val meta: Map<String, String> = emptyMap())
data class RagHit(val id: String, val text: String, val meta: Map<String, String>, val score: Float)

class InMemoryIndex {
    private val items = mutableListOf<Pair<FloatArray, RagDoc>>()

    fun add(doc: RagDoc, embedding: FloatArray) {
        items += normalize(embedding) to doc
    }
    fun size(): Int = items.size

    fun search(queryEmbedding: FloatArray, k: Int = 5): List<RagHit> {
        val q = normalize(queryEmbedding)
        return items.asSequence()
            .map { (vec, doc) -> RagHit(doc.id, doc.text, doc.meta, dot(q, vec)) }
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }
    private fun normalize(v: FloatArray): FloatArray {
        var s = 0f; for (x in v) s += x * x
        val inv = if (s > 0f) 1f / kotlin.math.sqrt(s) else 1f
        return FloatArray(v.size) { i -> v[i] * inv }
    }
    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f; for (i in a.indices) s += a[i] * b[i]; return s
    }
}
