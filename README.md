# NexuzyAI Android Assistant

> AI-powered Android assistant built on [MLC-LLM](https://github.com/mlc-ai/mlc-llm/tree/main/android) with on-device LLM inference + tool integrations.

## ✨ Features

| Feature | Technology |
|---|---|
| 🤖 On-device LLM | MLC-LLM (mlc4j) |
| 🌦️ Weather | Open-Meteo API |
| 📰 News | NewsAPI + Google RSS |
| 📍 Location | FusedLocationProviderClient |
| 🎙️ Voice STT | Vosk / Whisper.cpp |
| 🔊 Voice TTS | Android TTS / Coqui |
| 📱 Device Control | Android Intents + Accessibility Service |
| 🧠 Intent Routing | Middleware intent classifier |

## 🏗️ Architecture

```
User Input (text/voice)
       ↓
  IntentClassifier
  (AI or rule-based)
       ↓
 ┌─────┴─────┐
 Weather  News  Location  Device
  Tool    Tool    Tool    Control
       ↓
  AI formats human-like reply
       ↓
  Response (text + TTS)
```

## 🔧 Setup

1. Clone this repo
2. Open `app/` in Android Studio
3. Add your `NewsAPI` key in `local.properties`:
   ```
   NEWS_API_KEY=your_key_here
   ```
4. Download MLC-LLM model weights and place in `assets/`
5. Build & Run on Android 8.0+ device

## 📋 Permissions Required

- `ACCESS_FINE_LOCATION` — for location-aware responses
- `RECORD_AUDIO` — for voice input
- `INTERNET` — for weather/news APIs
- `BIND_ACCESSIBILITY_SERVICE` — for device control
- `FOREGROUND_SERVICE` — for background AI service

## 📦 Module Structure

```
app/
  src/main/
    java/ai/nexuzy/assistant/
      MainActivity.kt
      ChatActivity.kt
      middleware/
        IntentClassifier.kt       ← Routes input to correct tool
        ToolExecutor.kt           ← Executes weather/news/device
      tools/
        WeatherTool.kt            ← Open-Meteo integration
        NewsTool.kt               ← NewsAPI + Google RSS
        LocationTool.kt           ← FusedLocationProviderClient
        DeviceControlTool.kt      ← Intents + Accessibility
      voice/
        SpeechRecognizer.kt       ← Vosk/Whisper STT
        TextToSpeech.kt           ← Android TTS
      llm/
        MLCEngine.kt              ← MLC-LLM wrapper
        PromptBuilder.kt          ← System prompt injector
      services/
        AssistantService.kt       ← Foreground service
        NotificationListener.kt   ← Read notifications
```

## 🙏 Credits

- [MLC-LLM](https://github.com/mlc-ai/mlc-llm) — on-device LLM inference
- [Open-Meteo](https://open-meteo.com/) — free weather API
- [NewsAPI](https://newsapi.org/) — news feed
- [Vosk](https://alphacephei.com/vosk/) — offline STT
