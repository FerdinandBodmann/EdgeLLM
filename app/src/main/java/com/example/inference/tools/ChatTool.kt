package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo

/**
 * Result produced by a local [ChatTool] invocation.
 *
 * @property toolName display name of the tool that produced the content.
 * @property content markdown-ready answer text that the engine streams back.
 */
data class ToolResult(
    val toolName: String,
    val content: String
)

/**
 * A lightweight, deterministic "tool" that the on-device inference engine can
 * invoke before falling back to the response synthesizer.
 *
 * This mirrors the function-calling contract of cloud providers (Gemini tools,
 * Ollama tools) but keeps every capability local and dependency-free, so the
 * app retains its privacy-first/offline identity.
 */
interface ChatTool {

    /** Stable identifier, e.g. "Weather", "Math", "Clock". */
    val name: String

    /** Short human-readable description used in UI and diagnostics. */
    val description: String

    /**
     * Cheap, synchronous gate check. [execute] is only called when this
     * returns true, so implementations should keep this non-blocking.
     */
    fun canHandle(prompt: String): Boolean

    /**
     * Executes the tool for [prompt]. Return null when the prompt turns out
     * not to match after all (the registry moves on to the next tool).
     */
    suspend fun execute(prompt: String, model: ModelInfo, context: Context?): ToolResult?
}