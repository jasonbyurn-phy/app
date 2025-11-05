package com.google.ai.edge.gallery.rag

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ln
import android.util.Log

class RagService(private val context: Context) {

    private data class Doc(
        val id: String,
        val text: String,
        val tf: MutableMap<String, Int>,
        val length: Int
    )

    private val docs = mutableListOf<Doc>()
    private val df = mutableMapOf<String, Int>()
    private var avgDocLen: Double = 0.0

    /** assets/knowledge 텍스트 색인 (없어도 조용히 통과) */
    /** assets/knowledge (또는 artist 폴더 등) 텍스트 색인 — 폴더 재귀 지원 */
    suspend fun indexAssets(folder: String = "knowledge") {
        val am = context.assets
        val names = try {
            am.list(folder) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        for (name in names) {
            val path = "$folder/$name"

            // ✅ 하위 폴더일 경우 — 재귀 호출
            if (am.list(path)?.isNotEmpty() == true) {
                // suspend 함수이므로 withContext로 감싸지 않음
                indexAssets(path)
                continue
            }

            // ✅ 텍스트 파일만 인덱싱
            if (!name.endsWith(".txt", true)) continue

            val text = am.open(path).bufferedReader().use { it.readText() }
            addDocument(path, text)
        }
    }


    /** 단일 문서 추가 */
    suspend fun addDocument(id: String, text: String) = withContext(Dispatchers.Default) {
        val tokens = tokenize(text)
        val tf = mutableMapOf<String, Int>()
        for (t in tokens) tf[t] = (tf[t] ?: 0) + 1
        for (t in tf.keys) df[t] = (df[t] ?: 0) + 1
        val doc = Doc(id = id, text = text, tf = tf, length = tokens.size)
        docs.add(doc)
        avgDocLen = if (docs.isNotEmpty()) docs.map { it.length }.average() else 0.0
    }

    // ── BM25 리트리버 ─────────────────────────────────────────────

    private fun tokenize(text: String): List<String> =
        text.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .trim()
            .split(Regex("""\s+"""))
//            .filter { it.length >= 1 }

    private fun idf(term: String): Double {
        val N = docs.size
        val dfi = df[term] ?: 0
        return ln(((N - dfi + 0.5) / (dfi + 0.5)).coerceAtLeast(1e-6)) + 1.0
    }
// ── Cosine(TF-IDF) 리트리버 ─────────────────────────────────────────────

    /** TF → TF-IDF 벡터 (max-TF 정규화) */
    private fun toTfidf(tf: Map<String, Int>): Map<String, Double> {
        val maxTf = tf.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val out = HashMap<String, Double>(tf.size)
        for ((term, f) in tf) {
            val tfNorm = f.toDouble() / maxTf.toDouble()
            out[term] = tfNorm * idf(term)              // ← 기존 idf() 재사용
        }
        return out
    }

    private fun l2norm(v: Map<String, Double>): Double {
        var s = 0.0
        for (x in v.values) s += x * x
        return kotlin.math.sqrt(s)
    }

    private fun dot(a: Map<String, Double>, b: Map<String, Double>): Double {
        // 작은 쪽을 순회(성능)
        val (small, large) = if (a.size <= b.size) a to b else b to a
        var s = 0.0
        for ((t, av) in small) {
            val bv = large[t] ?: continue
            s += av * bv
        }
        return s
    }

    /** 질문 토큰 vs 문서의 코사인 유사도 (TF-IDF 기반) */
//    private fun cosineScore(qTokens: List<String>, doc: Doc): Double {
//        // 질문 TF
//        val qTf = HashMap<String, Int>()
//        for (t in qTokens) qTf[t] = (qTf[t] ?: 0) + 1
//
//        // TF-IDF 벡터
//        val qVec = toTfidf(qTf)
//        val dVec = toTfidf(doc.tf)
//
//        val qn = l2norm(qVec)
//        val dn = l2norm(dVec)
//        if (qn == 0.0 || dn == 0.0) return 0.0
//
//        return dot(qVec, dVec) / (qn * dn)
//    }

    private fun bm25Score(
        qTokens: List<String>,
        doc: Doc,
        k1: Double = 1.5,
        b: Double = 0.75
    ): Double {
        var score = 0.0
        val dl = doc.length.toDouble()
        val avgdl = if (avgDocLen > 0) avgDocLen else 1.0
        val qtf = qTokens.groupingBy { it }.eachCount()
        for ((t, _) in qtf) {
            val tf = doc.tf[t] ?: 0
            if (tf == 0) continue
            val idfVal = idf(t)
            val numerator = tf * (k1 + 1)
            val denominator = tf + k1 * (1 - b + b * dl / avgdl)
            score += idfVal * (numerator / denominator)
        }
        Log.d("SmartObjectAsk", "bm25Score: $score")
        return score
    }
    private data class ScoredDoc(val doc: Doc, val score: Double)

    // 2) 점수 포함 topK
    private fun topKWithScores(query: String, k: Int): List<ScoredDoc> {
        val qTokens = tokenize(query)
        return docs.asSequence()
            .map { doc -> ScoredDoc(doc, bm25Score(qTokens, doc)) } // cosine → bm25 교체 시 이 줄만
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }

    // 3) 기존 topK는 Doc만 반환(시그니처 유지)
    private fun topK(query: String, k: Int): List<Doc> =
        topKWithScores(query, k).map { it.doc }

    private fun topKScores(query: String, k: Int): List<Double> =
        topKWithScores(query, k).map { it.score }

    private fun defaultPrompt(question: String, contextText: String): String {
        val header = """
            You are a helpful on-device assistant.
            Answer in Korean (한국어로 답하세요).
            Use ONLY the following CONTEXT to answer. If it's not enough, say "맥락에 없음".
        """.trimIndent()
        return if (contextText.isBlank()) {
            "$header\n\nQUESTION:\n$question"
        } else {
            "$header\n\nCONTEXT:\n$contextText\n\nQUESTION:\n$question"
        }
    }


    // buildContext에서 k==0, docs 비었음, 히트 없음, 최고 점수<=0 처리
    suspend fun buildContext(
        question: String,
        k: Int = 5,
        maxChars: Int = 1200
    ): String = withContext(Dispatchers.Default) {
        if (k <= 0) return@withContext ""
        if (docs.isEmpty()) return@withContext ""

        val hits = topKWithScores(question, k)
        if (hits.isEmpty()) return@withContext ""

        val scores = topKScores(question, k)
        val maxScore = scores.first()   // 정렬되어 있으므로 첫 요소가 최대
        if (maxScore <= 0.0) return@withContext ""

        hits.joinToString("\n\n---\n\n") { it.doc.text.take(maxChars) }
    }
}
