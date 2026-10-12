package com.example.inference.native

/**
 * Kotlin side of the optional llama.cpp native bridge.
 *
 * The native library (`libsynapse_native.so`) is **opt-in**: it is only built
 * when the project is configured with `-PenableNative=true` and an NDK is
 * installed. On a standard build (and therefore on CI) the library is absent,
 * [isNativeAvailable] stays `false`, and every caller falls back to the pure
 * Kotlin simulated engine — the app never crashes for a missing `.so`.
 *
 * The bundled `synapse_native.cpp` is currently an *integration-level-0
 * scaffold*: it validates the JNI wiring and returns `0` from [nativeInit],
 * which signals "no weights loaded, use the fallback". Swapping in real
 * llama.cpp calls (llama_load_model_from_file / llama_eval / llama_sample) is
 * the intended next step and requires no changes on the Kotlin call sites.
 */
object LlamaBridge {

    const val LIBRARY_NAME = "synapse_native"

    /** True when the native library could be loaded on this device/build. */
    val isNativeAvailable: Boolean by lazy {
        try {
            System.loadLibrary(LIBRARY_NAME)
            true
        } catch (t: Throwable) {
            false
        }
    }

    // --- Native entry points ---------------------------------------------------

    external fun nativeInit(modelPath: String, threads: Int, contextLength: Int): Long

    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float
    ): String

    external fun nativeFree(handle: Long)

    external fun nativeVersion(): String

    // --- Safe Kotlin wrappers --------------------------------------------------

    /** Returns a native handle, or null when the native backend is unavailable. */
    fun init(modelPath: String, threads: Int, contextLength: Int): Long? {
        if (!isNativeAvailable) return null
        return try {
            nativeInit(modelPath, threads, contextLength).takeIf { it != 0L }
        } catch (t: Throwable) {
            null
        }
    }

    /** Returns generated text, or null when the native backend is unavailable. */
    fun generate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float
    ): String? {
        if (!isNativeAvailable || handle == 0L) return null
        return try {
            nativeGenerate(handle, prompt, maxTokens, temperature, topP)
        } catch (t: Throwable) {
            null
        }
    }

    fun free(handle: Long) {
        if (!isNativeAvailable || handle == 0L) return
        try {
            nativeFree(handle)
        } catch (t: Throwable) {
            // Releasing a native context must never surface as a crash.
        }
    }

    fun version(): String = try {
        if (isNativeAvailable) nativeVersion() else "unavailable"
    } catch (t: Throwable) {
        "unavailable"
    }
}
