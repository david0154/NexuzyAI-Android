# NexuzyAI Setup Guide

## 1. Qwen 3B Model (On-Device LLM)

### Option A — MLC-LLM (Recommended)
```bash
# Install MLC-LLM
pip install mlc-llm

# Download pre-compiled Qwen2-1.5B for Android (Qwen "3B" class)
# From HuggingFace:
https://huggingface.co/mlc-ai/Qwen2-1.5B-Instruct-q4f16_1-MLC

# Place model files in:
app/src/main/assets/qwen3b/

# Place mlc4j-release.aar in:
app/libs/

# Uncomment in app/build.gradle:
implementation files('libs/mlc4j-release.aar')

# Uncomment in QwenEngine.kt lines:
// engine = ai.mlc.mlcllm.MLCEngine()
// engine?.load(...)
```

### Option B — llama.cpp (GGUF)
```bash
# Download:
https://huggingface.co/Qwen/Qwen2-1.5B-Instruct-GGUF
# File: Qwen2-1.5B-Instruct-Q4_K_M.gguf

# Android JNI binding:
https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
```

---

## 2. Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create project → Enable **Maps SDK for Android** + **Geocoding API**
3. Create API Key → restrict to package `ai.nexuzy.assistant`
4. Add to `local.properties`:
   ```
   MAPS_API_KEY=AIzaSy...
   ```

> Used for: reverse geocoding GPS → city name in AI responses

---

## 3. Google News RSS (No Key Needed ✅)

Already wired in `NewsTool.kt`. Works out of the box:
```
https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en
```
For topic-specific news, the app calls:
```
https://news.google.com/rss/search?q=TOPIC&hl=en-IN
```

---

## 4. NewsAPI (Optional)

1. Register free at [newsapi.org](https://newsapi.org/register)
2. Add to `local.properties`:
   ```
   NEWS_API_KEY=your_key
   ```
> Without this key, app automatically falls back to Google News RSS.

---

## 5. AdMob Ads

1. Go to [admob.google.com](https://admob.google.com/)
2. Add App → Android → Get **App ID** and create **Banner Ad Unit**
3. Add to `local.properties`:
   ```
   ADMOB_APP_ID=ca-app-pub-XXXX~XXXX
   ADMOB_BANNER_ID=ca-app-pub-XXXX/XXXX
   ```
> Test ads work immediately with the default test IDs in `build.gradle`.

---

## 6. Voice (Works Out of Box ✅)

- **STT**: Uses Android's built-in `SpeechRecognizer` (no setup needed)
- **TTS**: Uses Android's built-in `TextToSpeech` in `en-IN` locale
- Partial results appear in real-time in the input field as you speak
- Voice orb animates based on microphone volume level

> For offline voice: replace `VoiceInputManager.kt` with [Vosk Android](https://alphacephei.com/vosk/android)

---

## 7. Device Control & Accessibility

Enable manually on device:
- **Accessibility**: Settings → Accessibility → NexuzyAI → Enable
- **Notification Access**: Settings → Notifications → Notification Access → NexuzyAI
