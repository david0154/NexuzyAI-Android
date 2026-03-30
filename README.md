<!-- NexuzyAI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="NexuzyAI Logo" />
</p>

<h1 align="center">NexuzyAI</h1>
<p align="center">
  <b>On-Device AI Assistant for Android</b><br/>
  Powered by Qwen3 · Built with MLC-LLM · Runs 100% offline
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/Model-Qwen3%201.7B-orange.svg" />
</p>

---

## ✨ Features

| Feature | Details |
|---|---|
| 🧠 On-Device AI | Qwen3-1.7B runs 100% on your phone via MLC-LLM |
| 🎙️ Voice Assistant | STT (SpeechRecognizer) + TTS (TextToSpeech), `en-IN` locale |
| 🌦️ Weather | Real-time via Open-Meteo, no API key needed |
| 📰 News | Google News RSS, no API key needed |
| 📍 Location-aware | GPS → city name via Android Geocoder |
| 📱 Device Control | Alarms, flashlight, media, open apps |
| 📮 AdMob Ads | Google AdMob banner (test IDs included) |
| 🌙 Day / Night Theme | Adaptive — follows system setting |
| 🔒 Zero Data Collection | No servers, no tracking, fully private |

---

## 📱 Screenshots

> UI adapts to system light/dark mode. Chat bubbles, voice orb, and typing indicator included.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Android Views + XML (ConstraintLayout, CardView, RecyclerView) |
| AI Runtime | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j` module) |
| AI Model | [Qwen3-1.7B-q4f16_1-MLC](https://huggingface.co/mlc-ai/Qwen3-1.7B-q4f16_1-MLC) |
| Voice STT | Android `SpeechRecognizer` |
| Voice TTS | Android `TextToSpeech` |
| Weather | [Open-Meteo API](https://open-meteo.com) (free, no key) |
| News | Google News RSS |
| Location | `FusedLocationProviderClient` + `Geocoder` |
| Maps | Google Maps SDK + Geocoding API |
| Ads | Google AdMob |
| Networking | OkHttp 4 + Retrofit 2 |
| Dependency Injection | Manual (no Hilt/Dagger) |
| Build System | Gradle (Kotlin DSL compatible) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

---

## 🚀 Quick Start

```bash
# 1. Clone
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android

# 2. Copy API keys template
cp local.properties.example local.properties
# Edit local.properties with your keys (Maps, AdMob, optional NewsAPI)

# 3. Open in Android Studio Hedgehog or later
# 4. Build & Run on Android 8.0+ device
```

> **The app works immediately** without any model setup.
> Weather, news, voice, device control, and ads all function out of the box.

---

## 🧠 Enable Qwen3 On-Device AI

Qwen3 requires running the MLC-LLM build pipeline once:

```bash
# Requires: Python 3.10+, Linux/Mac or WSL2 on Windows
pip install mlc-llm

# From project root (mlc-package-config.json already present):
python3 -m mlc_llm package
# Generates: dist/lib/mlc4j  +  dist/bundle/ (model weights)
```

Then in `settings.gradle` uncomment:
```gradle
include ':mlc4j'
project(':mlc4j').projectDir = file('dist/lib/mlc4j')
```

In `app/build.gradle` uncomment:
```gradle
implementation project(':mlc4j')
```

In `MLCEngineWrapper.kt` set `MLC_AVAILABLE = true` and uncomment the 3 imports.

See full instructions in **[SETUP_GUIDE.md](SETUP_GUIDE.md)**.

---

## 📂 Project Structure

```
NexuzyAI-Android/
├── app/src/main/java/ai/nexuzy/assistant/
│   ├── ChatActivity.kt          # Main chat screen
│   ├── AboutActivity.kt         # About + privacy + GitHub
│   ├── PrivacyPolicyActivity.kt # Full privacy policy screen
│   ├── SplashActivity.kt        # Animated splash screen
│   ├── NexuzyApp.kt             # Application class (AdMob init)
│   ├── llm/
│   │   ├── MLCEngineWrapper.kt  # MLC-LLM engine wrapper
│   │   ├── QwenEngine.kt        # Qwen3 high-level interface
│   │   └── PromptBuilder.kt     # Qwen3 chat-template builder
│   ├── middleware/
│   │   ├── IntentClassifier.kt  # Classify user intent
│   │   └── ToolExecutor.kt      # Execute tools (weather/news/etc)
│   ├── tools/
│   │   ├── LocationTool.kt      # GPS location
│   │   ├── WeatherTool.kt       # Open-Meteo weather
│   │   └── NewsTool.kt          # Google News RSS
│   └── adapter/
│       └── ChatAdapter.kt       # RecyclerView chat adapter
├── mlc-package-config.json      # MLC model list (Qwen3-1.7B default)
├── SETUP_GUIDE.md               # Full setup instructions
├── PRIVACY_POLICY.md            # Privacy policy
├── CONTRIBUTING.md              # Contribution guide
└── LICENSE                      # MIT License
```

---

## 🔑 API Keys

Copy `local.properties.example` → `local.properties` and fill in:

| Key | Required | Where to get |
|---|---|---|
| `MAPS_API_KEY` | Optional | [Google Cloud Console](https://console.cloud.google.com) |
| `NEWS_API_KEY` | Optional | [newsapi.org](https://newsapi.org/register) |
| `ADMOB_APP_ID` | Optional | [admob.google.com](https://admob.google.com) |
| `ADMOB_BANNER_ID` | Optional | AdMob dashboard |

> Test AdMob IDs are pre-configured. Google News RSS needs no key.

---

## 🤝 Contributing

Contributions are welcome! See **[CONTRIBUTING.md](CONTRIBUTING.md)** for guidelines.

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit: `git commit -m "feat: add something"`
4. Push and open a Pull Request

---

## 🔒 Privacy

NexuzyAI collects **zero data**. All AI runs on-device. No servers. No tracking.

Read the full policy: **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

---

## 📄 License

```
MIT License © 2025–2026 David (Nexuzy Lab)
```

See **[LICENSE](LICENSE)** for full text.

---

## 📧 Contact & Support

| | |
|---|---|
| 📧 Nexuzy Lab | nexuzylab@gmail.com |
| 📧 Developer | davidk76011@gmail.com |
| 🐙 GitHub | [david0154/NexuzyAI-Android](https://github.com/david0154/NexuzyAI-Android) |

---

<p align="center">Made with ❤️ by <b>David</b> · Managed by <b>Nexuzy Lab</b></p>
