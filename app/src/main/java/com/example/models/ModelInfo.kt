package com.example.models

import java.io.File

enum class ModelSource {
    CURATED,
    OLLAMA,
    HUGGING_FACE,
    CUSTOM_URL,
    LOCAL_FILE
}

data class ModelInfo(
    val id: String,
    val name: String,
    val family: String, // "Gemma", "LLaMA", "SmolLM", "Phi", "Qwen", "DeepSeek", "Mistral", "Custom"
    val parameterCount: String, // "135M", "360M", "0.5B", "1.1B", "1.5B", "3B", "7B"
    val quantization: String, // "Q4_K_M", "Q4_0", "INT4", "GGUF", "Embedded"
    val downloadSizeMb: Int,
    val memoryFootprintMb: Int,
    val contextLength: Int,
    val description: String,
    val tags: List<String>,
    val isEmbedded: Boolean = false,
    val downloadUrl: String = "",
    val filename: String = "$id.bin",
    val source: ModelSource = ModelSource.CURATED,
    val repoOrTag: String = "" // e.g. "ollama:deepseek-r1:1.5b" or "hf:bartowski/Llama-3.2-1B-Instruct-GGUF"
)

sealed class ModelStatus {
    object NotDownloaded : ModelStatus()
    data class Downloading(
        val progress: Float, // 0.0 to 1.0
        val downloadedMb: Float,
        val totalMb: Float,
        val speedMbps: Float
    ) : ModelStatus()
    data class Downloaded(
        val file: File,
        val fileSizeBytes: Long
    ) : ModelStatus()
    data class Active(
        val isLoaded: Boolean,
        val allocatedRamMb: Int
    ) : ModelStatus()
}

data class ModelItem(
    val info: ModelInfo,
    val status: ModelStatus,
    val isActive: Boolean = false
)
