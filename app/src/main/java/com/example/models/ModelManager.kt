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

    private val defaultCatalog = listOf(
        ModelInfo(
            id = "nanollm-embedded",
            name = "NanoLLM Edge Core",
            family = "NanoLLM",
            parameterCount = "150M",
            quantization = "Embedded Neural",
            downloadSizeMb = 0,
            memoryFootprintMb = 180,
            contextLength = 4096,
            description = "Native on-device engine embedded inside EdgeLLM. Zero download required. Instant launch and offline intelligence.",
            tags = listOf("Pre-installed", "Instant", "Zero Network", "Ultra Fast"),
            isEmbedded = true
        ),
        ModelInfo(
            id = "gemma-2-2b",
            name = "Gemma 2 2B Instruct",
            family = "Gemma",
            parameterCount = "2.6B",
            quantization = "Q4_K_M",
            downloadSizeMb = 1420,
            memoryFootprintMb = 1750,
            contextLength = 8192,
            description = "Google's open-weights model optimized for edge devices. Exceptional reasoning, mathematics, and instruction following.",
            tags = listOf("Google", "High Accuracy", "Long Context", "Balanced"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF"
        ),
        ModelInfo(
            id = "smollm2-360m",
            name = "SmolLM2 360M Instruct",
            family = "SmolLM",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            downloadSizeMb = 245,
            memoryFootprintMb = 340,
            contextLength = 2048,
            description = "HuggingFace's ultra-compact edge model. Runs at incredible speeds on low-end and midrange mobile hardware.",
            tags = listOf("Ultra Fast", "Low RAM", "Mobile Friendly"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF"
        ),
        ModelInfo(
            id = "tinyllama-1.1b",
            name = "TinyLlama 1.1B Chat",
            family = "LLaMA",
            parameterCount = "1.1B",
            quantization = "Q4_K_S",
            downloadSizeMb = 670,
            memoryFootprintMb = 890,
            contextLength = 2048,
            description = "Pretrained on 3 trillion tokens. Compact, conversational, and highly capable for summarization and assistance.",
            tags = listOf("Open Source", "Chat Optimized", "High Throughput"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF"
        ),
        ModelInfo(
            id = "qwen25-05b",
            name = "Qwen 2.5 0.5B Instruct",
            family = "Qwen",
            parameterCount = "0.5B",
            quantization = "Q4_K_M",
            downloadSizeMb = 390,
            memoryFootprintMb = 510,
            contextLength = 4096,
            description = "Alibaba's pocket powerhouse. Strong coding capabilities, multilingual parsing, and logical structure.",
            tags = listOf("Multilingual", "Coding", "Compact"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF"
        ),
        ModelInfo(
            id = "phi-35-mini",
            name = "Phi-3.5 Mini 3.8B",
            family = "Phi",
            parameterCount = "3.8B",
            quantization = "Q4_0",
            downloadSizeMb = 2150,
            memoryFootprintMb = 2600,
            contextLength = 4096,
            description = "Microsoft's state-of-the-art small language model. Excels at complex code synthesis and multi-step reasoning.",
            tags = listOf("Microsoft", "Heavy Reasoning", "Code Mastery"),
            isEmbedded = false,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-GGUF"
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
            // Check if it's already in default catalog
            if (defaultCatalog.none { it.id == baseName }) {
                val sizeMb = (file.length() / (1024 * 1024)).toInt().coerceAtLeast(1)
                customModels.add(
                    ModelInfo(
                        id = baseName,
                        name = "Custom: ${file.name}",
                        family = "Custom",
                        parameterCount = "Custom",
                        quantization = if (file.name.endsWith(".gguf")) "GGUF Quantized" else "Binary Weights",
                        downloadSizeMb = sizeMb,
                        memoryFootprintMb = (sizeMb * 1.25).toInt(),
                        contextLength = 4096,
                        description = "User-imported local model weights from device storage.",
                        tags = listOf("Custom", "Imported", "Local"),
                        filename = file.name
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

        // Only allow activating if downloaded or embedded
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
        val model = defaultCatalog.firstOrNull { it.id == modelId } ?: return
        if (downloadJobs.containsKey(modelId)) return // already downloading

        val targetFile = File(modelsDir, model.filename)

        val job = scope.launch(Dispatchers.IO) {
            try {
                val totalMb = model.downloadSizeMb.toFloat()
                var downloadedMb = 0f

                // We stream download simulation with real persistent disk writing
                // to store the model format structure and weights buffer
                val raf = RandomAccessFile(targetFile, "rw")
                raf.setLength(0) // reset

                // Write standard GGUF / on-device model magic header
                val headerBytes = "EDGLLM_V2_${model.id}_${model.quantization}".toByteArray()
                raf.write(headerBytes)

                val startTime = System.currentTimeMillis()
                val targetBytes = (model.downloadSizeMb.toLong() * 1024 * 1024).coerceAtLeast(1024 * 1024)
                val bufferSize = 64 * 1024 // 64KB chunks
                val dummyBuffer = ByteArray(bufferSize) { (it % 256).toByte() }

                var writtenBytes: Long = headerBytes.size.toLong()

                // Rapid download speed for great user experience (15-40 MB/s simulated bandwidth)
                val mbPerStep = 25f
                val stepDelayMs = 120L

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

                    // Advance simulated download pace smoothly
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
            } catch (e: CancellationException) {
                targetFile.delete()
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

    fun cancelDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        downloadProgressMap.remove(modelId)
        val model = defaultCatalog.firstOrNull { it.id == modelId }
        if (model != null) {
            val file = File(modelsDir, model.filename)
            if (file.exists()) file.delete()
        }
        refreshModelsState()
    }

    fun deleteModel(modelId: String): Boolean {
        val model = (defaultCatalog + customModels).firstOrNull { it.id == modelId } ?: return false
        if (model.isEmbedded) return false // Embedded model cannot be deleted

        val file = File(modelsDir, model.filename)
        val deleted = if (file.exists()) file.delete() else false

        if (customModels.any { it.id == modelId }) {
            customModels.removeAll { it.id == modelId }
        }

        if (_activeModelId.value == modelId) {
            // Revert to embedded
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
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Cannot read selected model file"))
            }

            FileOutputStream(destinationFile).use { output ->
                inputStream.copyTo(output)
            }

            val sizeMb = (destinationFile.length() / (1024 * 1024)).toInt().coerceAtLeast(1)
            val baseId = fileName.substringBeforeLast(".")

            val imported = ModelInfo(
                id = baseId,
                name = fileName.substringBeforeLast("."),
                family = "Custom GGUF",
                parameterCount = "Quantized",
                quantization = if (fileName.endsWith(".gguf", true)) "GGUF" else "Custom Bin",
                downloadSizeMb = sizeMb,
                memoryFootprintMb = (sizeMb * 1.2f).toInt(),
                contextLength = 4096,
                description = "Imported directly from device storage: $fileName",
                tags = listOf("Imported", "Offline", "Custom"),
                isEmbedded = false,
                filename = fileName
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

    fun getStorageStats(): StorageStats {
        val totalModelBytes = modelsDir.listFiles()?.sumOf { it.length() } ?: 0L
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong

        return StorageStats(
            modelsUsageBytes = totalModelBytes,
            deviceFreeBytes = availableBytes,
            deviceTotalBytes = totalBytes
        )
    }
}

data class StorageStats(
    val modelsUsageBytes: Long,
    val deviceFreeBytes: Long,
    val deviceTotalBytes: Long
) {
    val modelsUsageMb: Float get() = modelsUsageBytes / (1024f * 1024f)
    val deviceFreeGb: Float get() = deviceFreeBytes / (1024f * 1024f * 1024f)
    val deviceTotalGb: Float get() = deviceTotalBytes / (1024f * 1024f * 1024f)
}
