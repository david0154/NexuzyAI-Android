# 🛠️ NexuzyAI Android — Complete Setup Guide

> **Full walkthrough** from a fresh machine to a signed release APK.
> Follow every section in order for the smoothest experience.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Android Studio Setup](#2-android-studio-setup)
3. [Clone the Repository](#3-clone-the-repository)
4. [Project Structure Overview](#4-project-structure-overview)
5. [API Keys Setup](#5-api-keys-setup)
   - [Google Maps SDK + Geocoding API](#51-google-maps-sdk--geocoding-api)
   - [NewsAPI (optional)](#52-newsapi-optional)
   - [Sarvaam AI API (optional)](#53-sarvaam-ai-api-optional)
   - [Google AdMob (optional)](#54-google-admob-optional)
6. [Logo & App Icon Replacement](#6-logo--app-icon-replacement)
7. [Keystore Generation](#7-keystore-generation)
8. [SHA-1 & SHA-256 Key Extraction](#8-sha-1--sha-256-key-extraction)
9. [Build Debug APK](#9-build-debug-apk)
10. [Build Signed Release APK](#10-build-signed-release-apk)
11. [Enable On-Device MLC-LLM Model](#11-enable-on-device-mlc-llm-model)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Prerequisites

Make sure you have the following installed before starting:

| Tool | Version | Download |
|---|---|---|
| **JDK** | 17 or 21 (LTS) | [adoptium.net](https://adoptium.net) |
| **Android Studio** | Meerkat (2024.3+) | [developer.android.com/studio](https://developer.android.com/studio) |
| **Git** | Latest | [git-scm.com](https://git-scm.com) |
| **Python** | 3.10+ (for MLC only) | [python.org](https://python.org) |

> ⚠️ **Windows users**: Use PowerShell or Git Bash for all terminal commands.

---

## 2. Android Studio Setup

### Step 1 — Install Android Studio
1. Download **Android Studio Meerkat** from [developer.android.com/studio](https://developer.android.com/studio)
2. Run the installer and follow the wizard
3. Select **Standard** installation type
4. Let it download the Android SDK (API 34 recommended)

### Step 2 — Configure SDK
Go to **File → Settings → Android SDK** (or **Android Studio → Settings** on Mac):

| SDK Platforms tab | Install |
|---|---|
| Android 14 (API 34) | ✅ Required |
| Android 8.0 (API 26) | ✅ Minimum target |

| SDK Tools tab | Install |
|---|---|
| Android SDK Build-Tools 34 | ✅ Required |
| Android Emulator | ✅ For testing |
| Android SDK Platform-Tools | ✅ Required (adb) |
| CMake | ✅ Required for native libs |

### Step 3 — Configure JDK
Go to **File → Project Structure → SDK Location**:
- Set **JDK Location** to your JDK 17 or 21 install path
- Or use the bundled JDK: check **Use embedded JDK**

---

## 3. Clone the Repository

```bash
# Clone
git clone https://github.com/david0154/NexuzyAI-Android.git
cd NexuzyAI-Android

# Copy the example keys file
cp local.properties.example local.properties
```

Now open the project in Android Studio:
- **File → Open** → select the `NexuzyAI-Android` folder
- Wait for Gradle sync to complete (first time may take 3–5 minutes)
- If Gradle sync fails: **File → Sync Project with Gradle Files**

---

## 4. Project Structure Overview

```
NexuzyAI-Android/
├── app/
│   ├── src/main/
│   │   ├── java/ai/nexuzy/assistant/   ← All Kotlin source code
│   │   ├── res/
│   │   │   ├── drawable/               ← Logo SVG/XML files
│   │   │   ├── mipmap-*/               ← App launcher icons
│   │   │   ├── layout/                 ← XML UI layouts
│   │   │   └── values/                 ← Colors, strings, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle                    ← Dependencies + API key injection
├── local.properties                    ← YOUR SECRET KEYS (never commit!)
├── local.properties.example            ← Template — safe to commit
├── SETUP_GUIDE.md                      ← This file
└── README.md
```

---

## 5. API Keys Setup

All keys go into **`local.properties`** (never commit this file — it is in `.gitignore`).

```properties
# local.properties
sdk.dir=/path/to/your/Android/Sdk
MAPS_API_KEY=YOUR_GOOGLE_MAPS_KEY
NEWS_API_KEY=YOUR_NEWSAPI_KEY
SARVAAM_API_KEY=YOUR_SARVAAM_KEY
KEYSTORE_PATH=../keystore/nexuzy-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=nexuzy
KEY_PASSWORD=your_key_password
```

---

### 5.1 Google Maps SDK + Geocoding API

Required for: location city name, Google Maps display.

**Step-by-step on Google Cloud Console:**

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Click **Select a project** → **New Project** → name it `NexuzyAI`
3. In the left menu: **APIs & Services → Library**
4. Search and **Enable** both:
   - `Maps SDK for Android`
   - `Geocoding API`
5. Go to **APIs & Services → Credentials**
6. Click **Create Credentials → API Key**
7. Copy the key → paste into `local.properties`:
   ```properties
   MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```
8. (Recommended) Click **Restrict Key**:
   - Application restriction: **Android apps**
   - Add your package name: `ai.nexuzy.assistant`
   - Add your SHA-1 fingerprint (see [Section 8](#8-sha-1--sha-256-key-extraction))

> 💡 **Free tier**: Maps SDK has $200/month free credit. Geocoding API has 40,000 free calls/month.

---

### 5.2 NewsAPI (Optional)

For better news headlines. Falls back to free Google News RSS if not set.

1. Register free at [newsapi.org/register](https://newsapi.org/register)
2. Verify your email → copy your API key
3. Add to `local.properties`:
   ```properties
   NEWS_API_KEY=your_newsapi_key_here
   ```

> 💡 Free tier: 100 requests/day. Leave blank to use Google News RSS (unlimited, no key).

---

### 5.3 Sarvaam AI API (Optional)

For cloud AI responses when internet is available (higher accuracy than offline NLP).

1. Go to [sarvam.ai](https://sarvam.ai) → Sign up
2. Navigate to **API Keys** section
3. Create a new key → copy it
4. Add to `local.properties`:
   ```properties
   SARVAAM_API_KEY=your_sarvaam_api_key_here
   ```

> 💡 Without this key the app still works — uses DuckDuckGo + offline engine instead.

---

### 5.4 Google AdMob (Optional)

For in-app ads. Test IDs are pre-configured in the project.

1. Go to [admob.google.com](https://admob.google.com) → Create account
2. Add your app → get your **App ID**
3. In `app/src/main/AndroidManifest.xml`, replace:
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX" />
   ```
4. Create Ad Unit IDs for Banner → replace test IDs in `ChatActivity.kt`

> 💡 Leave as-is for test ads during development. Never use test IDs in production.

---

## 6. Logo & App Icon Replacement

### Replace the App Launcher Icon

**Method 1 — Android Studio Image Asset Studio (Recommended):**
1. In Android Studio, right-click `app/src/main/res` → **New → Image Asset**
2. Icon Type: **Launcher Icons (Adaptive and Legacy)**
3. Source Asset: choose your PNG/SVG logo (min 512×512 px recommended)
4. Set background color or image as needed
5. Click **Next → Finish** — it generates all `mipmap-*` sizes automatically

**Method 2 — Manual replacement:**
Replace files in each density folder:
```
app/src/main/res/
├── mipmap-mdpi/    ic_launcher.png        (48x48)
├── mipmap-hdpi/    ic_launcher.png        (72x72)
├── mipmap-xhdpi/   ic_launcher.png        (96x96)
├── mipmap-xxhdpi/  ic_launcher.png        (144x144)
└── mipmap-xxxhdpi/ ic_launcher.png        (192x192)
```
Also replace `ic_launcher_round.png` in each folder for round icons.

### Replace the In-App Logo (Splash / About screen)

Replace `app/src/main/res/drawable/ic_nexuzy_logo.xml` with your own SVG/XML vector drawable,
or replace all references to it with a PNG in the drawable folder.

### App Name

Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">YourAppName</string>
```

---

## 7. Keystore Generation

A keystore is required to sign your release APK for Google Play or direct distribution.

### Create the keystore directory
```bash
mkdir -p keystore
```

### Generate keystore using keytool (JDK)
```bash
keytool -genkey -v \
  -keystore keystore/nexuzy-release.jks \
  -alias nexuzy \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted to enter:
```
Enter keystore password:      [choose a strong password]
Re-enter new password:        [same password]
What is your first and last name?  [Your Name]
What is your organization unit?    [Nexuzy Lab]
What is your organization name?    [Nexuzy Lab]
What is your city or locality?     [Kolkata]
What is your state or province?    [West Bengal]
What is your two-letter country code?  [IN]
Is CN=... correct?  [yes]
Enter key password for nexuzy:    [can be same as keystore password]
```

> ⚠️ **CRITICAL**: Back up `keystore/nexuzy-release.jks` safely. If you lose it, you cannot
> update your app on Google Play. **Never commit it to GitHub.**

Add to `.gitignore`:
```
keystore/
*.jks
*.keystore
```

Add keystore config to `local.properties`:
```properties
KEYSTORE_PATH=../keystore/nexuzy-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=nexuzy
KEY_PASSWORD=your_key_password
```

---

## 8. SHA-1 & SHA-256 Key Extraction

SHA keys are needed for:
- Restricting your Google Maps API key to your app
- Firebase project setup (if you add Firebase later)
- Google Sign-In

### Debug SHA-1 (for development)

**Windows (PowerShell):**
```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey `
  -storepass android `
  -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

### Release SHA-1 (for production)

```bash
keytool -list -v \
  -keystore keystore/nexuzy-release.jks \
  -alias nexuzy
```
Enter your keystore password when prompted.

**Output to look for:**
```
Certificate fingerprints:
  SHA1:   AA:BB:CC:DD:EE:FF:...
  SHA256: AA:BB:CC:DD:EE:FF:...
```

Copy the **SHA1** value and paste it into your Google Cloud Console API key restriction.

### Via Android Studio (easiest)
1. Open **Gradle** panel (right sidebar)
2. Navigate: **app → Tasks → android → signingReport**
3. Double-click **signingReport**
4. SHA-1 and SHA-256 appear in the **Run** console

---

## 9. Build Debug APK

A debug APK is unsigned and for testing only. No keystore needed.

### Via Android Studio:
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Click **locate** in the notification to find the APK
3. Output: `app/build/outputs/apk/debug/app-debug.apk`

### Via Terminal:
```bash
# Windows
.\gradlew assembleDebug

# Mac/Linux
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install directly to connected device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 10. Build Signed Release APK

### Step 1 — Configure signing in `app/build.gradle`

Make sure these lines exist in `app/build.gradle` (already set up in this project):
```groovy
android {
    signingConfigs {
        release {
            storeFile     file(KEYSTORE_PATH)
            storePassword KEYSTORE_PASSWORD
            keyAlias      KEY_ALIAS
            keyPassword   KEY_PASSWORD
        }
    }
    buildTypes {
        release {
            signingConfig    signingConfigs.release
            minifyEnabled    true
            shrinkResources  true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Step 2 — Build release APK

**Via Android Studio:**
1. **Build → Generate Signed Bundle / APK**
2. Select **APK** → Next
3. Browse to your `keystore/nexuzy-release.jks`
4. Enter keystore password, key alias (`nexuzy`), key password
5. Select **release** build variant → Finish
6. Output: `app/build/outputs/apk/release/app-release.apk`

**Via Terminal:**
```bash
# Windows
.\gradlew assembleRelease

# Mac/Linux
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Step 3 — Verify the APK is signed
```bash
android-sdk/build-tools/34.0.0/apksigner verify --verbose app-release.apk
```
Expected output: `Verified using v2 scheme (APK Signature Scheme v2): true`

---

## 11. Enable On-Device MLC-LLM Model

This section enables the fully local AI model (Qwen3/Gemma runs 100% on-device).

### Step 1 — Install MLC-LLM
```bash
pip install mlc-llm
```

### Step 2 — Package the model
```bash
# From the project root
python3 -m mlc_llm package
```
This reads `mlc-package-config.json` and generates `dist/lib/mlc4j/`.

### Step 3 — Uncomment in `settings.gradle`
```groovy
// Uncomment this line:
include ':mlc4j'
```

### Step 4 — Uncomment in `app/build.gradle`
```groovy
// Uncomment this line:
implementation project(':mlc4j')
```

### Step 5 — Enable in code
In `MLCEngineWrapper.kt`, change:
```kotlin
const val MLC_AVAILABLE = false   // ← change to:
const val MLC_AVAILABLE = true
```
Also uncomment the MLC import lines and engine call blocks.

### Step 6 — Sync and rebuild
```bash
./gradlew assembleDebug
```

> 📊 Model sizes: Lite ~700MB, 1B ~1.4GB, 2B ~2.1GB. Make sure your test device has enough storage.

---

## 12. Troubleshooting

| Problem | Fix |
|---|---|
| Gradle sync fails | File → Invalidate Caches → Restart |
| `MAPS_API_KEY` not found | Make sure `local.properties` exists and has the key |
| Maps not showing | Enable Maps SDK for Android in Google Cloud Console |
| Geocoder returns null | Enable Geocoding API in Google Cloud Console |
| Build error `local.properties missing` | Run `cp local.properties.example local.properties` |
| APK install fails `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall old version first: `adb uninstall ai.nexuzy.assistant` |
| `keytool` not found | Add JDK `bin` folder to your PATH environment variable |
| News shows raw titles with source names | Update `NewsTool.kt` — already fixed in latest commit |
| Sarvaam AI returns null | Check `SARVAAM_API_KEY` is correct and not expired |
| Offline mode shows no answer | `LocalOfflineEngine` handles this — check latest `HybridAnswerEngine.kt` |

---

## 📧 Support

| | |
|---|---|
| 📧 Nexuzy Lab | nexuzylab@gmail.com |
| 📧 Developer | davidk76011@gmail.com |
| 🐙 GitHub Issues | [Open an issue](https://github.com/david0154/NexuzyAI-Android/issues) |

---

<p align="center">Made with ❤️ by <b>David</b> · <b>Nexuzy Lab</b></p>
