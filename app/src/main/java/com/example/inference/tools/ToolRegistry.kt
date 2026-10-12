package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo

/**
 * Ordered registry of local [ChatTool] implementations.
 *
 * Order matters: tools are consulted in registration order, so specific,
 * unambiguous detectors (weather, math) run before broader heuristic tools.
 */
object ToolRegistry {

    private val tools: List<ChatTool> = listOf(
        WeatherTool,
        MathTool,
        TimeDateTool,
        UnitConverterTool,
        DeviceInfoTool,
        RagTool
    )

    /** All registered tools, in execution order. */
    val registeredTools: List<ChatTool> get() = tools

    /**
     * Finds and runs the first applicable tool for [prompt].
     *
     * @return the produced [ToolResult], or null when no tool matched.
     */
    suspend fun tryExecute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return null

        for (tool in tools) {
            val result = try {
                if (tool.canHandle(trimmed)) {
                    tool.execute(trimmed, model, context)
                } else {
                    null
                }
            } catch (t: Throwable) {
                // A failing tool must never block the other tools or the engine.
                null
            }
            if (result != null) return result
        }
        return null
    }
}