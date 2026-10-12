package com.example

import com.example.services.WeatherService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testWeatherQueryDetection() {
        assertTrue(WeatherService.isWeatherQuery("hows the weather in haßloch germany"))
        assertTrue(WeatherService.isWeatherQuery("what's the temperature in Berlin?"))
        assertTrue(WeatherService.isWeatherQuery("will it rain tomorrow in Paris"))
    }

    @Test
    fun testWeatherLocationExtraction() {
        val extracted = WeatherService.extractLocation("hows the weather in haßloch germany")
        assertTrue(extracted.contains("haßloch", ignoreCase = true) || extracted.contains("hassloch", ignoreCase = true))
    }

    @Test
    fun testOfflineWeatherKnowledgeFormatting() {
        val response = WeatherService.formatOfflineWeatherKnowledge("haßloch germany")
        assertTrue(response.contains("Haßloch"))
        assertTrue(response.contains("Rhineland-Palatinate") || response.contains("Pfalz"))
    }
}
