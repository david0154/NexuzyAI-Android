# David AI — Setup Guide

---

## Why mlc4j Is Needed

David AI uses Qwen3 / Gemma models compiled as native C++ (TVM runtime).
Android can’t call C++ from Kotlin directly — it needs a JNI bridge.
**mlc4j IS that bridge.** It provides:

- `MLCEngine` Kotlin class → call `engine.reload()`, `engine.chat.completions.create()`
- `libmlc_llm.so` → compiled AI runtime (arm64-v8a)
- `libmlc4j.so` → JNI layer between Kotlin and C++

It can’t be downloaded from Maven because the `.so` files are compiled
specifically for your chosen model (Qwen3 vs Gemma) and quantization (q4f16).
**MLC-LLM compiles it fresh once from `mlc-package-config.json`.**

---

## Without mlc4j — What Still Works ✅

| Feature | Works without mlc4j? |
|---|---|
| 🌦️ Weather (Open-Meteo) | ✅ Yes |
| 📰 News (Google RSS) | ✅ Yes |
| 📍 Location (GPS) | ✅ Yes |
| ⏰ Alarms | ✅ Yes |
| 💡 Flashlight | ✅ Yes |
| 🎙️ Voice STT + TTS | ✅ Yes |
| 🚀 Open Apps | ✅ Yes |
| 👨‍💻 Developer info | ✅ Yes |
| 🧠 On-device AI chat | ❌ Needs mlc4j |

---

## Step 1 — Generate mlc4j

```bash
# Linux / Mac / WSL2 on Windows (native Windows NOT supported)
pip install mlc-llm

# From NexuzyAI-Android project root:
python3 -m mlc_llm package
# Reads: mlc-package-config.json
# Outputs:
#   dist/lib/mlc4j     ← Android JNI module (this is what gets added as :mlc4j)
#   dist/bundle/       ← compiled model weights
```

---

## Step 2 — Connect mlc4j to the Project

In `settings.gradle`, uncomment:
```gradle
include ':mlc4j'
project(':mlc4j').projectDir = file('dist/lib/mlc4j')
```

In `app/build.gradle`, uncomment:
```gradle
implementation project(':mlc4j')
```

---

## Step 3 — Enable in Code

In `app/src/main/java/ai/david/ai/llm/MLCEngineWrapper.kt`:

1. Set `MLC_AVAILABLE = true`
2. Uncomment the 3 imports:
   ```kotlin
   import ai.mlc.mlcllm.MLCEngine
   import ai.mlc.mlcllm.OpenAIProtocol
   import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
   ```
3. Uncomment `private val engine = MLCEngine()`
4. Uncomment all `engine.*` lines in `loadModel()` and `generate()`

---

## Step 4 — Build and Run

Open Android Studio → **Build → Make Project** → **Run** on device.

The model badge will show:
- `David AI Lite` on low-RAM phones
- `David AI 1B` on most phones
- `David AI 2B` on flagship phones

---

## API Keys (Optional)

```
cp local.properties.example local.properties
```

| Key | Required | Source |
|---|---|---|
| `MAPS_API_KEY` | Optional | [console.cloud.google.com](https://console.cloud.google.com) |
| `NEWS_API_KEY` | Optional | [newsapi.org](https://newsapi.org/register) |
| `ADMOB_APP_ID` | Optional | [admob.google.com](https://admob.google.com) |

Test AdMob IDs are pre-configured — ads work without any key.
