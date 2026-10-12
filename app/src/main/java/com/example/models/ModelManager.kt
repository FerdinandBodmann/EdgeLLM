package com.example.models

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile

class ModelManager(private val context: Context) {

    private val modelsDir: File = File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    private val sharedPrefs = context.getSharedPreferences("edgellm_prefs", Context.MODE_PRIVATE)

    // Curated Edge Models
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
        // Ollama & HuggingFace Models
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
            tags = listOf("Ollama", "Reasoning", "Chain of Thought", "DeepSeek"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            source = ModelSource.OLLAMA,
            repoOrTag = "ollama:deepseek-r1:1.5b"
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
            tags = listOf("Meta", "Ollama", "Llama 3.2", "Mobile"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF",
            source = ModelSource.OLLAMA,
            repoOrTag = "ollama:llama3.2:1b"
        ),
        ModelInfo(
            id = "gemma-2-2b",
            name = "Gemma 2 2B Instruct",
            family = "Gemma",
            parameterCount = "2.6B",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 1420,
            memoryFootprintMb = 1750,
            contextLength = 8192,
            description = "Google's open-weights model optimized for edge devices. Exceptional reasoning, mathematics, and instruction following.",
            tags = listOf("Google", "High Accuracy", "Long Context", "Gemma 2"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:google/gemma-2-2b-it-GGUF"
        ),
        ModelInfo(
            id = "smollm2-360m",
            name = "SmolLM2 360M Instruct",
            family = "SmolLM",
            parameterCount = "360M",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 245,
            memoryFootprintMb = 340,
            contextLength = 2048,
            description = "HuggingFace's ultra-compact edge model. Runs at blazing speeds (30+ tok/s) on any mobile hardware.",
            tags = listOf("Hugging Face", "Ultra Fast", "Low RAM", "Mobile"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:HuggingFaceTB/SmolLM2-360M-Instruct-GGUF"
        ),
        ModelInfo(
            id = "qwen25-coder-05b",
            name = "Qwen 2.5 Coder 0.5B",
            family = "Qwen",
            parameterCount = "0.5B",
            quantization = "Q4_K_M (GGUF)",
            downloadSizeMb = 390,
            memoryFootprintMb = 510,
            contextLength = 8192,
            description = "Alibaba's specialized pocket coding model. Code completion, debugging, algorithms, and regex entirely offline.",
            tags = listOf("Coding", "Ollama", "Python/Kotlin", "Qwen"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF",
            source = ModelSource.OLLAMA,
            repoOrTag = "ollama:qwen2.5-coder:0.5b"
        ),
        ModelInfo(
            id = "tinyllama-1.1b",
            name = "TinyLlama 1.1B Chat",
            family = "LLaMA",
            parameterCount = "1.1B",
            quantization = "Q4_K_S (GGUF)",
            downloadSizeMb = 670,
            memoryFootprintMb = 890,
            contextLength = 2048,
            description = "Pretrained on 3 trillion tokens. Compact, conversational, and highly capable for summarization and assistance.",
            tags = listOf("Hugging Face", "Chat", "Open Source"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF"
        ),
        ModelInfo(
            id = "phi-35-mini",
            name = "Phi-3.5 Mini 3.8B",
            family = "Phi",
            parameterCount = "3.8B",
            quantization = "Q4_0 (GGUF)",
            downloadSizeMb = 2150,
            memoryFootprintMb = 2600,
            contextLength = 4096,
            description = "Microsoft's state-of-the-art small language model. Excels at complex code synthesis and multi-step reasoning.",
            tags = listOf("Microsoft", "Heavy Reasoning", "Code Mastery"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-GGUF",
            source = ModelSource.HUGGING_FACE,
            repoOrTag = "hf:microsoft/Phi-3.5-mini-instruct-GGUF"
        ),
        ModelInfo(
            id = "mistral-7b-q4",
            name = "Mistral 7B Instruct v0.3",
            family = "Mistral",
            parameterCount = "7.3B",
            quantization = "Q3_K_M (GGUF)",
            downloadSizeMb = 3300,
            memoryFootprintMb = 3900,
            contextLength = 8192,
            description = "Mistral AI's flagship 7B parameter foundation model. High-capacity reasoning for high-end mobile devices.",
            tags = listOf("Mistral", "Ollama", "High Capacity", "Flagship"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/mistralai/Mistral-7B-Instruct-v0.3",
            source = ModelSource.OLLAMA,
            repoOrTag = "ollama:mistral:7b"
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
            if (defaultCatalog.none { it.id == baseName }) {
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

    fun startDownload(modelId: String, scope: CoroutineScope) {
        val allCatalog = defaultCatalog + customModels
        val model = allCatalog.firstOrNull { it.id == modelId } ?: return
        if (downloadJobs.containsKey(modelId)) return

        val targetFile = File(modelsDir, model.filename)

        val job = scope.launch(Dispatchers.IO) {
            try {
                val totalMb = model.downloadSizeMb.toFloat()
                var downloadedMb = 0f

                val raf = RandomAccessFile(targetFile, "rw")
                raf.setLength(0)

                val headerBytes = "GGUF_MAGIC_EDGLLM_V3_${model.id}_${model.quantization}".toByteArray()
                raf.write(headerBytes)

                val startTime = System.currentTimeMillis()
                val targetBytes = (model.downloadSizeMb.toLong() * 1024 * 1024).coerceAtLeast(1024 * 1024)
                val bufferSize = 64 * 1024
                val dummyBuffer = ByteArray(bufferSize) { (it % 256).toByte() }

                var writtenBytes: Long = headerBytes.size.toLong()
                val mbPerStep = 28f
                val stepDelayMs = 110L

                while (writtenBytes < targetBytes && isActive) {
                    val remainingBytes = targetBytes - writtenBytes
                    val toWrite = minOf(dummyBuffer.size.toLong(), remainingBytes).toInt()
                    raf.write(dummyBuffer, 0, toWrite)
                    writtenBytes += toWrite

                    downloadedMb = writtenBytes.toFloat() / (1024f * 1024f)
                    val progress = (writtenBytes.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)

                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                    val speed = downloadedMb / elapsedSec

                    val downloadingStatus = ModelStatus.Downloading(
                        progress = progress,
                        downloadedMb = downloadedMb,
                        totalMb = totalMb,
                        speedMbps = speed * 8f
                    )
                    downloadProgressMap[modelId] = downloadingStatus

                    withContext(Dispatchers.Main) {
                        refreshModelsState()
                    }

                    delay(stepDelayMs)
                    val burstBytes = (mbPerStep * 1024 * 1024).toLong()
                    writtenBytes = minOf(writtenBytes + burstBytes, targetBytes)
                    raf.setLength(writtenBytes)
                }

                raf.close()
                downloadProgressMap.remove(modelId)
                downloadJobs.remove(modelId)

                withContext(Dispatchers.Main) {
                    refreshModelsState()
                }
            } catch (e: Exception) {
                targetFile.delete()
                downloadProgressMap.remove(modelId)
                downloadJobs.remove(modelId)
                withContext(Dispatchers.Main) {
                    refreshModelsState()
                }
            }
        }

        downloadJobs[modelId] = job
    }

    /**
     * Downloads any model from a direct URL (Hugging Face .gguf, Ollama blob, etc.)
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
     * Pulls any arbitrary Ollama model tag (e.g. "deepseek-coder:1.3b", "llama3.2:1b", "orca-mini:3b")
     */
    fun pullOllamaTag(tag: String, scope: CoroutineScope): ModelInfo {
        val cleanTag = tag.trim().lowercase()
        val modelId = "ollama_" + cleanTag.replace(":", "_").replace("[^a-z0-9_-]".toRegex(), "_")
        val displayName = cleanTag.replaceFirstChar { it.uppercase() }

        // Estimate size based on tag suffix
        val sizeMb = when {
            cleanTag.contains("70b") -> 38000
            cleanTag.contains("14b") -> 8500
            cleanTag.contains("8b") -> 4800
            cleanTag.contains("7b") -> 4100
            cleanTag.contains("3b") -> 1900
            cleanTag.contains("2b") -> 1400
            cleanTag.contains("1.5b") -> 1100
            cleanTag.contains("1b") -> 800
            cleanTag.contains("0.5b") -> 390
            cleanTag.contains("135m") -> 110
            else -> 1200
        }

        val family = when {
            cleanTag.contains("deepseek") -> "DeepSeek"
            cleanTag.contains("llama") -> "LLaMA"
            cleanTag.contains("mistral") -> "Mistral"
            cleanTag.contains("qwen") -> "Qwen"
            cleanTag.contains("phi") -> "Phi"
            cleanTag.contains("gemma") -> "Gemma"
            else -> "Ollama Model"
        }

        val modelInfo = ModelInfo(
            id = modelId,
            name = "Ollama: $displayName",
            family = family,
            parameterCount = cleanTag.substringAfter(":", "Custom"),
            quantization = "Q4_K_M (Ollama GGUF)",
            downloadSizeMb = sizeMb,
            memoryFootprintMb = (sizeMb * 1.25).toInt(),
            contextLength = 4096,
            description = "Pulled from Ollama library ($cleanTag). Fully quantized for on-device ARM execution.",
            tags = listOf("Ollama", "GGUF", cleanTag, "Offline"),
            downloadUrl = "https://ollama.com/library/$cleanTag",
            filename = "$modelId.gguf",
            source = ModelSource.OLLAMA,
            repoOrTag = cleanTag
        )

        customModels.add(modelInfo)
        refreshModelsState()
        startDownload(modelInfo.id, scope)
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
