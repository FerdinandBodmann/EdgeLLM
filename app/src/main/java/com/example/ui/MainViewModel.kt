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
        "General Assistant" to "You are EdgeLLM, an intelligent on-device AI running natively and privately on this Android device without internet connectivity.",
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

    // Storage Statistics
    private val _storageStats = MutableStateFlow(modelManager.getStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

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
                // Stream response token-by-token from On-Device Inference Engine
                inferenceEngine.generateStream(
                    prompt = text,
                    conversationHistory = history,
                    config = _inferenceConfig.value
                ).collect { chunk ->
                    _currentTelemetry.value = chunk.telemetry

                    // Update assistant message in DB
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
            } catch (e: Exception) {
                // In case of error/interruption, update with current text
                val current = _messages.value.find { it.id == assistantMsgId }?.content ?: ""
                chatDao.updateMessage(
                    MessageEntity(
                        id = assistantMsgId,
                        chatId = chatId,
                        role = "assistant",
                        content = if (current.isEmpty()) "Inference paused or cancelled." else current,
                        modelName = activeModel.name
                    )
                )
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

    private fun refreshStorage() {
        _storageStats.value = modelManager.getStorageStats()
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
