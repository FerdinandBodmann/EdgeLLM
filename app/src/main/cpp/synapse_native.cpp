// Synapse → llama.cpp JNI bridge (integration-level-0 scaffold).
//
// This file exists so the project has a complete, compiling JNI surface that
// matches `com.example.inference.native.LlamaBridge`. It is intentionally a
// *scaffold*: no weights are loaded and no tokens are decoded here yet.
//
// Why a scaffold instead of the full backend?
//   * Real llama.cpp inference needs the llama.cpp sources, an NDK toolchain
//     and a per-ABI build — none of which are available in the default CI image.
//   * `nativeInit` therefore returns 0, which the Kotlin side reads as
//     "no native context, use the simulated engine". The app always works.
//
// To finish the integration:
//   1. Vendor llama.cpp (submodule or FetchContent) and link it in CMakeLists.txt.
//   2. Allocate a llama context in nativeInit and store it as the returned handle.
//   3. Tokenize, decode and sample in nativeGenerate, returning the full string
//      (or switch to a chunked callback if you want true token streaming).
//   4. Free the context in nativeFree.

#include <jni.h>
#include <android/log.h>

#include <string>

#define LOG_TAG "SynapseNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace {

// Small helper: jstring -> std::string (UTF-8), safe against nullptrs.
std::string ToUtf8(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result = (chars != nullptr) ? std::string(chars) : std::string();
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_inference_native_LlamaBridge_nativeInit(
        JNIEnv* env, jobject /*thiz*/, jstring modelPath, jint threads, jint contextLength) {
    const std::string path = ToUtf8(env, modelPath);
    LOGI("nativeInit (scaffold): path=%s threads=%d contextLength=%d",
         path.c_str(), static_cast<int>(threads), static_cast<int>(contextLength));

    // Integration level 0: we deliberately do NOT load the model yet.
    // Returning 0 tells LlamaBridge.init() to fall back to simulate mode.
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_inference_native_LlamaBridge_nativeGenerate(
        JNIEnv* env, jobject /*thiz*/, jlong handle, jstring prompt,
        jint maxTokens, jfloat temperature, jfloat topP) {
    const std::string text = ToUtf8(env, prompt);
    LOGW("nativeGenerate (scaffold) called with %zu prompt chars, handle=%lld, "
         "maxTokens=%d temp=%.2f topP=%.2f",
         text.size(), static_cast<long long>(handle),
         static_cast<int>(maxTokens),
         static_cast<double>(temperature), static_cast<double>(topP));

    const std::string message =
            "[synapse-native scaffold] The native llama.cpp backend is not "
            "compiled into this build; falling back to the on-device simulated engine.";
    return env->NewStringUTF(message.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_inference_native_LlamaBridge_nativeFree(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong /*handle*/) {
    // Nothing to release while the backend is a scaffold.
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_inference_native_LlamaBridge_nativeVersion(
        JNIEnv* env, jobject /*thiz*/) {
    return env->NewStringUTF("synapse-native/0.0.1-scaffold");
}
