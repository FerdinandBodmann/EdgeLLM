package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo
import com.example.models.ModelSource
import com.example.rag.RagIndex
import com.example.rag.RetrievedPassage

/**
 * Local retrieval-augmented generation tool.
 *
 * When the user refers to their imported documents/notes/memory, this tool
 * searches the fully-local [RagIndex] and returns the best-matching passages
 * so the engine can ground its answer in the user's own content.
 */
object RagTool : ChatTool {

    override val name: String = "Notes & Documents (Local RAG)"

    override val description: String =
        "Searches the user's locally imported documents (text, markdown, PDF) for relevant passages."

    private val triggers = listOf(
        "my documents", "my notes", "my pdf", "search my", "in my notes",
        "from my documents", "look in my", "in my documents", "according to the document",
        "according to my", "rag", "local knowledge", "my uploaded", "my files",
        "was ist in meinen notizen", "meine notizen", "meine dokumente"
    )

    override fun canHandle(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return triggers.any { lower.contains(it) }
    }

    override suspend fun execute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        if (!RagIndex.hasDocuments()) {
            return ToolResult(
                toolName = name,
                content = "You don't have any imported documents yet.\n\n" +
                    "• Open the chat menu → **Documents (RAG)** and import a text, Markdown or PDF file.\n" +
                    "• Once indexed, ask e.g. *\"according to my notes, what is the main topic?\"*\n" +
                    "• Everything stays on-device: your files are never uploaded."
            )
        }

        val query = prompt
            .replace(Regex("(?i)search my|look in my|find in my|in my notes|in my documents|according to the document|according to my"), "")
            .trim()
            .ifEmpty { prompt }

        val passages: List<RetrievedPassage> = RagIndex.search(query, topK = 5)
        if (passages.isEmpty()) {
            return ToolResult(
                toolName = name,
                content = "I searched your ${RagIndex.documentCount()} local document(s) but found nothing matching " +
                    "\"" + prompt.take(80) + "\".\n\n" +
                    "• Try rephrasing with different keywords.\n" +
                    "• Ask for a general summary with *\"summarize my documents\"*."
            )
        }

        val matchLines = passages.take(3).joinToString("\n") { p ->
            "- **\"${p.documentName}\"** (relevance ${"%.2f".format(p.score)}):\n  ${p.snippet.take(240)}"
        }

        // Ensure docs are "from" a local file so the model info path stays honest.
        val engineNote = if (model.source == ModelSource.CURATED || model.isEmbedded) {
            "*(Note: this query was answered from your local documents via on-device retrieval — the current " +
                "${model.name} profile synthesizes the answer locally.)*"
        } else {
            "*(Retrieved from your local documents on-device.)*"
        }

        return ToolResult(
            toolName = name,
            content = "### Matches from your local documents\n\n" +
                matchLines + "\n\n" + engineNote
        )
    }
}