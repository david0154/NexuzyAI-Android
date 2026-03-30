<!-- David AI README -->
<p align="center">
  <img src="https://raw.githubusercontent.com/david0154/NexuzyAI-Android/main/app/src/main/res/drawable/ic_nexuzy_logo.xml" width="88" alt="David AI Logo" />
</p>

<h1 align="center">NexuzyAI</h1>
<p align="center">
  <b>On-Device + Cloud-Hybrid AI Assistant for Android</b><br/>
  Device-adaptive model tiers · 100% private offline · Voice + Text · MLC-LLM + Sarvaam AI + DuckDuckGo
</p>

<p align="center">
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" /></a>
  <a href="https://github.com/david0154/NexuzyAI-Android/blob/main/PRIVACY_POLICY.md"><img src="https://img.shields.io/badge/Privacy-No%20Data%20Collected-green.svg" /></a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg" />
  <img src="https://img.shields.io/badge/Model-Device--Adaptive-orange.svg" />
  <img src="https://img.shields.io/badge/AI-Offline%20%2B%20Cloud-blueviolet.svg" />
</p>

---

## 🧠 AI Generation Priority Chain

NexuzyAI uses a smart layered AI strategy based on what's available:

```
1. 🔒 Local MLC Model   — On-device Qwen3/Gemma (if mlc4j built)
        ↓ if not available
2. 🌐 Sarvaam AI API    — Cloud AI for accuracy (when internet ON + key set)
        ↓ if no key / fails
3. 🦆 DuckDuckGo Search — Real-time facts injected into AI context
        ↓ always available
4. 💡 Offline Fallback  — Tool result or guidance message
```

---

## 🧠 Model Tiers

NexuzyAI automatically selects the best model based on your device RAM:

| Display Name | Backend Model | RAM Required | Size | Best For |
|---|---|---|---|---|
| **David AI Lite** | Qwen3-0.6B-q0f16-MLC | ≥ 2 GB | ~700 MB | Budget / low-RAM phones |
| **David AI 1B** | Qwen3-1.7B-q4f16_1-MLC | ≥ 3 GB | ~1.4 GB | Most mid-range phones |
| **David AI 2B** | gemma-2-2b-it-q4f16_1-MLC | ≥ 5 GB | ~2.1 GB | Flagship phones |

Tap the model badge in the top bar to manually choose a different tier.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🧠 On-Device AI | Runs 100% on-device via MLC-LLM. Zero cloud dependency |
| 🌐 Sarvaam AI API | When internet is ON + key set: uses Sarvaam AI for accurate cloud responses |
| 🦆 DuckDuckGo Search | Auto-searches DuckDuckGo when AI doesn't know something (internet required) |
| 🔗 Link Reader | Paste any URL — AI fetches + reads + summarizes the page content |
| 📡 Internet-Gated Tools | Weather, News, Location (city name), Web Search gracefully require internet |
| 📱 Device-Adaptive | Auto-selects David AI Lite / 1B / 2B based on device RAM |
| 🎙️ Voice + Text | Both input modes work for all features and commands |
| 🌦️ Weather | Real-time via Open-Meteo (internet required) |
| 📰 News | Google News RSS headlines (internet required) |
| 📍 Location-aware | GPS works offline; city name via Geocoder (internet required) |
| 🗺️ Google Maps SDK | Maps SDK for Android + Geocoding API integrated |
| 📱 Device Control | Alarms, flashlight, media, open apps — all offline |
| 👨‍💻 Developer Info | Ask "Who made you?" — AI knows its own identity |
| 📮 AdMob Ads | Google AdMob banner (test IDs included) |
| 🌙 Day / Night Theme | Follows system setting |
| 🔒 Zero Data Collection | No servers. No tracking. Fully private. |

---

## 🗣️ What You Can Ask (Voice or Text)

```
"What's the weather today?"          → Live weather (internet required)
"Latest news"                        → Top headlines (internet required)
"Where am I?"                        → GPS city name (city needs internet)
"Set alarm at 7 AM"                  → Opens Android alarm
"Turn on flashlight"                 → Torch control
"Play music" / "Next song"           → Media key dispatch
"Open YouTube"                       → Launches app
"Who made you?"                      → Developer info (David, Nexuzy Lab)
"What model are you?"                → Shows David AI tier + RAM info
"What can you do?"                   → Feature overview
"Search for latest AI news"          → DuckDuckGo web search
"Summarize https://example.com"      → Reads + summarizes any URL
"Look up Python decorators"          → DuckDuckGo instant answer
Anything else                        → Local AI → Sarvaam AI → DuckDuckGo
```

---

## 🌐 Internet vs Offline Behaviour

