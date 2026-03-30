<!-- NexuzyAI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="NexuzyAI Logo" />
</p>

<h1 align="center">NexuzyAI</h1>
<p align="center">
  <b>On-Device + Cloud-Hybrid AI Assistant for Android</b><br/>
  Offline-first · MLC-LLM + Sarvaam AI + DuckDuckGo<br/>
  Voice + Text · Android 8–16 · Zero data collection
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026--36-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/Offline-First%20After%20Setup-orange.svg" />
  <a href="SETUP_GUIDE.md"><img src="https://img.shields.io/badge/Setup-Full%20Guide-orange.svg" /></a>
</p>

---

## 📦 How First Install Works

### Why the AI model cannot be inside the APK
> The MLC model weights are **700 MB – 2.1 GB**.
> Google Play APK limit is **100 MB**.
> It is **physically impossible** to bundle the model in the APK.
> Every on-device AI app (Gemini, ChatGPT mobile, etc.) does the same —
> **download the model once, then run forever offline.**

### First Launch Flow
```
Install APK (small, < 100 MB)
        ↓
First Launch Screen
        ├─ Internet OFF
        │      → Starts with basic offline mode (LocalOfflineEngine)
        │      → Reconnect Wi-Fi → reopen app → auto-downloads model
        └─ Internet ON
               → Auto-downloads MLC model in background (~700MB–2.1GB)
               → Can skip anytime → uses Sarvaam AI + DuckDuckGo instead
               → After download: FULL AI works offline forever ✔️
```

---

## ✅ What "Fully Offline" Means

| | No Internet + No MLC | No Internet + MLC Downloaded | Internet (any time) |
|---|---|---|---|
| **AI Chat** | ⚠️ Basic NLP only | ✅ **Full real AI** | ✅ Sarvaam + DDG |
| **Weather** | ❌ | ❌ | ✅ Live |
| **News** | ❌ | ❌ | ✅ Live |
| **Web Search** | ❌ | ❌ | ✅ DuckDuckGo |
| **Date / Time** | ✅ | ✅ | ✅ |
| **Math** | ✅ | ✅ | ✅ |
| **Device Control** | ✅ | ✅ | ✅ |
| **Location** | ✅ GPS | ✅ GPS | ✅ GPS + city |
| **Privacy** | ✅ Zero data | ✅ Zero data | ⚠️ API calls made |

> 🔑 **After the one-time MLC download, NexuzyAI works 100% offline with real AI.**
> No internet. No account. No tracking. Ever.

---

## 🧠 AI Engine Routing

```
┌────────────────────────────────────────────────────┐
│  INTERNET ON                                      │
│  DuckDuckGo ──┬─► HybridAnswerEngine ► answer   │
│  Sarvaam AI ──┤                                   │
│  Local MLC  ──┘ (fused when MLC downloaded)       │
├────────────────────────────────────────────────────┤
│  INTERNET OFF + MLC downloaded (ideal)            │
│  MLC on-device model ► full real AI offline       │
│  100% private · no server · no data sent          │
├────────────────────────────────────────────────────┤
│  INTERNET OFF + NO MLC (before download)          │
│  LocalOfflineEngine: greetings · date/time · math │
│  Shows guide to connect Wi-Fi and download model  │
└────────────────────────────────────────────────────┘
```

---

## 🧠 Model Tiers

Auto-selected based on device RAM. Downloaded once from First Launch screen.

| Display Name | Backend Model | RAM Required | Download Size | Offline AI? |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | ✅ After download |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | ✅ After download |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | ✅ After download |

---

## ✨ Features

| Feature | Needs Internet? | Notes |
|---|---|---|
| 🧠 MLC On-Device AI | ⚠️ Once to download | After download: 100% offline real AI forever |
| 💡 LocalOfflineEngine | ❌ Never | Basic NLP bridge until MLC is downloaded |
| 🌐 Sarvaam AI | ✅ Each time | Best cloud AI quality online |
| 🦆 DuckDuckGo | ✅ Each time | Web grounding for answers |
| 🔗 Link Reader | ✅ Each time | Paste URL — AI reads & summarizes |
| 🌦️ Weather | ✅ Each time | Real-time Open-Meteo |
| 📰 News | ✅ Each time | RSS + NewsAPI |
| 📍 Location | ⚠️ Partial | GPS offline; city name online |
| 📱 Device Control | ❌ Never | Alarms, flashlight, media, launch apps |
| 🎙️ Voice + Text | ❌ Never | Both input modes work offline |
| 🔒 Zero Data | ❌ Never | No tracking, no accounts |

---

## 🚀 Quick Start

```bash
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android
cp local.properties.example local.properties
# Fill in your API keys in local.properties
# Open in Android Studio → Sync → Run
```

> 📚 **Full setup instructions**: See **[SETUP_GUIDE.md](SETUP_GUIDE.md)**

---

## 🔑 API Keys

| Key | Required | Where to get |
|---|---|---|
| `MAPS_API_KEY` | ✅ Yes | [console.cloud.google.com](https://console.cloud.google.com) |
| `NEWS_API_KEY` | ❌ Optional | [newsapi.org](https://newsapi.org) |
| `SARVAAM_API_KEY` | ❌ Optional | [sarvam.ai](https://sarvam.ai) |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Android Views + XML |
| On-Device AI | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j`) — one-time download |
| AI Models | Qwen3-0.6B / Qwen3-1.7B / Gemma-2-2B |
| Cloud AI | [Sarvaam AI API](https://sarvam.ai) |
| Web Search | DuckDuckGo Instant Answer API |
| Offline NLP Bridge | `LocalOfflineEngine` (zero download, basic until MLC ready) |
| Hybrid Engine | `HybridAnswerEngine` (parallel async fusion) |
| First Launch | `FirstLaunchActivity` auto-downloads MLC when internet available |
| Android Compat | API 26 (min) → API 36 (target, Android 16) |

---

## 📂 Project Structure

```
app/src/main/java/ai/nexuzy/assistant/
├── SplashActivity.kt            # Routes first-launch vs returning user
├── FirstLaunchActivity.kt       # Auto-downloads MLC on first launch if internet
├── ChatActivity.kt
├── llm/
│   ├── HybridAnswerEngine.kt    # Online: DuckDuckGo + Sarvaam fused
│   ├── LocalOfflineEngine.kt    # Offline bridge: basic NLP until MLC downloaded
│   ├── MLCEngineWrapper.kt      # Orchestrates MLC + routing
│   ├── SarvaamAIClient.kt       # Sarvaam cloud AI
│   ├── ModelDownloadManager.kt  # One-time MLC download, resume, progress
│   ├── QwenEngine.kt
│   ├── ModelManager.kt
│   └── PromptBuilder.kt
├── middleware/
│   ├── IntentClassifier.kt
│   └── ToolExecutor.kt
└── tools/
    ├── NetworkUtils.kt
    ├── InternetSearchTool.kt
    ├── LinkReaderTool.kt
    ├── WeatherTool.kt
    ├── NewsTool.kt
    ├── LocationTool.kt
    └── DeviceControlTool.kt
```

---

## 🤝 Contributing
See **[CONTRIBUTING.md](CONTRIBUTING.md)**

## 🔒 Privacy
Zero data collected. See **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

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
