package com.example.models

import java.io.File

data class ModelInfo(
    val id: String,
    val name: String,
    val family: String, // "Gemma", "LLaMA", "SmolLM", "Phi", "Qwen", "Custom"
    val parameterCount: String, // "360M", "1.1B", "2.0B", "3.8B"
    val quantization: String, // "Q4_K_M", "Q4_0", "INT4", "Bundled"
    val downloadSizeMb: Int,
    val memoryFootprintMb: Int,
    val contextLength: Int,
    val description: String,
    val tags: List<String>,
    val isEmbedded: Boolean = false,
    val downloadUrl: String = "",
    val filename: String = "$id.bin"
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
