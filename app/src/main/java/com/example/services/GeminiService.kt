package com.example.services

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object GeminiService {

    private const val DEFAULT_MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a valid-looking Gemini API key is configured.
     */
    fun hasApiKey(customKey: String? = null): Boolean {
        val key = getApiKey(customKey)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("dummy")
    }

    fun getApiKey(customKey: String? = null): String {
        return if (!customKey.isNullOrBlank()) {
            customKey.trim()
        } else {
            BuildConfig.GEMINI_API_KEY.trim()
        }
    }

    /**
     * Streams generation chunks directly from Gemini via Server-Sent Events (SSE).
     */
    fun streamGenerateContent(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = "You are Synapse, a helpful, thoughtful, intelligent assistant.",
        customApiKey: String? = null,
        modelName: String = DEFAULT_MODEL
    ): Flow<String> = flow {
        val apiKey = getApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API_KEY_MISSING")
        }

        val requestJson = JSONObject().apply {
            // System instructions
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            // Conversation history & user prompt
            val contentsArray = JSONArray()
            for ((role, content) in history) {
                if (content.isNotBlank()) {
                    val geminiRole = if (role == "assistant" || role == "model") "model" else "user"
                    contentsArray.put(JSONObject().apply {
                        put("role", geminiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", content) })
                        })
                    })
                }
            }

            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })
            put("contents", contentsArray)

            // Generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            })
        }

        val url = "$BASE_URL$modelName:streamGenerateContent?alt=sse&key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            val errMsg = try {
                val json = JSONObject(errBody)
                json.getJSONObject("error").getString("message")
            } catch (e: Exception) {
                "HTTP ${response.code}: ${response.message}"
            }
            throw RuntimeException("Gemini API Error: $errMsg")
        }

        val inputStream = response.body?.byteStream() ?: throw RuntimeException("Empty response stream")
        val reader = BufferedReader(InputStreamReader(inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line?.trim() ?: continue
            if (!currentLine.startsWith("data:")) continue

            val jsonPayload = currentLine.removePrefix("data:").trim()
            if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

            try {
                val json = JSONObject(jsonPayload)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val text = part.optString("text", "")
                            if (text.isNotEmpty()) {
                                emit(text)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors on individual SSE chunks
            }
        }
    }.flowOn(Dispatchers.IO)
}
