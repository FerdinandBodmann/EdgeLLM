# Copilot instructions for EdgeLLM

## Repository overview

This repository is a single-module Android app in `app/` built with Kotlin and Jetpack Compose. The app is organized around a chat UI, model management, and local/cloud inference flows rather than a traditional layered backend.

Key runtime entry points:
- `app/src/main/java/com/example/MainActivity.kt`: Compose root, tabbed navigation, and the floating bottom bar.
- `app/src/main/java/com/example/ui/MainViewModel.kt`: central state holder for chat history, selected persona, model selection, message streaming, and benchmark/telemetry updates.
- `app/src/main/java/com/example/models/ModelManager.kt`: curated model catalog, download logic, local model inventory, active model selection, and Ollama endpoint configuration.
- `app/src/main/java/com/example/inference/OnDeviceInferenceEngine.kt`: local generation loop, token streaming, telemetry, and synthesized responses.
- `app/src/main/java/com/example/services/GeminiService.kt` and `app/src/main/java/com/example/services/OllamaService.kt`: external provider integrations.
- `app/src/main/java/com/example/data/AppDatabase.kt` + `ChatDao.kt`: Room persistence for chats and messages.

The app intentionally blends local/offline experiences with optional cloud providers (Gemini, Ollama) and a simulated on-device inference path. Keep feature changes aligned with this mixed-provider model instead of introducing a completely separate architecture.

## Build, test, and validation commands

This project is Gradle-based and uses the Android plugin. CI is configured to use JDK 21.

Before local builds, create the app secret file and ensure the Android SDK is configured:

```bash
cp .env.example .env
```

If Android Studio is not installed or the SDK is not on the default path, set `ANDROID_HOME` or create a `local.properties` file with a valid `sdk.dir` entry before running Gradle.

Common commands:

```bash
# Compile the debug APK
./gradlew :app:assembleDebug

# Run the full unit test suite
./gradlew :app:testDebugUnitTest

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"

# Run one test method
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest.someSpecificMethod"

# Run instrumented tests on a connected device or emulator
./gradlew :app:connectedDebugAndroidTest
```

`./gradlew test` is also valid as a shorthand in this project because the root project includes the app module. There is no dedicated `lint` Gradle task configured in this repo; use the compile/test tasks above as the primary validation path.

## Key conventions specific to this codebase

- The app uses Jetpack Compose and `StateFlow`/`viewModelScope` to drive UI state. Prefer updating screen state through `MainViewModel` rather than introducing ad hoc state in composables.
- Chat history is persisted in Room (`ChatEntity`, `MessageEntity`, `ChatDao`). If a feature depends on chat state, treat the DAO and `Flow` emissions as the source of truth.
- Model source is explicitly modeled as embedded, local GGUF, Ollama, or external Gemini. Keep provider-specific behavior in `ModelManager`, `ModelInfo`, and the service classes instead of scattering it across UI code.
- The project has a deliberate hybrid architecture: some inference is simulated locally in `OnDeviceInferenceEngine`, while real calls can go through Gemini or Ollama. When changing generation logic, preserve the existing provider-selection flow in `MainViewModel.sendMessage()`.
- The app relies on Android-specific configuration values and local files (`.env`, `debug.keystore`, `google-services.json`) during builds. If a build fails on missing secrets or Firebase config, check the local Android app setup before changing code paths.
- Dependency generation is handled by Gradle version catalog (`gradle/libs.versions.toml`). Keep version changes centralized there instead of adding one-off dependency declarations in modules.
- The package structure stays under `com.example.*`. Keep new files in the existing namespace and align with the surrounding feature boundaries.

## Working assumptions

- The project is an Android app first; changes should be compatible with Compose, Room, and Android lifecycle patterns.
- Model and inference features are user-facing and often tied to screen state. If you modify generation behavior, verify the relevant UI/test flow still matches the app’s state model.
- This repo does not appear to include a separate README or contributor guide; the build and architecture conventions above are the effective source of truth for future Copilot sessions.
