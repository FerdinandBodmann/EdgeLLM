package com.example.services

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

data class OllamaPullProgress(
    val status: String,
    val completedBytes: Long = 0,
    val totalBytes: Long = 0,
    val progress: Float = 0f
)

object OllamaService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_ENDPOINT = "http://10.0.2.2:11434" // Default Android emulator host loopback or LAN IP

    /**
     * Checks if the Ollama server is reachable and returns the list of available model tags.
     */
    suspend fun fetchAvailableModels(endpoint: String = DEFAULT_ENDPOINT): Result<List<String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val cleanUrl = endpoint.trimEnd('/') + "/api/tags"
            val request = Request.Builder().url(cleanUrl).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: JSONArray()
            val modelNames = mutableListOf<String>()
            for (i in 0 until modelsArray.length()) {
                val item = modelsArray.getJSONObject(i)
                val name = item.optString("name")
                if (name.isNotBlank()) modelNames.add(name)
            }
            Result.success(modelNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams real-time tokens from a running Ollama server instance via /api/chat.
     */
    fun streamChat(
        endpoint: String,
        modelTag: String,
        messages: List<Pair<String, String>>,
        systemPrompt: String = ""
    ): Flow<String> = flow {
        val cleanUrl = endpoint.trimEnd('/') + "/api/chat"

        val jsonBody = JSONObject().apply {
            put("model", modelTag)
            put("stream", true)

            val messagesArray = JSONArray()
            if (systemPrompt.isNotBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            for ((role, content) in messages) {
                messagesArray.put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            put("messages", messagesArray)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(cleanUrl)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Ollama server error ${response.code}: ${response.message}")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                try {
                    val chunkJson = JSONObject(trimmed)
                    val messageObj = chunkJson.optJSONObject("message")
                    val content = messageObj?.optString("content").orEmpty()
                    if (content.isNotEmpty()) {
                        emit(content)
                    }
                    if (chunkJson.optBoolean("done", false)) {
                        break
                    }
                } catch (_: Exception) {}
            }
            line = reader.readLine()
        }
        reader.close()
    }.flowOn(Dispatchers.IO)

    /**
     * Pulls a model on the Ollama host with real streaming progress.
     */
    fun pullModel(
        endpoint: String,
        modelTag: String
    ): Flow<OllamaPullProgress> = flow {
        val cleanUrl = endpoint.trimEnd('/') + "/api/pull"
        val jsonBody = JSONObject().apply {
            put("name", modelTag)
            put("stream", true)
        }
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(cleanUrl)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to pull from Ollama: HTTP ${response.code}")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                try {
                    val obj = JSONObject(trimmed)
                    val status = obj.optString("status", "Pulling...")
                    val total = obj.optLong("total", 0L)
                    val completed = obj.optLong("completed", 0L)
                    val progress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    emit(OllamaPullProgress(status, completed, total, progress))
                } catch (_: Exception) {}
            }
            line = reader.readLine()
        }
        reader.close()
    }.flowOn(Dispatchers.IO)
}
