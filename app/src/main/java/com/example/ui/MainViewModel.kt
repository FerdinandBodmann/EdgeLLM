package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BenchmarkRunEntity
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import com.example.inference.BenchmarkProgress
import com.example.inference.ComputeBackend
import com.example.inference.EngineBenchmark
import com.example.inference.EngineState
import com.example.inference.InferenceConfig
import com.example.inference.InferenceTelemetry
import com.example.inference.OnDeviceInferenceEngine
import com.example.models.ModelInfo
import com.example.models.ModelItem
import com.example.models.ModelManager
import com.example.models.ModelStatus
import com.example.models.StorageStats
import com.example.rag.RagIndex
import com.example.util.ChatExporter
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
    private val knowledgeDao = db.knowledgeDao()
    private val benchmarkDao = db.benchmarkDao()

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

    // Overlay screens (rendered above the main content)
    val showChatHistory = MutableStateFlow(false)
    val showDocuments = MutableStateFlow(false)

    // Chat Session & Messages
    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentTelemetry = MutableStateFlow<InferenceTelemetry?>(null)
    val currentTelemetry: StateFlow<InferenceTelemetry?> = _currentTelemetry.asStateFlow()

    // Message currently being edited/resubmitted (null when composing a new turn)
    private val _editingMessageId = MutableStateFlow<Long?>(null)
    val editingMessageId: StateFlow<Long?> = _editingMessageId.asStateFlow()

    // Assistant message currently being read aloud (null when idle)
    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    val promptInput = MutableStateFlow("")

    /** An image staged in the composer, pending send. */
    data class PendingImage(
        val uriString: String,
        val mimeType: String,
        val bytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PendingImage) return false
            return uriString == other.uriString
        }

        override fun hashCode(): Int = uriString.hashCode()
    }

    private val _pendingImage = MutableStateFlow<PendingImage?>(null)
    val pendingImage: StateFlow<PendingImage?> = _pendingImage.asStateFlow()

    /** Reads a picked/captured image into memory so it can be sent as context. */
    fun attachImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val image = runCatching {
                val resolver = getApplication<Application>().contentResolver
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                if (bytes.isEmpty()) null else PendingImage(uri.toString(), mime, bytes)
            }.getOrNull()
            if (image != null) _pendingImage.value = image
        }
    }

    fun clearPendingImage() {
        _pendingImage.value = null
    }

    // Conversation history (pinned first, then most recent activity)
    val allChats: StateFlow<List<ChatEntity>> = chatDao.getAllChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Persisted benchmark history
    val benchmarkHistory: StateFlow<List<BenchmarkRunEntity>> = benchmarkDao.getAllRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Local retrieval-augmented generation toggle
    private val _ragEnabled = MutableStateFlow(prefs.getBoolean("rag_enabled", true))
    val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    fun setRagEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("rag_enabled", enabled).apply()
        _ragEnabled.value = enabled
    }

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
        // Text-to-speech for reading assistant answers aloud.
        com.example.services.SpeechOutput.init(getApplication())
        com.example.services.SpeechOutput.onFinished = { _speakingMessageId.value = null }

        // Initialize default model
        viewModelScope.launch {
            val activeModel = modelManager.getActiveModelInfo()
            inferenceEngine.loadModel(activeModel)
            // Rebuild the local retrieval index before the (long-lived) chat
            // observer starts, otherwise it would never be reached.
            rebuildRagIndex()
            initializeChatSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.example.services.SpeechOutput.shutdown()
        streamingJob?.cancel()
        benchmarkJob?.cancel()
        messageCollectionJob?.cancel()
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
        _editingMessageId.value = null
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

    // --- Conversation management ------------------------------------------------

    fun renameChat(chatId: Long, newTitle: String) {
        val title = newTitle.trim().ifBlank { "Untitled" }
        viewModelScope.launch(Dispatchers.IO) { chatDao.updateChatTitle(chatId, title) }
    }

    fun toggleChatPinned(chatId: Long, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { chatDao.setChatPinned(chatId, pinned) }
    }

    fun deleteChatById(chatId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessagesForChat(chatId)
            chatDao.deleteChat(chatId)
            if (_currentChatId.value == chatId) {
                _currentChatId.value = null
                _messages.value = emptyList()
                _currentTelemetry.value = null
            }
        }
    }

    fun currentChatTitle(): String {
        val id = _currentChatId.value ?: return "Conversation"
        return allChats.value.firstOrNull { it.id == id }?.title ?: "Conversation"
    }

    fun buildCurrentChatMarkdown(): String =
        ChatExporter.toMarkdown(currentChatTitle(), _messages.value)

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

    /** Reads a message aloud, or stops playback if it is already speaking. */
    fun toggleSpeak(message: MessageEntity) {
        if (_speakingMessageId.value == message.id) {
            com.example.services.SpeechOutput.stop()
            _speakingMessageId.value = null
            return
        }
        // Strip fenced code blocks and markdown markers for cleaner narration.
        val spoken = message.content
            .replace(Regex("```[\\s\\S]*?```"), " code block ")
            .replace(Regex("`"), "")
            .replace(Regex("[#*_>|]"), " ")
            .trim()
        if (spoken.isBlank()) return
        _speakingMessageId.value = message.id
        com.example.services.SpeechOutput.speak(spoken)
    }

    fun stopSpeaking() {
        com.example.services.SpeechOutput.stop()
        _speakingMessageId.value = null
    }

    // --- Message actions --------------------------------------------------------

    /**
     * Re-runs generation for an assistant message, replacing it with a fresh
     * response based on the preceding user turn.
     */
    fun regenerateMessage(messageId: Long) {
        if (_isStreaming.value) return
        val chatId = _currentChatId.value ?: return
        val msgs = _messages.value
        val index = msgs.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val target = msgs[index]
        if (target.role != "assistant") return
        val userMessage = msgs.subList(0, index).lastOrNull { it.role == "user" } ?: return
        val history = msgs.subList(0, index).map { it.role to it.content }
        val activeModel = modelManager.getActiveModelInfo()

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            _isStreaming.value = true
            try {
                chatDao.deleteMessage(target.id)
                streamAssistantResponse(chatId, userMessage.content, activeModel, history)
            } finally {
                _isStreaming.value = false
            }
        }
    }

    /** Loads a user message into the composer for editing and resubmission. */
    fun startEditing(messageId: Long) {
        val message = _messages.value.firstOrNull { it.id == messageId } ?: return
        if (message.role != "user") return
        _editingMessageId.value = messageId
        promptInput.value = message.content
    }

    fun cancelEditing() {
        _editingMessageId.value = null
    }

    private fun submitEdit(chatId: Long, messageId: Long, newText: String) {
        val activeModel = modelManager.getActiveModelInfo()
        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            _isStreaming.value = true
            try {
                val existing = chatDao.getMessageById(messageId)
                if (existing != null) {
                    chatDao.updateMessage(
                        existing.copy(
                            content = newText,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                chatDao.deleteMessagesAfterId(chatId, messageId)
                chatDao.touchChat(chatId, System.currentTimeMillis())
                val history = _messages.value
                    .filter { it.id <= messageId }
                    .map { it.role to it.content }
                streamAssistantResponse(chatId, newText, activeModel, history)
            } finally {
                _isStreaming.value = false
            }
        }
    }

    // --- Personas & generation --------------------------------------------------

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

        // Editing an existing turn: update it in place and regenerate the answer.
        val editingId = _editingMessageId.value
        if (editingId != null) {
            _editingMessageId.value = null
            promptInput.value = ""
            submitEdit(chatId, editingId, text)
            return
        }

        val activeModel = modelManager.getActiveModelInfo()
        val attachedImage = _pendingImage.value
        promptInput.value = ""
        _pendingImage.value = null

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            _isStreaming.value = true
            try {
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
                        modelName = activeModel.name,
                        imageUri = attachedImage?.uriString,
                        imageMimeType = attachedImage?.mimeType
                    )
                )
                chatDao.touchChat(chatId, System.currentTimeMillis())

                // Build historical context pairs (prior turns)
                val history = _messages.value.map { it.role to it.content }

                streamAssistantResponse(chatId, text, activeModel, history, image = attachedImage)
            } finally {
                _isStreaming.value = false
            }
        }
    }

    /**
     * Streams an assistant answer for [userText] into [chatId], inserting the
     * assistant row and updating it token-by-token. Shared by send, regenerate
     * and edit-resubmit flows so provider selection stays in one place.
     */
    private suspend fun streamAssistantResponse(
        chatId: Long,
        userText: String,
        activeModel: ModelInfo,
        history: List<Pair<String, String>>,
        existingAssistantId: Long? = null,
        image: PendingImage? = null
    ) {
        // Insert Assistant Message Placeholder
        val assistantMsgId = existingAssistantId ?: chatDao.insertMessage(
            MessageEntity(
                chatId = chatId,
                role = "assistant",
                content = "",
                modelName = activeModel.name
            )
        )

        val geminiSeesImages = activeModel.family == "Gemini" &&
            com.example.services.GeminiService.hasApiKey(customGeminiApiKey.value)

        // Cloud models receive the raw image; local models get on-device OCR text
        // appended instead so "explain this screenshot" still works offline.
        var promptText = userText
        if (image != null && !geminiSeesImages) {
            val ocr = com.example.services.ImageTextRecognizer.recognizeText(image.bytes)
            promptText = if (ocr.isNotBlank()) {
                "$userText\n\n[Text recognised on-device from the attached image]\n$ocr"
            } else {
                "$userText\n\n[An image was attached; no text could be recognised in it on-device.]"
            }
        }

        // Local RAG: ground the answer in the user's own documents when enabled.
        val ragBlock = if (_ragEnabled.value) RagIndex.buildContextBlock(promptText) else ""
        val baseSystemPrompt = _inferenceConfig.value.systemPrompt
        val systemPrompt = if (ragBlock.isNotBlank()) "$baseSystemPrompt\n\n$ragBlock" else baseSystemPrompt
        val config = _inferenceConfig.value.copy(systemPrompt = systemPrompt)

        try {
            if (activeModel.family == "Gemini" && com.example.services.GeminiService.hasApiKey(customGeminiApiKey.value)) {
                val startTime = android.os.SystemClock.elapsedRealtime()
                var firstTokenTime = 0L
                var tokenCount = 0
                val accumulated = StringBuilder()

                val geminiFlow = if (image != null) {
                    com.example.services.GeminiService.streamGenerateContentWithImages(
                        prompt = userText,
                        history = history,
                        systemPrompt = systemPrompt,
                        customApiKey = customGeminiApiKey.value,
                        modelName = "gemini-2.5-flash",
                        imageParts = listOf(image.mimeType to image.bytes)
                    )
                } else {
                    com.example.services.GeminiService.streamGenerateContent(
                        prompt = userText,
                        history = history,
                        systemPrompt = systemPrompt,
                        customApiKey = customGeminiApiKey.value,
                        modelName = "gemini-2.5-flash"
                    )
                }
                geminiFlow.collect { chunk ->
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
                        messages = history + ("user" to promptText),
                        systemPrompt = systemPrompt
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
                // Stream response token-by-token from On-Device Inference Engine (tools, RAG & smart answering)
                inferenceEngine.generateStream(
                    prompt = promptText,
                    conversationHistory = history,
                    config = config
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
                    prompt = promptText,
                    conversationHistory = history,
                    config = config
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
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }

    // --- Local RAG (documents) --------------------------------------------------

    val documents: StateFlow<List<com.example.data.DocumentEntity>> = knowledgeDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _documentNotice = MutableStateFlow<String?>(null)
    val documentNotice: StateFlow<String?> = _documentNotice.asStateFlow()

    fun clearDocumentNotice() {
        _documentNotice.value = null
    }

    /** Imports a user-selected file into the local knowledge base and re-indexes. */
    fun importDocument(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.rag.DocumentImporter.importFromUri(getApplication(), uri)
            result.fold(
                onSuccess = { document ->
                    knowledgeDao.insertDocument(document)
                    rebuildRagIndex()
                    _documentNotice.value =
                        "Indexed \"${document.name}\" (${document.tokenEstimate} estimated tokens)."
                },
                onFailure = { error ->
                    _documentNotice.value = "Import failed: ${error.localizedMessage ?: "unknown error"}"
                }
            )
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            knowledgeDao.deleteDocumentById(id)
            rebuildRagIndex()
        }
    }

    fun clearAllDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            knowledgeDao.deleteAllDocuments()
            rebuildRagIndex()
        }
    }

    private suspend fun rebuildRagIndex() = withContext(Dispatchers.IO) {
        val docs = knowledgeDao.getAllDocumentsOnce()
        RagIndex.rebuild(docs.map { com.example.rag.IndexedDocument(it.id, it.name, it.text) })
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
                if (progress is BenchmarkProgress.Completed) {
                    val result = progress.result
                    viewModelScope.launch(Dispatchers.IO) {
                        benchmarkDao.insertRun(
                            BenchmarkRunEntity(
                                modelName = result.modelName,
                                family = model.family,
                                parameterCount = model.parameterCount,
                                totalTokens = result.totalTokens,
                                timeToFirstTokenMs = result.timeToFirstTokenMs,
                                totalDurationMs = result.totalDurationMs,
                                tokensPerSecond = result.tokensPerSecond,
                                peakRamMb = result.peakRamMb,
                                computeBackend = result.computeBackend,
                                performanceRating = result.performanceRating
                            )
                        )
                    }
                }
            }
        }
    }

    fun cancelBenchmark() {
        benchmarkJob?.cancel()
        _benchmarkProgress.value = BenchmarkProgress.Idle
    }

    fun clearBenchmarkHistory() {
        viewModelScope.launch(Dispatchers.IO) { benchmarkDao.clearRuns() }
    }

    fun benchmarkHistoryCsv(): String {
        val header = "timestamp,model,family,parameters,tokens,token_per_sec,ttft_ms,duration_ms,peak_ram_mb,backend,rating"
        val rows = benchmarkHistory.value.joinToString("\n") { run ->
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(run.timestampMs))
            listOf(
                ts,
                run.modelName,
                run.family,
                run.parameterCount,
                run.totalTokens.toString(),
                run.tokensPerSecond.toString(),
                run.timeToFirstTokenMs.toString(),
                run.totalDurationMs.toString(),
                run.peakRamMb.toString(),
                run.computeBackend,
                run.performanceRating
            ).joinToString(",") { "\"${it.replace("\"", "'")}\"" }
        }
        return (header + "\n" + rows + "\n")
    }
}