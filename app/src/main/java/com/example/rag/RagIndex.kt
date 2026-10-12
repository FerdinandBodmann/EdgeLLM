package com.example.rag

/**
 * A single document held in the in-memory retrieval index.
 */
data class IndexedDocument(
    val id: Long,
    val name: String,
    val text: String
)

/**
 * A retrieved passage with its BM25 relevance score and a snippet window.
 */
data class RetrievedPassage(
    val documentId: Long,
    val documentName: String,
    val score: Float,
    val snippet: String
)

/**
 * Minimal, fully-local inverted index with a compact BM25 ranking model.
 *
 * The index lives in memory and is rebuilt whenever the document corpus
 * changes (app start, import, delete). It requires no network, no embedding
 * models and no extra storage on-device, matching the privacy-first ethos of
 * the rest of the app.
 */
object RagIndex {

    private data class Posting(val documentId: Long, val termFrequency: Int)

    private val documents = mutableMapOf<Long, IndexedDocument>()
    private val invertedIndex = mutableMapOf<String, MutableList<Posting>>()
    private var documentLengths = mutableMapOf<Long, Int>()
    private var totalTerms = 0L

    private val avgDocLength: Float
        get() = if (documents.isEmpty()) 1f
        else (totalTerms.toFloat() / documents.size.toFloat()).coerceAtLeast(1f)

    /** Tokenizes a text into normalized terms (lowercase alphanumeric words). */
    fun tokenize(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val terms = ArrayList<String>(text.length / 6)
        val current = StringBuilder()
        for (c in text) {
            if (c.isLetterOrDigit()) {
                current.append(c.lowercaseChar())
            } else if (current.isNotEmpty()) {
                if (current.length >= 2) terms.add(current.toString())
                current.clear()
            }
        }
        if (current.isNotEmpty() && current.length >= 2) terms.add(current.toString())
        return terms
    }

    /** Replaces the whole corpus and rebuilds the index. */
    @Synchronized
    fun rebuild(entries: List<IndexedDocument>) {
        documents.clear()
        invertedIndex.clear()
        documentLengths = mutableMapOf()
        totalTerms = 0L

        entries.forEach { doc ->
            documents[doc.id] = doc
        }

        for ((docId, doc) in documents) {
            val perTerm = HashMap<String, Int>(64)
            for (term in tokenize(doc.text)) {
                perTerm[term] = (perTerm[term] ?: 0) + 1
            }
            var docTermCount = 0
            for ((term, tf) in perTerm) {
                invertedIndex.getOrPut(term) { mutableListOf() }.add(Posting(docId, tf))
                docTermCount += tf
            }
            documentLengths[docId] = docTermCount
            totalTerms += docTermCount
        }
    }

    /** Number of documents currently indexed. */
    @Synchronized
    fun documentCount(): Int = documents.size

    /** True when the corpus contains at least one document. */
    @Synchronized
    fun hasDocuments(): Boolean = documents.isNotEmpty()

    /**
     * Runs a BM25 (k1=1.2, b=0.75) retrieval over the corpus.
     *
     * @param query raw user query.
     * @param topK maximum number of passages to return.
     */
    @Synchronized
    fun search(query: String, topK: Int = 4): List<RetrievedPassage> {
        if (documents.isEmpty()) return emptyList()

        val queryTerms = tokenize(query).toSet()
        if (queryTerms.isEmpty()) return emptyList()

        val n = documents.size
        val scores = HashMap<Long, Float>()
        val matchedTerms = HashMap<Long, MutableSet<String>>()

        for (term in queryTerms) {
            val postings = invertedIndex[term] ?: continue
            val docFreq = postings.size
            val idf = Math.log(1.0 + ((n - docFreq + 0.5) / (docFreq + 0.5))).toFloat()
            for (posting in postings) {
                val docLen = documentLengths[posting.documentId] ?: 1
                val tf = posting.termFrequency.toFloat()
                val denom = tf + 1.2f * (1.0f - 0.75f + 0.75f * (docLen.toFloat() / avgDocLength))
                val score = idf * (tf * (1.2f + 1.0f)) / denom
                scores[posting.documentId] = (scores[posting.documentId] ?: 0f) + score
                matchedTerms.getOrPut(posting.documentId) { mutableSetOf() }.add(term)
            }
        }

        if (scores.isEmpty()) return emptyList()

        val ranked = scores.entries
            .sortedByDescending { it.value }
            .take(topK)

        return ranked.mapNotNull { (docId, score) ->
            val doc = documents[docId] ?: return@mapNotNull null
            val terms = matchedTerms[docId] ?: return@mapNotNull null
            RetrievedPassage(
                documentId = docId,
                documentName = doc.name,
                score = score,
                snippet = buildSnippet(doc.text, terms)
            )
        }
    }

    /**
     * Extracts a readable excerpt from [text] around the first matched term.
     */
    private fun buildSnippet(text: String, terms: Set<String>): String {
        val lower = text.lowercase()
        val term = terms.firstOrNull { lower.contains(it) } ?: return text.take(400)
        val idx = lower.indexOf(term)
        if (idx == -1) return text.take(400)

        val window = 400
        val start = (idx - window / 3).coerceAtLeast(0)
        val end = (start + window).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + text.substring(start, end).replace('\n', ' ').trim() + suffix
    }

    /**
     * Builds a markdown "document context" block for prompt injection.
     */
    fun buildContextBlock(query: String, topK: Int = 4): String {
        val passages = search(query, topK)
        if (passages.isEmpty()) return ""

        return buildString {
            append("### Retrieved from your local documents\n\n")
            passages.forEachIndexed { index, passage ->
                append("**[$index] From \"${passage.documentName}\":**\n")
                append("> ${passage.snippet}\n\n")
            }
        }
    }
}