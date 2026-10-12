package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import com.example.inference.BenchmarkProgress
import com.example.inference.ComputeBackend
import com.example.inference.EngineBenchmark
import com.example.inference.EngineState
import com.example.inference.InferenceConfig
import com.example.inference.InferenceTelemetry
import com.example.inference.OnDeviceInferenceEngine
import com.example.models.ModelItem
import com.example.models.ModelManager
import com.example.models.ModelStatus
import com.example.models.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()

    private val prefs = application.getSharedPreferences("synapse_prefs", android.content.Context.MODE_PRIVATE)
    val customGeminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")

    fun updateGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        customGeminiApiKey.value = trimmed
    }

    val modelManager = ModelManager(application)
    val inferenceEngine = OnDeviceInferenceEngine(application)
    private val benchmark = EngineBenchmark(inferenceEngine)

    // Active Navigation Tab
    val selectedTab = MutableStateFlow(0) // 0: Chat, 1: Models, 2: Benchmark & Settings

    // Chat Session & Messages
    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentTelemetry = MutableStateFlow<InferenceTelemetry?>(null)
    val currentTelemetry: StateFlow<InferenceTelemetry?> = _currentTelemetry.asStateFlow()

    val promptInput = MutableStateFlow("")

    val systemPersonas = listOf(
        "General Assistant" to "You are Synapse, a thoughtful on-device AI running natively and privately on this Android device without internet connectivity.",
        "Code Copilot" to "You are an expert offline code assistant. Provide clean, secure, idiomatic code examples with concise technical explanations.",
        "Concise Analyzer" to "You are a concise, direct analytical AI. Provide structured bullet points, clear facts, and zero conversational fluff.",
        "Creative Writer" to "You are an imaginative creative writer and brainstorming partner running locally on device.",
        "Offline Tutor" to "You are a patient, encouraging tutor. Explain complex concepts in simple, accessible steps without assuming online access."
    )

    private val _selectedPersonaIndex = MutableStateFlow(0)
    val selectedPersonaIndex: StateFlow<Int> = _selectedPersonaIndex.asStateFlow()

    // Inference Settings
    private val _inferenceConfig = MutableStateFlow(
        InferenceConfig(
            systemPrompt = systemPersonas[0].second
        )
    )
    val inferenceConfig: StateFlow<InferenceConfig> = _inferenceConfig.asStateFlow()

    // Benchmark State
    private val _benchmarkProgress = MutableStateFlow<BenchmarkProgress>(BenchmarkProgress.Idle)
    val benchmarkProgress: StateFlow<BenchmarkProgress> = _benchmarkProgress.asStateFlow()

    // Storage Statistics & Downloaded Inventory
    private val _storageStats = MutableStateFlow(modelManager.getStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _downloadedFiles = MutableStateFlow(modelManager.getDownloadedModelFiles())
    val downloadedFiles: StateFlow<List<com.example.models.DownloadedModelFile>> = _downloadedFiles.asStateFlow()

    private var streamingJob: Job? = null
    private var benchmarkJob: Job? = null
    private var messageCollectionJob: Job? = null

    init {
        // Initialize default model
        viewModelScope.launch {
            val activeModel = modelManager.getActiveModelInfo()
            inferenceEngine.loadModel(activeModel)
            initializeChatSession()
        }
    }

    private suspend fun initializeChatSession() {
        chatDao.getAllChats().collectLatest { chats ->
            if (chats.isNotEmpty()) {
                if (_currentChatId.value == null) {
                    selectChat(chats.first().id)
                }
            } else {
                createNewChatInternal("New Chat")
            }
        }
    }

    fun selectChat(chatId: Long) {
        _currentChatId.value = chatId
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatDao.getMessagesForChat(chatId).collectLatest { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            createNewChatInternal("New Chat")
        }
    }

    private suspend fun createNewChatInternal(title: String) {
        val newChatId = chatDao.insertChat(
            ChatEntity(
                title = title,
                activeModelId = modelManager.activeModelId.value
            )
        )
        selectChat(newChatId)
    }

    fun selectPersona(index: Int) {
        if (index in systemPersonas.indices) {
            _selectedPersonaIndex.value = index
            _inferenceConfig.value = _inferenceConfig.value.copy(
                systemPrompt = systemPersonas[index].second
            )
        }
    }

    fun sendMessage(userText: String) {
        val text = userText.trim()
        if (text.isEmpty() || _isStreaming.value) return

        val chatId = _currentChatId.value ?: return
        val activeModel = modelManager.getActiveModelInfo()

        promptInput.value = ""

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            _isStreaming.value = true

            // If first message in chat, update chat title
            if (_messages.value.isEmpty()) {
                val titleSnippet = if (text.length > 28) text.take(28) + "..." else text
                chatDao.updateChatTitle(chatId, titleSnippet)
            }

            // Insert User Message into Database
            chatDao.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    role = "user",
                    content = text,
                    modelName = activeModel.name
                )
            )

            // Insert Assistant Message Placeholder
            val assistantMsgId = chatDao.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    role = "assistant",
                    content = "",
                    modelName = activeModel.name
                )
            )

            // Build historical context pairs
            val history = _messages.value.map { it.role to it.content }

            try {
                if (activeModel.family == "Gemini" && com.example.services.GeminiService.hasApiKey(customGeminiApiKey.value)) {
                    val startTime = android.os.SystemClock.elapsedRealtime()
                    var firstTokenTime = 0L
                    var tokenCount = 0
                    val accumulated = StringBuilder()

                    com.example.services.GeminiService.streamGenerateContent(
                        prompt = text,
                        history = history,
                        systemPrompt = _inferenceConfig.value.systemPrompt,
                        customApiKey = customGeminiApiKey.value,
                        modelName = "gemini-2.5-flash"
                    ).collect { chunk ->
                        if (firstTokenTime == 0L) {
                            firstTokenTime = android.os.SystemClock.elapsedRealtime()
                        }
                        accumulated.append(chunk)
                        tokenCount++
                        val now = android.os.SystemClock.elapsedRealtime()
                        val elapsed = (now - firstTokenTime).coerceAtLeast(1L)
                        val tps = (tokenCount.toFloat() / (elapsed / 1000f)).coerceAtLeast(0.1f)
                        val telemetry = InferenceTelemetry(
                            tokensGenerated = tokenCount,
                            tokensPerSecond = Math.round(tps * 10f) / 10f,
                            timeToFirstTokenMs = (firstTokenTime - startTime).coerceAtLeast(1L),
                            totalDurationMs = now - startTime,
                            ramAllocatedMb = 35,
                            activeBackend = "Google Gemini 2.5 Flash Cloud",
                            modelName = "Gemini 2.5 Flash"
                        )
                        _currentTelemetry.value = telemetry
                        chatDao.updateMessage(
                            MessageEntity(
                                id = assistantMsgId,
                                chatId = chatId,
                                role = "assistant",
                                content = accumulated.toString(),
                                tokensCount = tokenCount,
                                tokensPerSec = telemetry.tokensPerSecond,
                                timeToFirstTokenMs = telemetry.timeToFirstTokenMs,
                                modelName = "Gemini 2.5 Flash"
                            )
                        )
                    }
                } else if (activeModel.source == com.example.models.ModelSource.OLLAMA || activeModel.family == "Ollama") {
                    val tag = if (activeModel.repoOrTag.isNotBlank()) {
                        activeModel.repoOrTag.removePrefix("ollama:")
                    } else {
                        activeModel.id.removePrefix("ollama_")
                    }
                    val startTime = android.os.SystemClock.elapsedRealtime()
                    var firstTokenTime = 0L
                    var tokenCount = 0
                    val accumulated = StringBuilder()

                    try {
                        com.example.services.OllamaService.streamChat(
                            endpoint = modelManager.ollamaHostUrl.value,
                            modelTag = tag,
                            messages = history + ("user" to text),
                            systemPrompt = _inferenceConfig.value.systemPrompt
                        ).collect { chunk ->
                            if (firstTokenTime == 0L) {
                                firstTokenTime = android.os.SystemClock.elapsedRealtime()
                            }
                            accumulated.append(chunk)
                            tokenCount++
                            val now = android.os.SystemClock.elapsedRealtime()
                            val elapsed = (now - firstTokenTime).coerceAtLeast(1L)
                            val tps = (tokenCount.toFloat() / (elapsed / 1000f)).coerceAtLeast(0.1f)
                            val telemetry = InferenceTelemetry(
                                tokensGenerated = tokenCount,
                                tokensPerSecond = Math.round(tps * 10f) / 10f,
                                timeToFirstTokenMs = (firstTokenTime - startTime).coerceAtLeast(1L),
                                totalDurationMs = now - startTime,
                                ramAllocatedMb = 250,
                                activeBackend = "Ollama Host (${modelManager.ollamaHostUrl.value})",
                                modelName = activeModel.name
                            )
                            _currentTelemetry.value = telemetry
                            chatDao.updateMessage(
                                MessageEntity(
                                    id = assistantMsgId,
                                    chatId = chatId,
                                    role = "assistant",
                                    content = accumulated.toString(),
                                    tokensCount = tokenCount,
                                    tokensPerSec = telemetry.tokensPerSecond,
                                    timeToFirstTokenMs = telemetry.timeToFirstTokenMs,
                                    modelName = activeModel.name
                                )
                            )
                        }
                    } catch (ollamaErr: Exception) {
                        val notice = "⚠️ **Could not connect to Ollama server at `${modelManager.ollamaHostUrl.value}`**\n\n" +
                                "To run **${activeModel.name}** with real neural weights, ensure Ollama is running on your machine or local network (`ollama run $tag`).\n\n" +
                                "• You can update the Ollama server IP in the Models tab.\n" +
                                "• Or switch to **NanoLLM Edge Core** for native offline intelligence, or **Gemini 2.5 Flash** for cloud reasoning.\n\n" +
                                "*Error details: ${ollamaErr.localizedMessage}*"
                        chatDao.updateMessage(
                            MessageEntity(
                                id = assistantMsgId,
                                chatId = chatId,
                                role = "assistant",
                                content = notice,
                                tokensCount = 0,
                                tokensPerSec = 0f,
                                timeToFirstTokenMs = 0L,
                                modelName = activeModel.name
                            )
                        )
                    }
                } else {
                    // Stream response token-by-token from On-Device Inference Engine (with live WeatherService & smart answering)
                    inferenceEngine.generateStream(
                        prompt = text,
                        conversationHistory = history,
                        config = _inferenceConfig.value
                    ).collect { chunk ->
                        _currentTelemetry.value = chunk.telemetry
                        chatDao.updateMessage(
                            MessageEntity(
                                id = assistantMsgId,
                                chatId = chatId,
                                role = "assistant",
                                content = chunk.accumulatedText,
                                tokensCount = chunk.telemetry.tokensGenerated,
                                tokensPerSec = chunk.telemetry.tokensPerSecond,
                                timeToFirstTokenMs = chunk.telemetry.timeToFirstTokenMs,
                                modelName = activeModel.name
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // If cloud/API stream encounters an issue, fallback seamlessly to on-device engine
                val current = _messages.value.find { it.id == assistantMsgId }?.content ?: ""
                if (current.isEmpty()) {
                    inferenceEngine.generateStream(
                        prompt = text,
                        conversationHistory = history,
                        config = _inferenceConfig.value
                    ).collect { chunk ->
                        _currentTelemetry.value = chunk.telemetry
                        chatDao.updateMessage(
                            MessageEntity(
                                id = assistantMsgId,
                                chatId = chatId,
                                role = "assistant",
                                content = chunk.accumulatedText,
                                tokensCount = chunk.telemetry.tokensGenerated,
                                tokensPerSec = chunk.telemetry.tokensPerSecond,
                                timeToFirstTokenMs = chunk.telemetry.timeToFirstTokenMs,
                                modelName = activeModel.name
                            )
                        )
                    }
                } else {
                    chatDao.updateMessage(
                        MessageEntity(
                            id = assistantMsgId,
                            chatId = chatId,
                            role = "assistant",
                            content = current,
                            modelName = activeModel.name
                        )
                    )
                }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }

    fun clearCurrentChat() {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessagesForChat(chatId)
            _currentTelemetry.value = null
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessage(messageId)
        }
    }

    // Model Management Actions
    fun activateModel(modelId: String) {
        viewModelScope.launch {
            modelManager.setActiveModel(modelId)
            val activeModel = modelManager.getActiveModelInfo()
            inferenceEngine.loadModel(activeModel)
            refreshStorage()
        }
    }

    fun downloadModel(modelId: String) {
        modelManager.startDownload(modelId, viewModelScope)
        refreshStorage()
    }

    fun cancelDownload(modelId: String) {
        modelManager.cancelDownload(modelId)
        refreshStorage()
    }

    fun downloadFromDirectUrl(url: String, customName: String? = null, paramSize: String = "Quantized", sizeMb: Int = 800) {
        val model = modelManager.downloadFromDirectUrl(url, customName, paramSize, sizeMb, viewModelScope)
        refreshStorage()
    }

    fun pullOllamaTag(tag: String) {
        val model = modelManager.pullOllamaTag(tag, viewModelScope)
        refreshStorage()
    }

    fun deleteModel(modelId: String) {
        modelManager.deleteModel(modelId)
        refreshStorage()
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            val result = modelManager.importCustomModel(uri)
            if (result.isSuccess) {
                val imported = result.getOrNull()
                if (imported != null) {
                    inferenceEngine.loadModel(imported)
                }
            }
            refreshStorage()
        }
    }

    fun clearAllDownloadedModels() {
        viewModelScope.launch {
            modelManager.clearAllDownloadedModels()
            val activeModel = modelManager.getActiveModelInfo()
            inferenceEngine.loadModel(activeModel)
            refreshStorage()
        }
    }

    fun refreshStorage() {
        _storageStats.value = modelManager.getStorageStats()
        _downloadedFiles.value = modelManager.getDownloadedModelFiles()
    }

    // Tuning & Hyperparameters
    fun updateTemperature(value: Float) {
        _inferenceConfig.value = _inferenceConfig.value.copy(temperature = value)
    }

    fun updateTopP(value: Float) {
        _inferenceConfig.value = _inferenceConfig.value.copy(topP = value)
    }

    fun updateTopK(value: Int) {
        _inferenceConfig.value = _inferenceConfig.value.copy(topK = value)
    }

    fun updateMaxTokens(value: Int) {
        _inferenceConfig.value = _inferenceConfig.value.copy(maxTokens = value)
    }

    fun updateThreadCount(value: Int) {
        _inferenceConfig.value = _inferenceConfig.value.copy(threadCount = value)
    }

    fun updateComputeBackend(backend: ComputeBackend) {
        _inferenceConfig.value = _inferenceConfig.value.copy(computeBackend = backend)
    }

    fun updateSystemPrompt(prompt: String) {
        _inferenceConfig.value = _inferenceConfig.value.copy(systemPrompt = prompt)
    }

    // Benchmark Actions
    fun runBenchmark() {
        val model = modelManager.getActiveModelInfo()
        benchmarkJob?.cancel()
        benchmarkJob = viewModelScope.launch {
            benchmark.runBenchmark(model, _inferenceConfig.value).collect { progress ->
                _benchmarkProgress.value = progress
            }
        }
    }

    fun cancelBenchmark() {
        benchmarkJob?.cancel()
        _benchmarkProgress.value = BenchmarkProgress.Idle
    }
}
