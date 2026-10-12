package com.example.util

import com.example.data.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Serializes a conversation to shareable Markdown.
 */
object ChatExporter {

    fun toMarkdown(title: String, messages: List<MessageEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            append("# $title\n\n")
            append("_Exported from Synapse (EdgeLLM) on ${dateFormat.format(Date())}_\n\n")
            append("---\n\n")

            messages.forEach { message ->
                val role = when (message.role) {
                    "user" -> "🧑 You"
                    "assistant" -> "✨ ${message.modelName.ifBlank { "Assistant" }}"
                    else -> "⚙️ System"
                }
                append("### $role\n\n")
                if (message.content.isBlank()) {
                    append("_(empty message)_\n\n")
                } else {
                    append(message.content.trim())
                    append("\n\n")
                }
                if (message.role == "assistant" && message.tokensPerSec > 0f) {
                    append("> ${message.tokensCount} tokens • ${message.tokensPerSec} tok/s • " +
                        "${message.timeToFirstTokenMs} ms TTFT\n\n")
                }
            }
        }
    }
}