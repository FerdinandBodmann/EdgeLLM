package com.example.models

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ModelManager(private val context: Context) {

    private val modelsDir: File = File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    private val sharedPrefs = context.getSharedPreferences("edgellm_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val _downloadMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val downloadMessages: SharedFlow<String> = _downloadMessages.asSharedFlow()

    // Configurable Ollama Host endpoint
    val ollamaHostUrl = MutableStateFlow(
        sharedPrefs.getString("ollama_host", "http://10.0.2.2:11434") ?: "http://10.0.2.2:11434"
    )

    fun updateOllamaHost(newHost: String) {
        val trimmed = newHost.trim().trimEnd('/')
        sharedPrefs.edit().putString("ollama_host", trimmed).apply()
        ollamaHostUrl.value = trimmed
    }

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Curated Edge Models with Real Hugging Face Direct GGUF Download URLs
    private val defaultCatalog = mutableListOf(
        ModelInfo(
            id = "nanollm-embedded",
            name = "NanoLLM Edge Core",
            family = "NanoLLM",
            parameterCount = "150M",
            quantization = "Embedded Neural",
            downloadSizeMb = 0,
            memoryFootprintMb = 180,
            contextLength = 4096,
            description = "Native on-device engine embedded inside Synapse. Zero download required. Instant launch and offline intelligence.",
            tags = listOf("Pre-installed", "Instant", "Zero Network", "Ultra Fast"),
            isEmbedded = true,
            source = ModelSource.CURATED
        ),
        ModelInfo(
            id = "gemini-25-flash",
            name = "Gemini 2.5 Flash",
            family = "Gemini",
            parameterCount = "Cloud AI",
            quantization = "Google Cloud",
            downloadSizeMb = 0,
            memoryFootprintMb = 35,
            contextLength = 32768,
            description = "Google's ultra-fast multimodal reasoning model. Answers any question with live world knowledge, code generation, and deep explanations.",
            tags = listOf("Google", "Real-Time AI", "Cloud", "Live Knowledge"),
            isEmbedded = true,
            source = ModelSource.CURATED
        ),
        ModelInfo(
            id = "smollm2-135m",
            name = "SmolLM2 135M Instruct",
            family = "SmolLM",
            parameterCount = "135M",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 92,
            memoryFootprintMb = 140,
            contextLength = 2048,
            description = "Hugging Face's ultra-compact 135M parameter mobile edge model. Downloads quickly (~90MB) and operates efficiently on mobile.",
            tags = listOf("Hugging Face", "Real Download", "Fast", "Mobile"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
            filename = "smollm2-135m-instruct-q4_k_m.gguf",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:HuggingFaceTB/SmolLM2-135M-Instruct-GGUF"
        ),
        ModelInfo(
            id = "qwen25-05b",
            name = "Qwen 2.5 0.5B Instruct",
            family = "Qwen",
            parameterCount = "0.5B",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 395,
            memoryFootprintMb = 510,
            contextLength = 4096,
            description = "Alibaba's specialized 0.5B instruction-tuned model. Multilingual and coding knowledge formatted as native GGUF.",
            tags = listOf("Qwen", "Real Download", "Coding", "0.5B"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:Qwen/Qwen2.5-0.5B-Instruct-GGUF"
        ),
        ModelInfo(
            id = "llama32-1b",
            name = "Llama 3.2 1B Instruct",
            family = "LLaMA",
            parameterCount = "1.2B",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 850,
            memoryFootprintMb = 1100,
            contextLength = 8192,
            description = "Meta's official compact edge model. Built for on-device reasoning, summarization, and multilingual instruction following.",
            tags = listOf("Meta", "Llama 3.2", "Mobile", "GGUF"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:bartowski/Llama-3.2-1B-Instruct-GGUF"
        ),
        ModelInfo(
            id = "deepseek-r1-15b",
            name = "DeepSeek R1 Distill 1.5B",
            family = "DeepSeek",
            parameterCount = "1.5B",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 1120,
            memoryFootprintMb = 1450,
            contextLength = 8192,
            description = "DeepSeek's breakout reasoning model. Produces transparent step-by-step thinking tokens (<think>...</think>) before answering.",
            tags = listOf("DeepSeek", "Reasoning", "Chain of Thought", "R1"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF"
        ),
        ModelInfo(
            id = "ollama-connected-model",
            name = "Ollama Local Engine",
            family = "Ollama",
            parameterCount = "Variable",
            quantization = "Host Managed",
            downloadSizeMb = 0,
            memoryFootprintMb = 120,
            contextLength = 8192,
            description = "Connects directly to your local Ollama server over Wi-Fi/LAN (e.g. gpt-oss, llama3.2, deepseek-r1) for genuine multi-billion parameter inference.",
            tags = listOf("Ollama", "Live Server", "Local LAN", "Real Weights"),
            isEmbedded = true,
            source = ModelSource.OLLAMA,
            repoOrTag = "ollama:host"
        )
    )

    private val customModels = mutableListOf<ModelInfo>()

    private val _modelsState = MutableStateFlow<List<ModelItem>>(emptyList())
    val modelsState: StateFlow<List<ModelItem>> = _modelsState.asStateFlow()

    private val _activeModelId = MutableStateFlow(
        sharedPrefs.getString("active_model_id", "nanollm-embedded") ?: "nanollm-embedded"
    )
    val activeModelId: StateFlow<String> = _activeModelId.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val downloadProgressMap = mutableMapOf<String, ModelStatus.Downloading>()

    init {
        loadCustomModelsFromDisk()
        refreshModelsState()
    }

    private fun loadCustomModelsFromDisk() {
        val files = modelsDir.listFiles { _, name ->
            name.endsWith(".bin") || name.endsWith(".gguf") || name.endsWith(".task")
        } ?: emptyArray()

        for (file in files) {
            val baseName = file.nameWithoutExtension
            if (defaultCatalog.none { it.id == baseName || it.filename == file.name }) {
                val sizeMb = (file.length() / (1024 * 1024)).toInt().coerceAtLeast(1)
                customModels.add(
                    ModelInfo(
                        id = baseName,
                        name = "Custom: ${file.name}",
                        family = if (file.name.contains("llama", true)) "LLaMA"
                                 else if (file.name.contains("deepseek", true)) "DeepSeek"
                                 else if (file.name.contains("mistral", true)) "Mistral"
                                 else "Custom GGUF",
                        parameterCount = "Quantized",
                        quantization = if (file.name.endsWith(".gguf")) "GGUF Quantized" else "Binary Weights",
                        downloadSizeMb = sizeMb,
                        memoryFootprintMb = (sizeMb * 1.25).toInt(),
                        contextLength = 4096,
                        description = "User-imported or downloaded model from web repository: ${file.name}",
                        tags = listOf("Custom", "Imported", "Local"),
                        filename = file.name,
                        source = ModelSource.LOCAL_FILE
                    )
                )
            }
        }
    }

    fun refreshModelsState() {
        val activeId = _activeModelId.value
        val allCatalog = defaultCatalog + customModels
        val items = allCatalog.map { info ->
            val status = getModelStatus(info)
            ModelItem(
                info = info,
                status = status,
                isActive = (info.id == activeId)
            )
        }
        _modelsState.value = items
    }

    private fun getModelStatus(info: ModelInfo): ModelStatus {
        if (info.isEmbedded) {
            return ModelStatus.Downloaded(
                file = File(modelsDir, "embedded_weights.bin"),
                fileSizeBytes = 180L * 1024 * 1024
            )
        }

        downloadProgressMap[info.id]?.let {
            return it
        }

        val file = File(modelsDir, info.filename)
        return if (file.exists() && file.length() > 0) {
            ModelStatus.Downloaded(file, file.length())
        } else {
            ModelStatus.NotDownloaded
        }
    }

    fun setActiveModel(modelId: String) {
        val allCatalog = defaultCatalog + customModels
        val model = allCatalog.firstOrNull { it.id == modelId } ?: return
        val status = getModelStatus(model)

        if (status is ModelStatus.Downloaded || model.isEmbedded) {
            _activeModelId.value = modelId
            sharedPrefs.edit().putString("active_model_id", modelId).apply()
            refreshModelsState()
        }
    }

    fun getActiveModelInfo(): ModelInfo {
        val allCatalog = defaultCatalog + customModels
        return allCatalog.firstOrNull { it.id == _activeModelId.value }
            ?: defaultCatalog.first()
    }

    /**
     * Downloads real GGUF weights via HTTPS using OkHttp with network verification.
     */
    fun startDownload(modelId: String, scope: CoroutineScope) {
        val allCatalog = defaultCatalog + customModels
        val model = allCatalog.firstOrNull { it.id == modelId } ?: return
        if (downloadJobs.containsKey(modelId)) return

        // 1. Check network connectivity before starting
        if (!isNetworkConnected()) {
            _downloadMessages.tryEmit("Cannot download ${model.name}: No active internet connection. Please connect to Wi-Fi.")
            return
        }

        val url = model.downloadUrl
        if (url.isBlank()) {
            _downloadMessages.tryEmit("Model '${model.name}' does not have a direct download URL.")
            return
        }

        val targetFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.part")

        // Guardrails: don't start a download the device cannot store or hold.
        val freeStorageMb = com.example.util.DeviceCapabilities.freeStorageMb(context)
        val alreadyDownloadedMb = if (tempFile.exists()) tempFile.length() / (1024L * 1024L) else 0L
        val remainingMb = (model.downloadSizeMb.toLong() - alreadyDownloadedMb).coerceAtLeast(0L)
        if (freeStorageMb > 0 && freeStorageMb < remainingMb) {
            _downloadMessages.tryEmit(
                "Not enough storage for ${model.name}: needs ~${remainingMb} MB but only ${freeStorageMb} MB free."
            )
            return
        }
        if (!com.example.util.DeviceCapabilities.fitsInMemory(context, model.memoryFootprintMb)) {
            _downloadMessages.tryEmit(
                "Note: ${model.name} needs ~${model.memoryFootprintMb} MB RAM, which may exceed this device."
            )
        }

        val job = scope.launch(Dispatchers.IO) {
            try {
                val resumeFrom = if (tempFile.exists()) tempFile.length() else 0L
                if (resumeFrom > 0) {
                    _downloadMessages.tryEmit("Resuming ${model.name} from ${resumeFrom / (1024 * 1024)} MB...")
                } else {
                    _downloadMessages.tryEmit("Connecting to Hugging Face CDN for ${model.name}...")
                }

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Synapse-Android-EdgeLLM/1.0")
                if (resumeFrom > 0) {
                    requestBuilder.header("Range", "bytes=$resumeFrom-")
                }
                val request = requestBuilder.build()

                val response = okHttpClient.newCall(request).execute()

                val resumed = resumeFrom > 0 && response.code == 206
                if (!response.isSuccessful) {
                    throw IOException("HTTP server returned error ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body from model repository")
                val contentLength = body.contentLength()
                val fallbackTotal = model.downloadSizeMb.toLong() * 1024 * 1024
                val totalBytes = when {
                    resumed && contentLength > 0 -> resumeFrom + contentLength
                    contentLength > 0 -> contentLength
                    else -> fallbackTotal
                }

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile, resumed)
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalRead: Long = if (resumed) resumeFrom else 0L
                val startBytes = totalRead
                val startTime = System.currentTimeMillis()
                var lastUiUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdate > 300) {
                        lastUiUpdate = now
                        val downloadedMb = totalRead.toFloat() / (1024f * 1024f)
                        val totalMb = totalBytes.toFloat() / (1024f * 1024f)
                        val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        val elapsedSec = ((now - startTime) / 1000f).coerceAtLeast(0.1f)
                        val speedMbps = ((totalRead - startBytes).toFloat() / (1024f * 1024f) / elapsedSec) * 8f

                        downloadProgressMap[modelId] = ModelStatus.Downloading(
                            progress = progress,
                            downloadedMb = downloadedMb,
                            totalMb = totalMb,
                            speedMbps = speedMbps
                        )
                        withContext(Dispatchers.Main) {
                            refreshModelsState()
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (isActive) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    val digest = sha256(targetFile)
                    writeChecksum(targetFile, digest)
                    val verification = if (digest.isBlank()) "" else " · SHA-256 ${digest.take(12)}…"
                    _downloadMessages.tryEmit(
                        "Successfully downloaded ${model.name} (${targetFile.length() / (1024 * 1024)} MB)$verification"
                    )
                } else {
                    // Cancelled mid-flight: keep the partial file so the next
                    // attempt can resume with an HTTP Range request.
                    _downloadMessages.tryEmit("Paused ${model.name}; partial file kept for resume.")
                }

                downloadProgressMap.remove(modelId)
                downloadJobs.remove(modelId)

                withContext(Dispatchers.Main) {
                    refreshModelsState()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                downloadProgressMap.remove(modelId)
                downloadJobs.remove(modelId)
                _downloadMessages.tryEmit("Download failed: ${e.localizedMessage ?: "Network error"}")
                withContext(Dispatchers.Main) {
                    refreshModelsState()
                }
            }
        }

        downloadJobs[modelId] = job
    }

    /**
     * Downloads any model from a direct URL (Hugging Face .gguf, direct HTTPS link).
     */
    fun downloadFromDirectUrl(
        url: String,
        customName: String?,
        paramSize: String = "Quantized",
        sizeMb: Int = 800,
        scope: CoroutineScope
    ): ModelInfo {
        val trimmedUrl = url.trim()
        val derivedFileName = trimmedUrl.substringAfterLast("/").substringBefore("?").ifEmpty {
            "custom_model_${System.currentTimeMillis()}.gguf"
        }
        val safeFileName = if (derivedFileName.endsWith(".gguf") || derivedFileName.endsWith(".bin")) {
            derivedFileName
        } else {
            "$derivedFileName.gguf"
        }

        val modelId = safeFileName.substringBeforeLast(".").lowercase().replace("[^a-z0-9_-]".toRegex(), "_")
        val displayName = customName?.takeIf { it.isNotBlank() } ?: safeFileName.substringBeforeLast(".")

        val isHuggingFace = trimmedUrl.contains("huggingface.co", ignoreCase = true)
        val source = if (isHuggingFace) ModelSource.HUGGING_FACE else ModelSource.CUSTOM_URL

        val modelInfo = ModelInfo(
            id = modelId,
            name = displayName,
            family = if (displayName.contains("llama", true)) "LLaMA"
                     else if (displayName.contains("deepseek", true)) "DeepSeek"
                     else if (displayName.contains("mistral", true)) "Mistral"
                     else "GGUF Model",
            parameterCount = paramSize,
            quantization = "GGUF Q4",
            downloadSizeMb = sizeMb,
            memoryFootprintMb = (sizeMb * 1.25).toInt(),
            contextLength = 4096,
            description = "Downloaded from URL: $trimmedUrl",
            tags = listOf(if (isHuggingFace) "Hugging Face" else "Custom URL", "GGUF", "Offline"),
            downloadUrl = trimmedUrl,
            filename = safeFileName,
            source = source,
            repoOrTag = trimmedUrl
        )

        customModels.add(modelInfo)
        refreshModelsState()
        startDownload(modelInfo.id, scope)
        return modelInfo
    }

    /**
     * Pulls or configures an Ollama model tag.
     */
    fun pullOllamaTag(tag: String, scope: CoroutineScope): ModelInfo {
        val cleanTag = tag.trim().lowercase()
        val modelId = "ollama_" + cleanTag.replace(":", "_").replace("[^a-z0-9_-]".toRegex(), "_")
        val displayName = cleanTag.replaceFirstChar { it.uppercase() }

        val sizeMb = when {
            cleanTag.contains("70b") -> 38000
            cleanTag.contains("20b") -> 12500
            cleanTag.contains("14b") -> 8500
            cleanTag.contains("8b") -> 4800
            cleanTag.contains("7b") -> 4100
            cleanTag.contains("3b") -> 1900
            cleanTag.contains("2b") -> 1400
            cleanTag.contains("1.5b") -> 1100
            cleanTag.contains("1b") -> 800
            else -> 1200
        }

        val family = when {
            cleanTag.contains("deepseek") -> "DeepSeek"
            cleanTag.contains("llama") -> "LLaMA"
            cleanTag.contains("mistral") -> "Mistral"
            cleanTag.contains("qwen") -> "Qwen"
            cleanTag.contains("gpt-oss") -> "GPT-OSS"
            else -> "Ollama Model"
        }

        val modelInfo = ModelInfo(
            id = modelId,
            name = "Ollama: $displayName",
            family = family,
            parameterCount = cleanTag.substringAfter(":", "Custom"),
            quantization = "Host Managed (Ollama)",
            downloadSizeMb = sizeMb,
            memoryFootprintMb = (sizeMb * 1.25).toInt(),
            contextLength = 8192,
            description = "Ollama server model: $cleanTag. Executes via Ollama API on your local network/host.",
            tags = listOf("Ollama", "Live Server", cleanTag),
            downloadUrl = "",
            filename = "$modelId.gguf",
            source = ModelSource.OLLAMA,
            repoOrTag = cleanTag
        )

        customModels.add(modelInfo)
        refreshModelsState()

        // Trigger real pull on Ollama host if reachable
        scope.launch(Dispatchers.IO) {
            try {
                val host = ollamaHostUrl.value
                _downloadMessages.tryEmit("Pulling $cleanTag on Ollama server ($host)...")
                com.example.services.OllamaService.pullModel(host, cleanTag).collect { progress ->
                    val downloadingStatus = ModelStatus.Downloading(
                        progress = progress.progress,
                        downloadedMb = progress.completedBytes.toFloat() / (1024f * 1024f),
                        totalMb = progress.totalBytes.toFloat() / (1024f * 1024f),
                        speedMbps = 0f
                    )
                    downloadProgressMap[modelId] = downloadingStatus
                    withContext(Dispatchers.Main) {
                        refreshModelsState()
                    }
                }
                downloadProgressMap.remove(modelId)
                _downloadMessages.tryEmit("Ollama model $cleanTag is ready on host.")
                withContext(Dispatchers.Main) {
                    setActiveModel(modelId)
                    refreshModelsState()
                }
            } catch (e: Exception) {
                downloadProgressMap.remove(modelId)
                _downloadMessages.tryEmit("Ollama pull note: ${e.localizedMessage ?: "Ensure Ollama is running"}")
                withContext(Dispatchers.Main) {
                    refreshModelsState()
                }
            }
        }

        return modelInfo
    }

    fun cancelDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        downloadProgressMap.remove(modelId)
        val allCatalog = defaultCatalog + customModels
        val model = allCatalog.firstOrNull { it.id == modelId }
        if (model != null) {
            val file = File(modelsDir, model.filename)
            if (file.exists()) file.delete()
            val part = File(modelsDir, "${model.filename}.part")
            if (part.exists()) part.delete()
        }
        refreshModelsState()
    }

    fun deleteModel(modelId: String): Boolean {
        val allCatalog = defaultCatalog + customModels
        val model = allCatalog.firstOrNull { it.id == modelId } ?: return false
        if (model.isEmbedded) return false

        val file = File(modelsDir, model.filename)
        val deleted = if (file.exists()) file.delete() else false

        customModels.removeAll { it.id == modelId }

        if (_activeModelId.value == modelId) {
            setActiveModel("nanollm-embedded")
        }

        refreshModelsState()
        return deleted
    }

    suspend fun importCustomModel(uri: Uri): Result<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_model_${System.currentTimeMillis()}.gguf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }

            val destinationFile = File(modelsDir, fileName)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot read selected model file"))

            FileOutputStream(destinationFile).use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            val sizeMb = (destinationFile.length() / (1024 * 1024)).toInt().coerceAtLeast(1)
            val baseId = fileName.substringBeforeLast(".")

            val imported = ModelInfo(
                id = baseId,
                name = fileName.substringBeforeLast("."),
                family = "Custom GGUF",
                parameterCount = "Quantized",
                quantization = if (fileName.endsWith(".gguf", true)) "GGUF" else "Binary Weights",
                downloadSizeMb = sizeMb,
                memoryFootprintMb = (sizeMb * 1.2f).toInt(),
                contextLength = 4096,
                description = "Imported directly from device storage: $fileName",
                tags = listOf("Imported", "Offline", "Custom"),
                isEmbedded = false,
                filename = fileName,
                source = ModelSource.LOCAL_FILE
            )

            customModels.add(imported)
            withContext(Dispatchers.Main) {
                setActiveModel(imported.id)
                refreshModelsState()
            }
            Result.success(imported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getModelsDirectoryPath(): String = modelsDir.absolutePath

    fun getDownloadedModelFiles(): List<DownloadedModelFile> {
        val allCatalog = defaultCatalog + customModels
        val result = mutableListOf<DownloadedModelFile>()

        result.add(
            DownloadedModelFile(
                id = "nanollm-embedded",
                name = "NanoLLM Edge Core",
                filename = "embedded_weights.bin (In-App Asset)",
                filePath = File(modelsDir, "embedded_weights.bin").absolutePath,
                sizeBytes = 180L * 1024 * 1024,
                isEmbedded = true,
                isActive = (_activeModelId.value == "nanollm-embedded")
            )
        )

        val files = modelsDir.listFiles { _, name ->
            name.endsWith(".bin") || name.endsWith(".gguf") || name.endsWith(".task")
        } ?: emptyArray()

        for (file in files) {
            val matchingModel = allCatalog.firstOrNull { it.filename == file.name }
            val displayName = matchingModel?.name ?: file.nameWithoutExtension
            val modelId = matchingModel?.id ?: file.nameWithoutExtension
            result.add(
                DownloadedModelFile(
                    id = modelId,
                    name = displayName,
                    filename = file.name,
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    isEmbedded = false,
                    isActive = (_activeModelId.value == modelId)
                )
            )
        }
        return result
    }

    fun clearAllDownloadedModels(): Int {
        val files = modelsDir.listFiles { _, name ->
            name.endsWith(".bin") || name.endsWith(".gguf") || name.endsWith(".task")
        } ?: emptyArray()

        var deletedCount = 0
        for (file in files) {
            if (file.delete()) {
                deletedCount++
            }
        }
        customModels.clear()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        downloadProgressMap.clear()
        setActiveModel("nanollm-embedded")
        refreshModelsState()
        return deletedCount
    }

    fun getStorageStats(): StorageStats {
        val totalModelBytes = (modelsDir.listFiles()?.sumOf { it.length() } ?: 0L) + (180L * 1024 * 1024)
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val downloadedCount = (modelsDir.listFiles { _, name ->
            name.endsWith(".bin") || name.endsWith(".gguf") || name.endsWith(".task")
        }?.size ?: 0) + 1

        return StorageStats(
            modelsUsageBytes = totalModelBytes,
            deviceFreeBytes = availableBytes,
            deviceTotalBytes = totalBytes,
            downloadedModelsCount = downloadedCount,
            activeDownloadsCount = downloadProgressMap.size,
            modelsDirectoryPath = modelsDir.absolutePath
        )
    }
}

/** Lowercase hex SHA-256 of a file, or "" when it cannot be read. */
private fun sha256(file: File): String {
    if (!file.exists()) return ""
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (t: Throwable) {
        ""
    }
}

/** Stores a `<file>.sha256` sidecar used to verify a download later. */
private fun writeChecksum(file: File, digest: String) {
    if (digest.isBlank()) return
    try {
        File(file.parentFile, "${file.name}.sha256").writeText(digest)
    } catch (t: Throwable) {
        // The sidecar is advisory; a write failure must not break the download.
    }
}

data class DownloadedModelFile(
    val id: String,
    val name: String,
    val filename: String,
    val filePath: String,
    val sizeBytes: Long,
    val isEmbedded: Boolean,
    val isActive: Boolean
) {
    val sizeMb: Float get() = sizeBytes / (1024f * 1024f)
    val formattedSize: String
        get() = if (sizeMb >= 1000f) "%.2f GB".format(sizeMb / 1024f) else "%.1f MB".format(sizeMb)
}

data class StorageStats(
    val modelsUsageBytes: Long,
    val deviceFreeBytes: Long,
    val deviceTotalBytes: Long,
    val downloadedModelsCount: Int = 1,
    val activeDownloadsCount: Int = 0,
    val modelsDirectoryPath: String = ""
) {
    val modelsUsageMb: Float get() = modelsUsageBytes / (1024f * 1024f)
    val deviceFreeGb: Float get() = deviceFreeBytes / (1024f * 1024f * 1024f)
    val deviceTotalGb: Float get() = deviceTotalBytes / (1024f * 1024f * 1024f)
}
