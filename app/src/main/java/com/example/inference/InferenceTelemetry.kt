package com.example.inference

data class InferenceTelemetry(
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val ramAllocatedMb: Int = 0,
    val activeBackend: String = "CPU (ARM NEON)",
    val modelName: String = ""
)

data class GenerationChunk(
    val token: String,
    val accumulatedText: String,
    val isFirstToken: Boolean = false,
    val isFinished: Boolean = false,
    val telemetry: InferenceTelemetry
)

sealed class EngineState {
    object Uninitialized : EngineState()
    data class LoadingModel(val modelName: String, val progress: Float) : EngineState()
    data class Ready(val modelName: String, val ramMb: Int) : EngineState()
    data class Generating(val modelName: String, val currentTokens: Int) : EngineState()
    data class Error(val message: String) : EngineState()
}
