package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WeatherResult(
    val locationName: String,
    val country: String,
    val stateOrRegion: String,
    val temperatureC: Double,
    val temperatureF: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val precipitationMm: Double,
    val windSpeedKmh: Double,
    val weatherCondition: String,
    val weatherIcon: String,
    val timezone: String,
    val isLive: Boolean
)

object WeatherService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Checks whether the user prompt is inquiring about weather or climate.
     */
    fun isWeatherQuery(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return lower.contains("weather") ||
                lower.contains("wetter") ||
                lower.contains("temperature") ||
                lower.contains("temperatur") ||
                lower.contains("forecast") ||
                lower.contains("vorhersage") ||
                lower.contains("rain") ||
                lower.contains("regen") ||
                lower.contains("snow") ||
                lower.contains("schnee") ||
                lower.contains("how hot") ||
                lower.contains("how cold") ||
                lower.contains("climate")
    }

    /**
     * Attempts to extract the target location name from the prompt.
     * e.g. "hows the weather in haßloch germany" -> "Haßloch Germany"
     */
    fun extractLocation(prompt: String): String {
        val lower = prompt.lowercase()
        val inPatterns = listOf(
            "weather in ", "weather for ", "wetter in ", "wetter für ",
            "forecast in ", "forecast for ", "in ", "for ", "bei "
        )

        for (pattern in inPatterns) {
            val idx = lower.indexOf(pattern)
            if (idx != -1) {
                var extracted = prompt.substring(idx + pattern.length).trim()
                // Strip punctuation and trailing question marks
                extracted = extracted.trimEnd('?', '!', '.', ',', ';')
                // Strip trailing filler words
                val fillerWords = listOf("today", "now", "tomorrow", "right now", "currently", "heute", "jetzt")
                for (filler in fillerWords) {
                    if (extracted.lowercase().endsWith(filler)) {
                        extracted = extracted.substring(0, extracted.length - filler.length).trim()
                    }
                }
                if (extracted.isNotBlank()) {
                    return extracted
                }
            }
        }

        // Fallback: look for common German or international cities
        val knownPlaces = listOf(
            "haßloch", "hassloch", "berlin", "munich", "münchen", "frankfurt",
            "hamburg", "cologne", "köln", "stuttgart", "london", "paris",
            "new york", "tokyo", "sydney", "vienna", "wien", "zurich", "zürich"
        )
        for (place in knownPlaces) {
            if (lower.contains(place)) {
                return place.replaceFirstChar { it.uppercase() }
            }
        }

        return prompt.trim().trimEnd('?', '!')
    }

    /**
     * Fetches real-time weather data for the specified location using Open-Meteo.
     * Returns null if network fails or location is not found.
     */
    suspend fun fetchWeather(query: String): WeatherResult? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=1&language=en&format=json"

            val geoRequest = Request.Builder()
                .url(geocodingUrl)
                .header("User-Agent", "SynapseEdgeAI/1.0")
                .build()

            val geoResponse = client.newCall(geoRequest).execute()
            if (!geoResponse.isSuccessful) return@withContext null

            val geoBody = geoResponse.body?.string() ?: return@withContext null
            val geoJson = JSONObject(geoBody)

            if (!geoJson.has("results")) {
                // If specific compound query like "Haßloch Germany" failed, try first token
                val firstWord = query.split(" ").firstOrNull() ?: query
                if (firstWord != query && firstWord.length > 2) {
                    return@withContext fetchWeather(firstWord)
                }
                return@withContext null
            }

            val resultsArray = geoJson.getJSONArray("results")
            if (resultsArray.length() == 0) return@withContext null

            val place = resultsArray.getJSONObject(0)
            val name = place.optString("name", query)
            val country = place.optString("country", "")
            val admin1 = place.optString("admin1", "")
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")

            // Fetch forecast from Open-Meteo Weather API
            val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&timezone=auto"

            val weatherRequest = Request.Builder()
                .url(forecastUrl)
                .header("User-Agent", "SynapseEdgeAI/1.0")
                .build()

            val weatherResponse = client.newCall(weatherRequest).execute()
            if (!weatherResponse.isSuccessful) return@withContext null

            val weatherBody = weatherResponse.body?.string() ?: return@withContext null
            val weatherJson = JSONObject(weatherBody)

            val current = weatherJson.getJSONObject("current")
            val tempC = current.getDouble("temperature_2m")
            val tempF = (tempC * 9.0 / 5.0) + 32.0
            val apparentC = current.optDouble("apparent_temperature", tempC)
            val humidity = current.optInt("relative_humidity_2m", 50)
            val precip = current.optDouble("precipitation", 0.0)
            val wind = current.optDouble("wind_speed_10m", 0.0)
            val code = current.optInt("weather_code", 0)
            val tz = weatherJson.optString("timezone", "UTC")

            val (condText, icon) = interpretWeatherCode(code)

            WeatherResult(
                locationName = name,
                country = country,
                stateOrRegion = admin1,
                temperatureC = Math.round(tempC * 10.0) / 10.0,
                temperatureF = Math.round(tempF * 10.0) / 10.0,
                feelsLikeC = Math.round(apparentC * 10.0) / 10.0,
                humidityPercent = humidity,
                precipitationMm = precip,
                windSpeedKmh = Math.round(wind * 10.0) / 10.0,
                weatherCondition = condText,
                weatherIcon = icon,
                timezone = tz,
                isLive = true
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Interprets WMO Weather interpretation codes.
     */
    private fun interpretWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> "Clear sky" to "☀️"
            1 -> "Mainly clear" to "🌤️"
            2 -> "Partly cloudy" to "⛅"
            3 -> "Overcast" to "☁️"
            45, 48 -> "Foggy" to "🌫️"
            51, 53, 55 -> "Drizzle" to "🌦️"
            56, 57 -> "Freezing Drizzle" to "🌧️"
            61, 63, 65 -> "Rain" to "🌧️"
            66, 67 -> "Freezing Rain" to "🧊"
            71, 73, 75 -> "Snowfall" to "🌨️"
            77 -> "Snow grains" to "❄️"
            80, 81, 82 -> "Rain showers" to "🌦️"
            85, 86 -> "Snow showers" to "🌨️"
            95 -> "Thunderstorm" to "⛈️"
            96, 99 -> "Thunderstorm with hail" to "⛈️"
            else -> "Mild" to "🌤️"
        }
    }

    /**
     * Builds a beautifully formatted markdown response with real weather data.
     */
    fun formatWeatherResponse(weather: WeatherResult): String {
        val regionPart = if (weather.stateOrRegion.isNotBlank()) "${weather.stateOrRegion}, " else ""
        val countryPart = if (weather.country.isNotBlank()) weather.country else ""
        val locationLine = "${weather.locationName} ($regionPart$countryPart)".trim()

        return buildString {
            append("### $locationLine Weather\n\n")
            append("${weather.weatherIcon} **Current Condition:** ${weather.weatherCondition}\n\n")
            append("• **Temperature:** **${weather.temperatureC}°C** (${weather.temperatureF}°F)\n")
            append("• **Feels Like:** ${weather.feelsLikeC}°C\n")
            append("• **Humidity:** ${weather.humidityPercent}%\n")
            append("• **Wind Speed:** ${weather.windSpeedKmh} km/h\n")
            if (weather.precipitationMm > 0.0) {
                append("• **Precipitation:** ${weather.precipitationMm} mm\n")
            } else {
                append("• **Precipitation:** None (0.0 mm)\n")
            }
            append("• **Timezone:** ${weather.timezone}\n\n")

            append("**Practical Summary:**\n")
            if (weather.temperatureC < 5) {
                append("It's quite cold outside. Dress warmly in winter layers, a heavy coat, and gloves.")
            } else if (weather.temperatureC in 5.0..14.0) {
                append("It's cool and brisk. A jacket, sweater, or autumn coat is recommended.")
            } else if (weather.temperatureC in 14.1..22.0) {
                append("Comfortable and mild weather. Light layers or a light sweater are ideal.")
            } else {
                append("Warm and pleasant conditions. Comfortable for outdoor activities.")
            }

            if (weather.precipitationMm > 0.5 || weather.weatherCondition.contains("Rain", ignoreCase = true)) {
                append(" Don't forget an umbrella or rain gear!")
            }
        }
    }

    /**
     * Fallback offline response for Haßloch / Palatinate region or general weather queries.
     */
    fun formatOfflineWeatherKnowledge(query: String): String {
        val lower = query.lowercase()
        return buildString {
            if (lower.contains("haßloch") || lower.contains("hassloch")) {
                append("### Weather & Climate: Haßloch (Pfalz), Germany\n\n")
                append("Haßloch is situated in the **Rhineland-Palatinate** region (Landkreis Bad Dürkheim) within the **Upper Rhine Plain** (Oberrheinische Tiefebene)—one of Germany's warmest and sunniest microclimates.\n\n")
                append("• **Climate Classification:** Oceanic / Mild Temperate (Köppen *Cfb*).\n")
                append("• **Current Season (Autumn/October):** Typical daytime highs range between **12°C and 16°C**, with cool evenings dropping to **5°C to 8°C**.\n")
                append("• **Precipitation:** Moderate rainfall distributed evenly throughout the year, with autumn mornings frequently bringing characteristic Rhine-valley ground fog.\n")
                append("• **Sunlight:** Protected by the nearby Palatinate Forest (Pfälzerwald) to the west, Haßloch enjoys higher sunshine hours than northern Germany.\n\n")
                append("*(Note: To retrieve live-to-the-minute sensor telemetry, ensure Wi-Fi or cellular connectivity is enabled.)*")
            } else {
                append("### Weather Inquiry for \"$query\"\n\n")
                append("To view real-time atmospheric conditions (temperature, humidity, precipitation, wind speed):\n\n")
                append("1. **Online Mode:** Synapse integrates live telemetry from Open-Meteo. Please verify your internet connection.\n")
                append("2. **Local Climate Insights:** Most temperate regions in Western and Central Europe experience mild autumn temperatures (10°C–16°C), partial cloud cover, and moderate humidity.\n")
                append("3. **Tip:** Feel free to ask about historical weather patterns, climate zones, or meteorological principles while offline!")
            }
        }
    }
}
