<!-- David AI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="David AI Logo" />
</p>

<h1 align="center">David AI</h1>
<p align="center">
  <b>On-Device AI Assistant for Android</b><br/>
  Device-adaptive model tiers · 100% private · Voice + Text · Built with MLC-LLM
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/Model-Device--Adaptive-orange.svg" />
</p>

---

## 🧠 Model Tiers

David AI automatically selects the best model based on your device RAM:

| Display Name | Backend Model | RAM Required | Size | Best For |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | Budget / low-RAM phones |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | Most mid-range phones |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | Flagship phones |

Tap the model badge in the top bar to manually choose a different tier. The app always shows the David AI brand name — the backend model is transparent to the user.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🧠 On-Device AI | Runs 100% on-device via MLC-LLM. Zero cloud dependency |
| 📱 Device-Adaptive | Auto-selects David AI Lite / 1B / 2B based on device RAM |
| 🎙️ Voice + Text | Both input modes work for weather, news, location, all commands |
| 🌦️ Weather | Real-time via Open-Meteo, no API key needed |
| 📰 News | Google News RSS, no API key needed |
| 📍 Location-aware | GPS → city name via Android Geocoder |
| 📱 Device Control | Alarms, flashlight, media, open apps |
| 👨‍💻 Developer Info | Ask "Who made you?" — AI knows its own identity |
| 📮 AdMob Ads | Google AdMob banner (test IDs included) |
| 🌙 Day / Night Theme | Follows system setting |
| 🔒 Zero Data Collection | No servers. No tracking. Fully private. |

---

## 🗣️ What You Can Ask (Voice or Text)

```
"What's the weather today?"         → Live weather via Open-Meteo
"Latest news"                        → Top headlines from Google News RSS
"Where am I?"                        → GPS city name
"Set alarm at 7 AM"                  → Opens Android alarm
"Turn on flashlight"                 → Torch control
"Play music" / "Next song"           → Media key dispatch
"Open YouTube"                       → Launches app
"Who made you?"                      → Developer info (David, Nexuzy Lab)
"What model are you?"                → Shows David AI tier + RAM info
"What can you do?"                   → Feature overview
Anything else                        → On-device LLM chat
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Android Views + XML (ConstraintLayout, CardView, RecyclerView) |
| AI Runtime | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j` module) |
| AI Models | Qwen3-0.6B (Lite) · Qwen3-1.7B (1B) · Gemma-2-2B (2B) |
| Model Selection | `ModelManager.kt` — `ActivityManager.MemoryInfo.totalMem` |
| Voice STT | Android `SpeechRecognizer` |
| Voice TTS | Android `TextToSpeech` |
| Weather | [Open-Meteo API](https://open-meteo.com) (free, no key) |
| News | Google News RSS |
| Location | `FusedLocationProviderClient` + `Geocoder` |
| Maps | Google Maps SDK + Geocoding API |
| Ads | Google AdMob |
| Networking | OkHttp 4 + Retrofit 2 |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

---

## 🚀 Quick Start

```bash
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android
cp local.properties.example local.properties
# Edit local.properties with your API keys
# Open in Android Studio → Build → Run
```

> **Works immediately** without MLC build. Weather, news, voice, device control all function out of the box.

---

## 🧠 Enable On-Device AI

```bash
pip install mlc-llm
python3 -m mlc_llm package   # reads mlc-package-config.json
# Then uncomment 3 lines in settings.gradle + app/build.gradle
# Set MLC_AVAILABLE = true in MLCEngineWrapper.kt
```
See **[SETUP_GUIDE.md](SETUP_GUIDE.md)** for full instructions.

---

## 📂 Project Structure

```
app/src/main/java/ai/nexuzy/assistant/
├── ChatActivity.kt              # Main chat screen (voice + text unified)
├── AboutActivity.kt             # About + privacy + GitHub
├── PrivacyPolicyActivity.kt     # Full privacy policy
├── SplashActivity.kt            # Splash screen with logo
├── NexuzyApp.kt                 # Application class
├── llm/
│   ├── ModelManager.kt          # RAM detection + model tier selection
│   ├── QwenEngine.kt            # High-level David AI engine interface
│   ├── MLCEngineWrapper.kt      # MLC-LLM engine + streaming
│   └── PromptBuilder.kt         # Qwen3 chat-template prompt builder
├── middleware/
│   ├── IntentClassifier.kt      # Keyword intent detection (offline)
│   └── ToolExecutor.kt          # Execute tools, return ToolResult
├── tools/
│   ├── WeatherTool.kt           # Open-Meteo weather
│   ├── NewsTool.kt              # Google News RSS
│   └── LocationTool.kt          # GPS + Geocoder
└── ui/
    ├── ModelSelectorFragment.kt # Bottom sheet model picker
    └── adapter/ChatAdapter.kt   # RecyclerView chat bubbles
```

---

## 🤝 Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)**

---

## 🔒 Privacy

David AI collects **zero data**. See **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

---

## 📄 License

MIT License © 2025–2026 David (Nexuzy Lab) — see **[LICENSE](LICENSE)**

---

## 📧 Contact

| | |
|---|---|
| 📧 Nexuzy Lab | nexuzylab@gmail.com |
| 📧 Developer | davidk76011@gmail.com |
| 🐙 GitHub | [david0154/NexuzyAI-Android](https://github.com/david0154/NexuzyAI-Android) |

<p align="center">Made with ❤️ by <b>David</b> · Managed by <b>Nexuzy Lab</b></p>
