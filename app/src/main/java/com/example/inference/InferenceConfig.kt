package com.example.inference

enum class ComputeBackend(val displayName: String, val tag: String) {
    CPU_NEON("CPU (ARM NEON)", "cpu"),
    GPU_OPENCL("GPU (OpenCL / Vulkan)", "gpu"),
    NNAPI_NPU("NPU / NNAPI Accelerator", "npu")
}

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxTokens: Int = 512,
    val repetitionPenalty: Float = 1.1f,
    val threadCount: Int = 4,
    val computeBackend: ComputeBackend = ComputeBackend.CPU_NEON,
    val systemPrompt: String = "You are Synapse, a thoughtful on-device AI running natively and privately on this Android device without internet connectivity."
)
