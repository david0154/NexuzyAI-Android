# 🛠️ NexuzyAI Android — Complete Setup Guide

> **Full walkthrough** from a fresh machine to a signed release APK.
> Follow every section in order for the smoothest experience.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Android Studio Setup](#2-android-studio-setup)
3. [Clone the Repository](#3-clone-the-repository)
4. [Project Structure Overview](#4-project-structure-overview)
5. [First Install Behavior](#5-first-install-behavior)
6. [API Keys Setup](#6-api-keys-setup)
   - [Google Maps SDK + Geocoding API](#61-google-maps-sdk--geocoding-api)
   - [NewsAPI (optional)](#62-newsapi-optional)
   - [Sarvaam AI API (optional)](#63-sarvaam-ai-api-optional)
   - [Google AdMob (optional)](#64-google-admob-optional)
7. [Logo & App Icon Replacement](#7-logo--app-icon-replacement)
8. [Keystore Generation](#8-keystore-generation)
9. [SHA-1 & SHA-256 Key Extraction](#9-sha-1--sha-256-key-extraction)
10. [Build Debug APK](#10-build-debug-apk)
11. [Build Signed Release APK](#11-build-signed-release-apk)
12. [Enable On-Device MLC-LLM Model](#12-enable-on-device-mlc-llm-model)
13. [Android 15 & 16 Compatibility](#13-android-15--16-compatibility)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Download |
|---|---|---|
| **JDK** | 17 or 21 (LTS) | [adoptium.net](https://adoptium.net) |
| **Android Studio** | Meerkat (2024.3+) | [developer.android.com/studio](https://developer.android.com/studio) |
| **Git** | Latest | [git-scm.com](https://git-scm.com) |
| **Python** | 3.10+ (MLC only) | [python.org](https://python.org) |

---

## 2. Android Studio Setup

### Step 1 — Install Android Studio
1. Download **Android Studio Meerkat** and install with **Standard** setup
2. Let it download the Android SDK automatically

### Step 2 — Configure SDK
**File → Settings → Android SDK:**

| SDK Platforms | Install |
|---|---|
| Android 16 (API 36) | ✅ Target / Compile SDK |
| Android 15 (API 35) | ✅ Recommended |
| Android 14 (API 34) | ✅ Recommended |
| Android 8.0 (API 26) | ✅ Min SDK |

| SDK Tools | Install |
|---|---|
| Android SDK Build-Tools 36 | ✅ Required |
| Android Emulator | ✅ For testing |
| Android SDK Platform-Tools | ✅ Required (adb) |
| CMake | ✅ For native libs |

### Step 3 — Configure JDK
**File → Project Structure → SDK Location** → set JDK 17/21 path or use embedded JDK.

---

## 3. Clone the Repository

```bash
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android
cp local.properties.example local.properties
```

Open in Android Studio: **File → Open** → select `NexuzyAI-Android` folder.
Wait for Gradle sync (first time: 3–5 min).

---

## 4. Project Structure Overview

```
NexuzyAI-Android/
├── app/src/main/
│   ├── java/ai/nexuzy/assistant/
│   │   ├── SplashActivity.kt          ← Routes first-launch vs returning
│   │   ├── FirstLaunchActivity.kt     ← First-install screen
│   │   ├── ChatActivity.kt
│   │   ├── llm/
│   │   │   ├── HybridAnswerEngine.kt  ← Online: DDG + Sarvaam fused
│   │   │   ├── LocalOfflineEngine.kt  ← Offline: always works
│   │   │   ├── ModelDownloadManager.kt← Optional MLC download
│   │   │   └── MLCEngineWrapper.kt
│   │   └── tools/ middleware/
│   └── res/layout/activity_first_launch.xml
├── local.properties          ← Your secret keys (never commit!)
└── local.properties.example  ← Template — safe to commit
```

---

## 5. First Install Behavior

> ✅ **No internet is required to use NexuzyAI on first install.**

When the user installs the APK for the first time:

```
SplashActivity (1.8s)
       ↓
[First time?]
  YES → FirstLaunchActivity
           ├─ Internet OFF → Shows "Start Now" only
           └─ Internet ON  → Shows "Download MLC Model (optional)" + "Start Now"
                                     ↓ skip or download
  NO  → ChatActivity (direct)
```

### What works immediately without any download:
- 💬 Chat (greetings, date/time, math, who are you — via `LocalOfflineEngine`)
- ⏰ Set alarms
- 💡 Flashlight on/off
- 🎵 Media control
- 🚀 Open apps
- 📍 Location (GPS)

### What becomes better with internet:
- 🌐 Sarvaam AI + DuckDuckGo → accurate answers for any question
- 🌦️ Weather, 📰 News, 🔗 Link Reader, 🔍 Web Search

### What becomes better with MLC model download:
- 🧠 Fully local AI for complex conversations entirely offline
- Download size: ~700 MB (Lite) to ~2.1 GB (2B) depending on device RAM

---

## 6. API Keys Setup

All keys go in **`local.properties`** (never commit — in `.gitignore`):

```properties
sdk.dir=/path/to/Android/Sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_KEY
NEWS_API_KEY=YOUR_NEWSAPI_KEY
SARVAAM_API_KEY=YOUR_SARVAAM_KEY
KEYSTORE_PATH=../keystore/nexuzy-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=nexuzy
KEY_PASSWORD=your_key_password
```

### 6.1 Google Maps SDK + Geocoding API

1. Go to [console.cloud.google.com](https://console.cloud.google.com) → New Project `NexuzyAI`
2. **APIs & Services → Library** → enable:
   - `Maps SDK for Android`
   - `Geocoding API`
3. **Credentials → Create Credentials → API Key** → copy key
4. Add to `local.properties`: `MAPS_API_KEY=AIzaSyXXX...`
5. Restrict key: Android apps → package `ai.nexuzy.assistant` + SHA-1

> Free: $200/month Maps credit + 40,000 Geocoding calls/month

### 6.2 NewsAPI (Optional)

1. Register at [newsapi.org/register](https://newsapi.org/register)
2. Copy API key → `NEWS_API_KEY=...`

> Free: 100 req/day. Without key: uses Google News RSS (unlimited, no key).

### 6.3 Sarvaam AI API (Optional)

1. Sign up at [sarvam.ai](https://sarvam.ai) → API Keys section
2. Copy key → `SARVAAM_API_KEY=...`

> Without key: app uses DuckDuckGo + offline engine. Still works great.

### 6.4 Google AdMob (Optional)

1. Create account at [admob.google.com](https://admob.google.com)
2. Add app → get App ID → replace in `AndroidManifest.xml`:
   ```xml
   android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"
   ```
3. Replace banner ID test value in `ChatActivity.kt`

---

## 7. Logo & App Icon Replacement

**Method 1 — Image Asset Studio (recommended):**
1. Right-click `res` → **New → Image Asset**
2. Type: **Launcher Icons (Adaptive and Legacy)**
3. Choose your PNG/SVG (min 512×512 px) → Finish

**Method 2 — Manual:**
```
mipmap-mdpi/     ic_launcher.png   48x48
mipmap-hdpi/     ic_launcher.png   72x72
mipmap-xhdpi/    ic_launcher.png   96x96
mipmap-xxhdpi/   ic_launcher.png   144x144
mipmap-xxxhdpi/  ic_launcher.png   192x192
```
Also replace `ic_launcher_round.png` in each folder.

**App name:** Edit `res/values/strings.xml`:
```xml
<string name="app_name">YourAppName</string>
```

---

## 8. Keystore Generation

```bash
mkdir -p keystore
keytool -genkey -v \
  -keystore keystore/nexuzy-release.jks \
  -alias nexuzy \
  -keyalg RSA -keysize 2048 -validity 10000
```

Fill in: name, org (Nexuzy Lab), city (Kolkata), state (West Bengal), country (IN).

> ⚠️ **Back up your `.jks` file safely. Never commit it to GitHub.**

Add to `.gitignore`:
```
keystore/
*.jks
*.keystore
```

Add to `local.properties`:
```properties
KEYSTORE_PATH=../keystore/nexuzy-release.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=nexuzy
KEY_PASSWORD=your_key_password
```

---

## 9. SHA-1 & SHA-256 Key Extraction

### Debug SHA-1

**Windows:**
```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

### Release SHA-1
```bash
keytool -list -v -keystore keystore/nexuzy-release.jks -alias nexuzy
```

### Via Android Studio
**Gradle panel → app → Tasks → android → signingReport** → double-click.

---

## 10. Build Debug APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 11. Build Signed Release APK

**Via Android Studio:**
**Build → Generate Signed Bundle / APK → APK → Next** → browse keystore → Finish.

**Via Terminal:**
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

**Verify signing:**
```bash
android-sdk/build-tools/36.0.0/apksigner verify --verbose app-release.apk
```

---

## 12. Enable On-Device MLC-LLM Model

The in-app download (First Launch screen) downloads model files at runtime.
For developer builds with the model baked in:

```bash
pip install mlc-llm
python3 -m mlc_llm package
```

Then:
1. Uncomment `include ':mlc4j'` in `settings.gradle`
2. Uncomment `implementation project(':mlc4j')` in `app/build.gradle`
3. Set `MLC_AVAILABLE = true` in `MLCEngineWrapper.kt`
4. Uncomment MLC engine calls
5. Run `./gradlew assembleDebug`

---

## 13. Android 15 & 16 Compatibility

| API | Feature | Status |
|---|---|---|
| 35 (Android 15) | Edge-to-edge enforced | ✅ Fixed via `WindowCompat` + `ViewCompat` insets |
| 35 (Android 15) | Display cutout mode | ✅ `shortEdges` on all activities |
| 36 (Android 16) | Predictive back gesture | ✅ `OnBackPressedCallback` + manifest flag |
| 36 (Android 16) | compileSdk + targetSdk | ✅ Both set to 36 |

All AndroidX libraries updated to versions supporting API 36.
See `app/build.gradle` for full dependency versions.

---

## 14. Troubleshooting

| Problem | Fix |
|---|---|
| Gradle sync fails | File → Invalidate Caches → Restart |
| `MAPS_API_KEY` not found | Ensure `local.properties` exists with the key |
| Maps not loading | Enable Maps SDK for Android in Google Cloud Console |
| Geocoder returns null | Enable Geocoding API in Google Cloud Console |
| First launch shows every time | Check `nexuzy_prefs` SharedPrefs `first_launch_done` flag |
| MLC download fails | Check internet, retry — partial download resumes automatically |
| Content hidden behind status bar | `WindowCompat.setDecorFitsSystemWindows(window, false)` already applied |
| Back gesture not animated | `enableOnBackInvokedCallback=true` in manifest already set |
| APK install `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `adb uninstall ai.nexuzy.assistant` then reinstall |
| `keytool` not found | Add JDK `bin` to PATH |
| Build error: API 36 not found | Install Android SDK Platform 36 in SDK Manager |

---

## 📧 Support

| | |
|---|---|
| 📧 Nexuzy Lab | nexuzylab@gmail.com |
| 📧 Developer | davidk76011@gmail.com |
| 🐙 Issues | [Open an issue](https://github.com/david0154/NexuzyAI-Android/issues) |

---
<p align="center">Made with ❤️ by <b>David</b> · <b>Nexuzy Lab</b></p>
