<!-- NexuzyAI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="NexuzyAI Logo" />
</p>

<h1 align="center">NexuzyAI</h1>
<p align="center">
  <b>On-Device + Cloud-Hybrid AI Assistant for Android</b><br/>
  Offline-first · No download required on first install · MLC-LLM + Sarvaam AI + DuckDuckGo<br/>
  Voice + Text · Android 8–16 · Zero data collection
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026--36-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/Offline-First%20%E2%80%94%20No%20Download%20Needed-orange.svg" />
  <a href="SETUP_GUIDE.md"><img src="https://img.shields.io/badge/Setup-Full%20Guide-orange.svg" /></a>
</p>

---

## 📦 First Install — No Internet Required

```
Install APK
    ↓
Splash Screen
    ↓
First Launch Screen
    ├─ No Internet → "▶ Start Now" → Chat (fully offline, works immediately)
    └─ Internet ON → Optional: Download MLC model (~700MB–2.1GB)
                    OR skip → Chat (still fully works offline)
```

> ✅ **No download is ever mandatory.** The app works on first launch with zero internet
> using the built-in `LocalOfflineEngine` (handles greetings, date/time, math, identity).
> Connect to internet anytime for Sarvaam AI + DuckDuckGo hybrid answers.

---

## 🧠 AI Engine Routing

```
┌────────────────────────────────────────────────────┐
│  INTERNET ON                                      │
│  DuckDuckGo ──┬─► HybridAnswerEngine ► answer   │
│  Sarvaam AI ──┤          ▲                       │
│  Local MLC  ──┘ (fused when available)           │
├────────────────────────────────────────────────────┤
│  INTERNET OFF                                     │
│  MLC model (if downloaded) ► LocalOfflineEngine   │
│  Handles: greetings, date/time, math, identity    │
│  → ALWAYS gives a real answer — never silent     │
└────────────────────────────────────────────────────┘
```

---

## 🧠 Model Tiers

NexuzyAI automatically selects the best model based on device RAM.
MLC models are **optional** — downloadable from the First Launch screen.

| Display Name | Backend Model | RAM Required | Size | Best For |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | Budget / low-RAM phones |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | Most mid-range phones |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | Flagship phones |

---

## ✨ Features

| Feature | Internet | Details |
|---|---|---|
| 📦 First Install | ❌ Not required | Works immediately offline via LocalOfflineEngine |
| 🧠 On-Device AI | ❌ Offline | MLC-LLM — optional download, 100% private |
| 💡 LocalOfflineEngine | ❌ Offline | Built-in NLP: date, time, math, greetings, identity |
| 🌐 Sarvaam AI API | ✅ Online | Cloud AI for accurate answers |
| 🦆 DuckDuckGo Search | ✅ Online | Grounding context for AI answers |
| 🔗 Link Reader | ✅ Online | Paste URL — AI reads & summarizes |
| 🌦️ Weather | ✅ Online | Real-time via Open-Meteo |
| 📰 News | ✅ Online | Google News RSS + NewsAPI |
| 📍 Location | Both | GPS offline; city name online |
| 🗳️ Google Maps SDK | ✅ Online | Maps + Geocoding API |
| 📱 Device Control | ❌ Offline | Alarms, flashlight, media, app launch |
| 🎙️ Voice + Text | Both | Both input modes for all features |
| 🔒 Zero Data | ❌ Offline | No servers, no tracking ever |

---

## 🌐 Internet vs Offline

| Feature | Offline | Online |
|---|---|---|
| AI Chat | ✅ LocalOfflineEngine NLP | ✅ Sarvaam + DuckDuckGo fused |
| Weather | ❌ Offline message | ✅ Open-Meteo live |
| News | ❌ Offline message | ✅ RSS + NewsAPI |
| Location | ✅ GPS coords | ✅ Full city name |
| Web Search | ❌ Not available | ✅ DuckDuckGo |
| Link Reader | ❌ Cannot fetch | ✅ OkHttp + HTML strip |
| Date / Time | ✅ Device clock | ✅ Device clock |
| Math | ✅ Built-in solver | ✅ Built-in solver |
| MLC Download | ❌ Unavailable | ✅ Optional via First Launch screen |

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
| On-Device AI | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j`) — optional |
| AI Models | Qwen3-0.6B / Qwen3-1.7B / Gemma-2-2B |
| Cloud AI | [Sarvaam AI API](https://sarvam.ai) |
| Web Search | DuckDuckGo Instant Answer API |
| Offline NLP | `LocalOfflineEngine` (built-in, zero download) |
| Hybrid Engine | `HybridAnswerEngine` (parallel async fusion) |
| First Launch | `FirstLaunchActivity` + `ModelDownloadManager` |
| Android Compat | API 26 (min) → API 36 (target, Android 16) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 16 (API 36) |

---

## 📂 Project Structure

```
app/src/main/java/ai/nexuzy/assistant/
├── SplashActivity.kt            # Routes first-launch vs returning user
├── FirstLaunchActivity.kt       # First-install screen (offline OK, download optional)
├── ChatActivity.kt
├── llm/
│   ├── HybridAnswerEngine.kt    # Online: DuckDuckGo + Sarvaam fused
│   ├── LocalOfflineEngine.kt    # Offline: rule-based NLP (always works)
│   ├── MLCEngineWrapper.kt      # Orchestrates MLC + routing
│   ├── SarvaamAIClient.kt       # Sarvaam cloud AI
│   ├── ModelDownloadManager.kt  # Optional MLC model download with progress
│   ├── QwenEngine.kt            # High-level AI interface
│   ├── ModelManager.kt          # RAM → model tier selection
│   └── PromptBuilder.kt         # Qwen3 chat template builder
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
