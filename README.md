# Synapse (EdgeLLM)

An offline-first, on-device AI companion for Android. Synapse pairs a quiet,
Claude-inspired UI with a pluggable inference layer: local models, a bundled
on-device fallback engine, Google Gemini, and Ollama — plus local tooling,
retrieval over your own documents, voice I/O and multimodal input.

> **Reality check:** this repository mixes *fully working* features with a few
> deliberately **scaffolded** ones. See [Status: real vs. scaffolded](#status-real-vs-scaffolded)
> before making claims about "running LLMs on-device".

## Highlights

- **Chat with history** — persisted conversations (Room), pin/rename/delete,
  regenerate / edit / share / copy message actions, and Markdown export.
- **Voice input & read-aloud** — offline-preferred `SpeechRecognizer` dictation
  with live partial results, and text-to-speech playback from any bubble.
- **Local RAG** — import `.txt` / `.md` / `.json` / `.csv` / `.pdf`, index them
  in an in-memory BM25 index, and ground answers in your own documents.
- **Tool / function layer** — a `ChatTool` registry (weather, math, clock/date,
  unit conversion, device info, document search) that answers deterministically
  on-device before any model is asked.
- **Multimodal input** — attach a photo or capture a screenshot; Gemini reads
  the raw image, everything else falls back to on-device OCR text.
- **Robust downloads** — resumable HTTP Range downloads, SHA-256 verification,
  storage/RAM pre-flight checks, and speed/ETA telemetry.
- **Benchmarks** — run latency/throughput benchmarks, browse recent runs and
  export the full history as CSV.

## Status: real vs. scaffolded

| Area | Status | Notes |
| --- | --- | --- |
| Chat history, message actions, export | ✅ Real | Room v2 with destructive-migration fallback. |
| Voice input & TTS | ✅ Real | Uses platform `SpeechRecognizer` + `TextToSpeech`. |
| Document import & BM25 RAG | ✅ Real | Fully on-device; PDF via `pdfbox-android`. |
| Tool/function-calling registry | ✅ Real | Deterministic tools run locally. |
| Gemini streaming (text + images) | ✅ Real | Network calls to the Gemini REST API. |
| Ollama streaming | ✅ Real | Network calls to your Ollama endpoint. |
| Multiple-image / camera capture | ✅ Real | Photo picker + system camera via FileProvider. |
| Offline OCR fallback | ✅ Real | Bundled ML Kit Latin recognizer. |
| Resumable downloads + SHA-256 | ✅ Real | OkHttp Range + sidecar digest. |
| Benchmark history & CSV export | ✅ Real | Persisted in Room. |
| **On-device LLM inference** | ⚠️ Simulated by default | The pure-Kotlin engine *synthesizes* responses (tool answers, curated templates, heuristic composition). It does **not** decode GGUF weights unless the native backend below is enabled and completed. |
| **llama.cpp native backend** | 🧱 Scaffold (opt-in) | `LlamaBridge` + `synapse_native.cpp` compile only with `-PenableNative=true`. The native side is *integration-level-0*: it validates JNI and returns a `0` handle, which signals "fall back to the simulated engine". No weights are loaded yet. |

### Finishing the native backend

Real llama.cpp inference is intentionally left as a scaffold so the default
build (and CI, which has no NDK) stays green. To complete it:

1. Vendor llama.cpp (git submodule or CMake `FetchContent`) and link it in
   `app/src/main/cpp/CMakeLists.txt` (`target_link_libraries(synapse_native llama …)`).
2. In `synapse_native.cpp`, allocate a `llama_context` in `nativeInit` and
   return its pointer as the handle; tokenize/sample in `nativeGenerate`;
   free in `nativeFree`.
3. Build with the NDK:
   ```bash
   ./gradlew assembleDebug -PenableNative=true
   ```

The Kotlin call sites in `LlamaBridge.kt` do not need to change, and the app
keeps working with the simulated engine whenever the `.so` is absent.

## Building

```bash
# Standard build (pure Kotlin, no NDK required):
./gradlew assembleDebug

# Optional native llama.cpp build (requires the Android NDK):
./gradlew assembleDebug -PenableNative=true
```

CI (`.github/workflows/build-apk.yml`) runs the standard `assembleDebug` on
JDK 21 and produces a debug APK artifact.

### Providers

- **Gemini** — add `GEMINI_API_KEY` to `.env` (see `.env.example`), or enter a
  key at runtime in settings.
- **Ollama** — point the app at your Ollama endpoint and pull a tag.
- **Local files** — import a GGUF or download a catalog model.

## Privacy

Everything except explicitly configured cloud providers (Gemini / Ollama) runs
on-device: documents, the BM25 index, OCR, speech, TTS, and the download cache.
There is no telemetry upload; "benchmarks" are stored locally and only shared
when you tap export.
