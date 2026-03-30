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
  <a href="SETUP_GUIDE.md"><img src="https://img.shields.io/badge/Setup-Full%20Guide-orange.svg" /></a>
</p>

---

## 📦 First Install — Honest Explanation

> ⚠️ **You cannot download the AI model without internet.** Here is what exactly works in each scenario:

### 🔴 No Internet, Fresh Install
| What works | What does NOT work |
|---|---|
| ✅ App opens and runs | ❌ MLC model download (needs internet) |
| ✅ Greetings, date, time, math | ❌ Real AI conversations |
| ✅ Alarms, flashlight, media, app launch | ❌ Weather, news, web search |
| ✅ GPS location (coordinates only) | ❌ City name / reverse geocoding |
| ⚠️ Very basic NLP only (`LocalOfflineEngine`) | ❌ Sarvaam AI + DuckDuckGo |

> `LocalOfflineEngine` is **NOT** a full AI model. It is a lightweight rule-based engine
> that only answers: greetings · who are you · date/time · simple math.
> For anything else it says: *"I need internet for this."*

---

### 🟢 With Internet (Recommended for best experience)
```
Install APK → First Launch Screen
    └─ Internet ON
          ├─ Option A: "⬇ Download MLC Model" (~700MB–2.1GB)
          │      ↓ downloads once, stored on device
          │      ↓ after download: full AI works OFFLINE forever
          └─ Option B: "▶ Start Now" (skip download)
                 ↓ uses Sarvaam AI + DuckDuckGo online
                 ↓ real AI answers — but needs internet each time
```

### 🔵 After MLC Model Downloaded (Best Offline Experience)
```
No Internet needed anymore for AI chat!
MLC model runs 100% on-device, no server, no data sent.
Download is ONE TIME only (~700MB to ~2.1GB depending on your RAM).
```

---

## 🧠 AI Engine Routing

```
┌────────────────────────────────────────────────────────┐
│  INTERNET ON                                           │
│  DuckDuckGo ──┬─► HybridAnswerEngine ► answer        │
│  Sarvaam AI ──┤                                       │
│  Local MLC  ──┘ (fused when MLC downloaded)           │
├────────────────────────────────────────────────────────┤
│  INTERNET OFF + MLC downloaded                         │
│  MLC on-device model ► full AI offline conversations   │
├────────────────────────────────────────────────────────┤
│  INTERNET OFF + NO MLC download                        │
│  LocalOfflineEngine (rule-based only)                  │
│  ✅ greetings · date/time · math · identity            │
│  ❌ real AI chat — shows "connect to internet" message │
└────────────────────────────────────────────────────────┘
```

---

## 🧠 Model Tiers

MLC models are **optional** but recommended for full offline AI.
Downloaded once from the First Launch screen when internet is available.

| Display Name | Backend Model | RAM Required | Download Size | Best For |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | Budget / low-RAM phones |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | Most mid-range phones |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | Flagship phones |

> Model is auto-selected based on your device RAM. Download happens once.
> After download, full AI works permanently offline.

---

## ✨ Features

| Feature | Needs Internet? | Notes |
|---|---|---|
| 🧠 On-Device MLC AI | ⚠️ Once to download | After download: 100% offline forever |
| 💡 LocalOfflineEngine | ❌ Never | Basic only: greetings, date, time, math, identity |
| 🌐 Sarvaam AI API | ✅ Always | Cloud AI — best answer quality online |
| 🦆 DuckDuckGo Search | ✅ Always | Grounding context for AI answers |
| 🔗 Link Reader | ✅ Always | Paste URL — AI reads & summarizes |
| 🌦️ Weather | ✅ Always | Real-time via Open-Meteo |
| 📰 News | ✅ Always | Google News RSS + NewsAPI |
| 📍 Location | ⚠️ Partial | GPS offline ✅, city name needs internet |
| 🗳️ Google Maps SDK | ✅ Always | Maps + Geocoding API |
| 📱 Device Control | ❌ Never | Alarms, flashlight, media, app launch |
| 🎙️ Voice + Text | ❌ Never | Both input modes work offline |
| 🔒 Zero Data Collection | ❌ Never | No servers, no tracking |

---

## 🌐 Honest Internet vs Offline Table

| What you ask | No Internet, No MLC | No Internet + MLC downloaded | Internet (no MLC) |
|---|---|---|---|
| "Hello / Hi" | ✅ Answers | ✅ Answers | ✅ Answers |
| "What time is it?" | ✅ Answers | ✅ Answers | ✅ Answers |
| "What is 5 + 3?" | ✅ Answers | ✅ Answers | ✅ Answers |
| "Who made you?" | ✅ Answers | ✅ Answers | ✅ Answers |
| "Explain quantum physics" | ❌ Needs internet | ✅ MLC answers | ✅ Sarvaam AI answers |
| "Write me a poem" | ❌ Needs internet | ✅ MLC answers | ✅ Sarvaam AI answers |
| "What's the weather?" | ❌ Needs internet | ❌ Needs internet | ✅ Open-Meteo live |
| "Latest news" | ❌ Needs internet | ❌ Needs internet | ✅ RSS + NewsAPI |
| "Search the web" | ❌ Needs internet | ❌ Needs internet | ✅ DuckDuckGo |
| Set alarm / flashlight | ✅ Works | ✅ Works | ✅ Works |

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
| On-Device AI | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j`) — optional download |
| AI Models | Qwen3-0.6B / Qwen3-1.7B / Gemma-2-2B |
| Cloud AI | [Sarvaam AI API](https://sarvam.ai) |
| Web Search | DuckDuckGo Instant Answer API |
| Offline NLP | `LocalOfflineEngine` (rule-based, zero download, basic only) |
| Hybrid Engine | `HybridAnswerEngine` (parallel async fusion) |
| First Launch | `FirstLaunchActivity` + `ModelDownloadManager` |
| Android Compat | API 26 (min) → API 36 (target, Android 16) |

---

## 📂 Project Structure

```
app/src/main/java/ai/nexuzy/assistant/
├── SplashActivity.kt            # Routes first-launch vs returning user
├── FirstLaunchActivity.kt       # First-install: shows download option if internet available
├── ChatActivity.kt
├── llm/
│   ├── HybridAnswerEngine.kt    # Online: DuckDuckGo + Sarvaam fused
│   ├── LocalOfflineEngine.kt    # Offline fallback: basic rule-based NLP only
│   ├── MLCEngineWrapper.kt      # Orchestrates MLC + routing
│   ├── SarvaamAIClient.kt       # Sarvaam cloud AI
│   ├── ModelDownloadManager.kt  # MLC model download (internet required once)
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
