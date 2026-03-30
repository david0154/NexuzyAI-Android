<!-- NexuzyAI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="NexuzyAI Logo" />
</p>

<h1 align="center">NexuzyAI</h1>
<p align="center">
  <b>On-Device + Cloud-Hybrid AI Assistant for Android</b><br/>
  Offline-first · MLC-LLM + Sarvaam AI + DuckDuckGo · Voice + Text · Zero data collection
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/AI-Offline%20%2B%20Cloud%20Hybrid-blueviolet.svg" />
  <a href="SETUP_GUIDE.md"><img src="https://img.shields.io/badge/Setup-Full%20Guide-orange.svg" /></a>
</p>

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
│  MLC model (if built) ► LocalOfflineEngine NLP    │
│  Handles: greetings, date/time, math, identity    │
│  → ALWAYS gives a real answer, never silent      │
└────────────────────────────────────────────────────┘
```

---

## 🧠 Model Tiers

NexuzyAI automatically selects the best model based on device RAM:

| Display Name | Backend Model | RAM Required | Size | Best For |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | Budget / low-RAM phones |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | Most mid-range phones |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | Flagship phones |

Tap the model badge in the top bar to manually choose a different tier.

---

## ✨ Features

| Feature | Internet | Details |
|---|---|---|
| 🧠 On-Device AI | ❌ Offline | MLC-LLM — 100% private, zero cloud |
| 🌐 Sarvaam AI API | ✅ Online | Cloud AI for accurate answers |
| 🦆 DuckDuckGo Search | ✅ Online | Grounding context for AI answers |
| 🔗 Link Reader | ✅ Online | Paste URL — AI reads & summarizes |
| 💡 LocalOfflineEngine | ❌ Offline | Smart NLP: date, math, greetings, identity |
| 🌦️ Weather | ✅ Online | Real-time via Open-Meteo |
| 📰 News | ✅ Online | Google News RSS + NewsAPI |
| 📍 Location | Both | GPS offline; city name online |
| 🗺️ Google Maps SDK | ✅ Online | Maps + Geocoding API |
| 📱 Device Control | ❌ Offline | Alarms, flashlight, media, app launch |
| 🎙️ Voice + Text | Both | Both input modes for all features |
| 📋 Copy / Paste | ❌ Offline | Long-press bubble to copy any message |
| 🔒 Zero Data | ❌ Offline | No servers, no tracking |

---

## 🗣️ What You Can Ask

```
"What's the weather today?"         → Live weather (internet)
"Latest news"                        → Top headlines (internet)
"Where am I?"                        → GPS + city name
"Set alarm at 7 AM"                  → Android alarm
"Turn on flashlight"                 → Torch control
"Open YouTube"                       → Launches app
"Search for AI news"                 → DuckDuckGo search
"Summarize https://example.com"      → Reads URL content
"What is 56 * 8?"                    → Math (works offline)
"What time is it?"                   → Device time (works offline)
"Who made you?"                      → Developer info (works offline)
```

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

---

## 🚀 Quick Start

```bash
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android
cp local.properties.example local.properties
# Fill in your API keys in local.properties
# Open in Android Studio → Sync → Run
```

> 📚 **Full setup instructions**: See **[SETUP_GUIDE.md](SETUP_GUIDE.md)** — covers Android Studio,
> API platforms, icon/logo replacement, keystore generation, SHA keys, and release APK signing.

---

## 🔑 API Keys Summary

| Key | Required | Where to get |
|---|---|---|
| `MAPS_API_KEY` | ✅ Yes | [console.cloud.google.com](https://console.cloud.google.com) — enable Maps SDK + Geocoding API |
| `NEWS_API_KEY` | ❌ Optional | [newsapi.org](https://newsapi.org) — free 100 req/day |
| `SARVAAM_API_KEY` | ❌ Optional | [sarvam.ai](https://sarvam.ai) — cloud AI accuracy |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Android Views + XML |
| On-Device AI | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j`) |
| AI Models | Qwen3-0.6B / Qwen3-1.7B / Gemma-2-2B |
| Cloud AI | [Sarvaam AI API](https://sarvam.ai) |
| Web Search | [DuckDuckGo Instant Answer API](https://api.duckduckgo.com) |
| Offline NLP | `LocalOfflineEngine` (built-in rule-based) |
| Hybrid Engine | `HybridAnswerEngine` (parallel async fusion) |
| Internet Check | `NetworkUtils.isInternetAvailable()` |
| Voice STT | Android `SpeechRecognizer` |
| Voice TTS | Android `TextToSpeech` |
| Weather | [Open-Meteo API](https://open-meteo.com) |
| News | Google News RSS + NewsAPI |
| Location | `FusedLocationProviderClient` + `Geocoder` |
| Maps | Google Maps SDK + Geocoding API |
| Networking | OkHttp 4 + Retrofit 2 |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

---

## 📂 Project Structure

```
app/src/main/java/ai/nexuzy/assistant/
├── ChatActivity.kt
├── llm/
│   ├── HybridAnswerEngine.kt    # Online: DuckDuckGo + Sarvaam fused
│   ├── LocalOfflineEngine.kt    # Offline: rule-based NLP (date/math/identity)
│   ├── MLCEngineWrapper.kt      # Orchestrates MLC + routing
│   ├── SarvaamAIClient.kt       # Sarvaam cloud AI
│   ├── QwenEngine.kt            # High-level AI interface
│   ├── ModelManager.kt          # RAM → model tier selection
│   └── PromptBuilder.kt         # Qwen3 chat template builder
├── middleware/
│   ├── IntentClassifier.kt      # Keyword intent detection
│   └── ToolExecutor.kt          # Tool runner (internet-gated)
└── tools/
    ├── NetworkUtils.kt          # Internet check
    ├── InternetSearchTool.kt    # DuckDuckGo search
    ├── LinkReaderTool.kt        # URL reader
    ├── WeatherTool.kt
    ├── NewsTool.kt
    ├── LocationTool.kt
    └── DeviceControlTool.kt
```

---

## 🤝 Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)**

---

## 🔒 Privacy

NexuzyAI collects **zero data**. On-device AI runs fully locally. Sarvaam AI is optional — only your query is sent when used. See **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

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
