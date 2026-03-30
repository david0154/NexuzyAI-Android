# NexuzyAI — Complete Setup Guide

---

## ⚠️ IMPORTANT: mlc4j is NOT a downloadable AAR

The previous instructions were wrong. `mlc4j-release.aar` does **not exist** as a
downloadable file. MLC-LLM provides `mlc4j` as a **local Android library module** that
must be copied and built. There is only one release on GitHub (v0.1.dev0 from 2023)
with no assets.

The official MLC-LLM Android app uses:
```gradle
implementation project(":mlc4j")   // local module
```
not an AAR file.

---

## Step 1: Set up mlc4j (Required for Qwen 3B)

### Option A — Using prepare_libs.py (Easiest — downloads prebuilt .so)

```bash
# From your development machine (Windows PowerShell or Linux/Mac terminal)

# 1. Clone MLC-LLM repo
git clone --recursive https://github.com/mlc-ai/mlc-llm.git

# 2. Copy mlc4j into NexuzyAI-Android project root
cp -r mlc-llm/android/mlc4j  NexuzyAI-Android/mlc4j

# 3. Download prebuilt native libs (arm64-v8a .so files)
cd NexuzyAI-Android/mlc4j
python3 prepare_libs.py
# This downloads libtvm_runtime.so, libmlc_llm.so, libmlc_llm_jni.so

# 4. In settings.gradle, uncomment:
#    include ':mlc4j'

# 5. In app/build.gradle, uncomment:
#    implementation project(':mlc4j')

# 6. In MLCEngineWrapper.kt:
#    Set:        const val MLC_AVAILABLE = true
#    Uncomment:  import ai.mlc.mlcllm.MLCEngine
#    Uncomment:  import ai.mlc.mlcllm.OpenAIProtocol
#    Uncomment:  import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
#    Uncomment all engine.* lines in the generate() and loadModel() functions
```

### Option B — Build from Source (Full control)

```bash
# Requirements: NDK r26b+, CMake 3.24+, Python 3.10+, TVM nightly
pip install mlc-llm  # installs MLC Python package
cd mlc-llm
python3 cmake/gen_cmake_config.py  # generates build config
# Then build via Android Studio or command line with NDK
```

### Option C — Use llama.cpp instead (Simpler, GGUF support)

If MLC-LLM setup is too complex, use llama.cpp Android JNI:

```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
# Build for Android (arm64):
mkdir build-android && cd build-android
cmake .. -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 \
         -DLLAMA_ANDROID_JNI=ON
make -j4
# Copy libllama.so to NexuzyAI-Android/app/src/main/jniLibs/arm64-v8a/
```

Model (GGUF format — no compile needed):
```
https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF
File: Qwen2-1.5B-Instruct-Q4_K_M.gguf  (~1GB)
```

---

## Step 2: Download Qwen 3B Model Weights

### Auto-download (app does this on first launch if MLC enabled)
The app auto-downloads from HuggingFace:
```
https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC
```

### Manual push via ADB (faster)
```bash
git lfs install
git clone https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC

adb push Qwen2-1.5B-Instruct-q4f16_1-MLC \
    /sdcard/Android/data/ai.nexuzy.assistant/files/Qwen2-1.5B-Instruct-q4f16_1-MLC
```

---

## Step 3: Google Maps API Key

1. Go to [https://console.cloud.google.com](https://console.cloud.google.com)
2. Create project → Enable **Maps SDK for Android** + **Geocoding API**
3. Create API Key → restrict to package: `ai.nexuzy.assistant`
4. Add to `local.properties`:
   ```
   MAPS_API_KEY=AIzaSy_YOUR_KEY_HERE
   ```

---

## Step 4: News (Google RSS — Already Works, No Key)

Google News RSS is **already working with zero setup**:
```
https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en
```
Optional NewsAPI key (100 req/day free):
1. Register at [https://newsapi.org/register](https://newsapi.org/register)
2. Add to `local.properties`:  `NEWS_API_KEY=your_key`

---

## Step 5: AdMob Ads

1. Go to [https://admob.google.com](https://admob.google.com)
2. Add App → Android → Get **App ID** + create **Banner Ad Unit**
3. Add to `local.properties`:
   ```
   ADMOB_APP_ID=ca-app-pub-XXXX~XXXX
   ADMOB_BANNER_ID=ca-app-pub-XXXX/XXXX
   ```
> Test ads work immediately with default test IDs — no setup needed to test.

---

## Step 6: Voice (Works Out of Box ✅)

- **STT**: Android native SpeechRecognizer (en-IN) — no setup
- **TTS**: Android native TextToSpeech (en-IN) — no setup
- Partial results appear live in the input box
- Voice orb pulses with mic volume

---

## App Works Without MLC Right Now

Even without Qwen 3B loaded, the app is **fully functional**:
- 🌦️ Weather (Open-Meteo, no key)
- 📰 News (Google RSS, no key)
- 📍 Location (GPS → city name)
- 📱 Device control (alarms, flashlight, media, open apps)
- 🎙️ Voice input/output
- 📮 AdMob ads (test mode)

The fallback response shows the tool result directly until Qwen 3B is loaded.
