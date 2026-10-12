package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * On-device clock & calendar tool (no network required).
 */
object TimeDateTool : ChatTool {

    override val name: String = "Clock & Calendar"

    override val description: String = "Current device time, date and weekday, fully offline."

    private val triggers = listOf(
        "what time", "current time", "what's the time", "whats the time", "wie spät",
        "what day", "what's today", "whats today", "today's date", "todays date",
        "the date", "current date", "what is the date", "datum", "welcher tag",
        "wochentag", "what date is it"
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
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val isWeekend = dayOfWeek.equals("Saturday", true) || dayOfWeek.equals("Sunday", true)

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val period = when {
            hour < 5 -> "night"
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }
        val greeting = when {
            isWeekend -> "Enjoy your weekend!"
            hour >= 22 || hour < 5 -> "It's late — consider resting soon."
            else -> "Have a productive day!"
        }

        val content = buildString {
            append("### Local Time & Date\n\n")
            append("• **Time:** ${timeFormat.format(now)}\n")
            append("• **Date:** ${dateFormat.format(now)}\n")
            append("• **Day:** $dayOfWeek ($period)\n\n")
            append(greeting)
        }
        return ToolResult(toolName = name, content = content)
    }
}