| Feature | Internet OFF | Internet ON |
|---|---|---|
| AI Chat | ✅ Local MLC model | ✅ Local → Sarvaam AI (if key set) |
| Weather | ❌ Shows offline message | ✅ Live Open-Meteo data |
| News | ❌ Shows offline message | ✅ Google News RSS |
| Location (GPS) | ✅ Lat/Lon available | ✅ Full city name via Geocoder |
| Web Search | ❌ Not available | ✅ DuckDuckGo Instant Answers |
| Link Reader | ❌ Cannot fetch URL | ✅ Reads + summarizes any link |
| Unknown AI answers | 💡 Offline fallback | 🦆 DuckDuckGo context injected |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Android Views + XML (ConstraintLayout, CardView, RecyclerView) |
| AI Runtime | [MLC-LLM](https://github.com/mlc-ai/mlc-llm) (`mlc4j` module) |
| AI Models | Qwen3-0.6B (Lite) · Qwen3-1.7B (1B) · Gemma-2-2B (2B) |
| Cloud AI | [Sarvaam AI API](https://sarvam.ai) (optional, internet-gated) |
| Web Search | [DuckDuckGo Instant Answer API](https://api.duckduckgo.com) (no key needed) |
| Link Reader | OkHttp + HTML text extractor (any public URL) |
| Internet Check | `NetworkUtils` — `NetworkCapabilities.NET_CAPABILITY_VALIDATED` |
| Model Selection | `ModelManager.kt` — `ActivityManager.MemoryInfo.totalMem` |
| Voice STT | Android `SpeechRecognizer` |
| Voice TTS | Android `TextToSpeech` |
| Weather | [Open-Meteo API](https://open-meteo.com) (free, no key) |
| News | Google News RSS |
| Location | `FusedLocationProviderClient` + `Geocoder` |
| Maps | Google Maps SDK for Android + Geocoding API |
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

> **Works immediately** without MLC build. Weather, news, voice, web search, link reader, and device control all function out of the box (with internet for online features).

---

## 🔑 API Keys Setup

Copy `local.properties.example` → `local.properties` and fill in:

```properties
# Google Maps SDK for Android + Geocoding API
# Enable both at: https://console.cloud.google.com
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE

# News headlines — https://newsapi.org (free)
NEWS_API_KEY=YOUR_NEWS_API_KEY_HERE

# Sarvaam AI — cloud AI when internet is on (optional)
# Get at: https://sarvam.ai
# Leave blank to use DuckDuckGo context only
SARVAAM_API_KEY=YOUR_SARVAAM_API_KEY_HERE
```

> `local.properties` is git-ignored and never committed.

---

## 🧠 Enable On-Device AI (MLC-LLM)

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
├── ChatActivity.kt                      # Main chat screen (voice + text unified)
├── AboutActivity.kt                     # About + privacy + GitHub
├── PrivacyPolicyActivity.kt             # Full privacy policy
├── SplashActivity.kt                    # Splash screen with logo
├── NexuzyApp.kt                         # Application class
├── llm/
│   ├── ModelManager.kt                  # RAM detection + model tier selection
│   ├── QwenEngine.kt                    # High-level AI engine interface
│   ├── MLCEngineWrapper.kt              # MLC + Sarvaam AI fallback chain
│   ├── SarvaamAIClient.kt               # NEW: Sarvaam AI cloud API client
│   └── PromptBuilder.kt                 # Qwen3 chat-template prompt builder
├── middleware/
│   ├── IntentClassifier.kt              # Keyword intent detection (offline)
│   └── ToolExecutor.kt                  # Execute tools, internet-gated
├── tools/
│   ├── NetworkUtils.kt                  # NEW: Internet connectivity checker
│   ├── InternetSearchTool.kt            # NEW: DuckDuckGo Instant Answer search
│   ├── LinkReaderTool.kt                # NEW: URL fetcher + HTML text extractor
│   ├── WeatherTool.kt                   # Open-Meteo weather (internet required)
│   ├── NewsTool.kt                      # Google News RSS (internet required)
│   ├── LocationTool.kt                  # GPS + Geocoder
│   └── DeviceControlTool.kt             # Flashlight, media, alarm, app launch
├── services/
│   ├── AssistantService.kt              # Background assistant service
│   ├── AssistantAccessibilityService.kt # Accessibility integration
│   └── NotificationListenerService.kt  # Notification awareness
└── ui/
    ├── ModelSelectorFragment.kt         # Bottom sheet model picker
    └── adapter/ChatAdapter.kt           # RecyclerView chat bubbles
```

---

## 🤝 Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)**

---

## 🔒 Privacy

NexuzyAI collects **zero data**. On-device AI runs fully locally. When Sarvaam AI is used (optional, internet-gated), only your query is sent to the API — no personal data, no history stored. See **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

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
