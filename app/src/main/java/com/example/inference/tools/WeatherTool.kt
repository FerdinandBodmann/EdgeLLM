package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo
import com.example.services.WeatherService

/**
 * Live & offline weather tool backed by [WeatherService] (Open-Meteo).
 */
object WeatherTool : ChatTool {

    override val name: String = "Weather"

    override val description: String =
        "Live weather lookup for any city/location via Open-Meteo, with offline regional climate fallback."

    override fun canHandle(prompt: String): Boolean = WeatherService.isWeatherQuery(prompt)

    override suspend fun execute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        val location = WeatherService.extractLocation(prompt)
        val liveWeather = WeatherService.fetchWeather(location)
        val content = if (liveWeather != null) {
            WeatherService.formatWeatherResponse(liveWeather)
        } else {
            WeatherService.formatOfflineWeatherKnowledge(location)
        }
        return ToolResult(toolName = name, content = content)
    }
}