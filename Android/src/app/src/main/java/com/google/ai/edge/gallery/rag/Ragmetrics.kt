// ─────────────────────────────────────────────────────────────────────────────
// File: app/src/main/java/com/google/ai/edge/gallery/rag/RagMetrics.kt
// Description: Simple retrieval & output adherence metrics utilities
// ─────────────────────────────────────────────────────────────────────────────
package com.google.ai.edge.gallery.rag

object RagMetrics {

    /** Precision@K (returns null if gold not provided) */
    fun precisionAtK(retrievedIds: List<String>, goldIds: Set<String>, k: Int): Double? {
        if (goldIds.isEmpty() || k <= 0) return null
        val topK: Set<String> = retrievedIds.take(k).toSet()
        val hit = topK.count { it in goldIds }
        return hit.toDouble() / k.toDouble()
    }

    /** Recall@K (returns null if gold not provided) */
    fun recallAtK(retrievedIds: List<String>, goldIds: Set<String>, k: Int): Double? {
        if (goldIds.isEmpty() || k <= 0) return null
        val topK: Set<String> = retrievedIds.take(k).toSet()
        val denom = goldIds.size.coerceAtLeast(1)
        val hit = topK.count { it in goldIds }
        return hit.toDouble() / denom.toDouble()
    }

    /** Mean Reciprocal Rank (returns null if gold not provided) */
    fun mrr(retrievedIds: List<String>, goldIds: Set<String>): Double? {
        if (goldIds.isEmpty()) return null
        for (i in retrievedIds.indices) {
            if (retrievedIds[i] in goldIds) return 1.0 / (i + 1).toDouble()
        }
        return 0.0
    }

    // ── Output adherence helpers ──
    fun adherenceChars(output: String, maxChars: Int): Boolean =
        output.length <= maxChars

    fun bulletCount(output: String): Int {
        var c = 0
        for (line in output.lineSequence()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("-")) c++
        }
        return c
    }

    fun adherenceBullets(output: String, maxBullets: Int): Boolean =
        bulletCount(output) <= maxBullets
}
