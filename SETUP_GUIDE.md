# NexuzyAI — Complete Setup Guide (v6)

---

## ⚠️ THE REAL WAY mlc4j WORKS

After fully reading `mlc-ai/mlc-llm/android/MLCChat/settings.gradle`:

```gradle
// Official MLCChat settings.gradle:
include ':mlc4j'
project(':mlc4j').projectDir = file('dist/lib/mlc4j')
```

**mlc4j lives in `dist/lib/mlc4j`** — a folder that is **generated** by
running `python3 -m mlc_llm package`. It is not in the GitHub repo
(listed in `.gitignore`). It is NOT a downloadable AAR.

---

## Step 1 — Enable Qwen3 (MLC-LLM)

### The Correct Method

```bash
# 1. Install MLC-LLM Python package
#    (Use Linux/Mac or WSL on Windows — Windows native is NOT supported)
pip install mlc-llm

# Verify install:
python3 -c "import mlc_llm; print('MLC ready')"

# 2. mlc-package-config.json is already in your project root
#    It targets Qwen3-1.7B and Qwen3-0.6B for Android

# 3. Run the packager from NexuzyAI-Android project root:
python3 -m mlc_llm package

# This will:
#   a. Download/compile Qwen3-1.7B model library (.so) for arm64-v8a
#   b. Download model weights from HuggingFace (~1.5GB)
#   c. Generate dist/lib/mlc4j    ← Android module
#   d. Generate dist/bundle/      ← compiled weights
# Takes ~10-30 mins on first run

# 4. In settings.gradle, uncomment:
#      include ':mlc4j'
#      project(':mlc4j').projectDir = file('dist/lib/mlc4j')

# 5. In app/build.gradle, uncomment:
#      implementation project(':mlc4j')

# 6. In MLCEngineWrapper.kt:
#    - Set:       const val MLC_AVAILABLE = true
#    - Uncomment: import ai.mlc.mlcllm.MLCEngine
#    - Uncomment: import ai.mlc.mlcllm.OpenAIProtocol
#    - Uncomment: import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
#    - Uncomment: all engine.* lines inside loadModel() and generate()

# 7. Build and run in Android Studio
```

### Windows Users (WSL Required)

```powershell
# MLC-LLM Python build ONLY works on Linux/Mac.
# On Windows, use WSL2:
wsl --install
# Then inside WSL:
pip install mlc-llm
python3 -m mlc_llm package
# The generated dist/ folder works on both WSL and Windows Android Studio
```

### Lightweight Alternative: llama.cpp GGUF

If MLC setup is too heavy, use `llama.cpp` with GGUF model files.
No Python compilation needed:

```bash
# Download GGUF model directly:
# https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF
# File: Qwen2-1.5B-Instruct-Q4_K_M.gguf (~1GB)

# Build llama.cpp Android JNI:
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
mkdir build-android && cd build-android
cmake .. -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a \
         -DANDROID_PLATFORM=android-26 \
         -DLLAMA_ANDROID_JNI=ON
make -j$(nproc)
# Copy libllama.so to app/src/main/jniLibs/arm64-v8a/
```

---

## Step 2 — Google Maps API Key

1. [console.cloud.google.com](https://console.cloud.google.com)
2. Enable: **Maps SDK for Android** + **Geocoding API**
3. Create API key → restrict to package `ai.nexuzy.assistant`
4. `local.properties`:
   ```
   MAPS_API_KEY=AIzaSy_YOUR_KEY
   ```

---

## Step 3 — News (Google RSS, No Key Needed ✅)

Google News RSS works with zero setup:
```
https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en
```
Optional NewsAPI (100 req/day free): [newsapi.org/register](https://newsapi.org/register)
```
NEWS_API_KEY=your_key  (in local.properties)
```

---

## Step 4 — AdMob

1. [admob.google.com](https://admob.google.com) → Add app → Banner ad unit
2. `local.properties`:
   ```
   ADMOB_APP_ID=ca-app-pub-XXXX~XXXX
   ADMOB_BANNER_ID=ca-app-pub-XXXX/XXXX
   ```
> Test IDs already set — ads show immediately without real keys.

---

## Step 5 — Voice (Zero Setup ✅)

- **STT**: Android native `SpeechRecognizer` (`en-IN`)
- **TTS**: Android native `TextToSpeech` (`en-IN`)
- Partial speech shown live in input box
- Voice orb pulses with mic volume via `onRmsChanged()`

---

## App Works Right Now ✅

Without Qwen3, these work fully:
| Feature | How |
|---|---|
| 🌦️ Weather | Open-Meteo API, no key |
| 📰 News | Google RSS, no key |
| 📍 Location | GPS → city via Geocoder |
| 📱 Device control | Alarms, flashlight, media, apps |
| 🎙️ Voice | SpeechRecognizer + TTS |
| 📮 AdMob | Test mode, no key |
