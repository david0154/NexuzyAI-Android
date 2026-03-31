package ai.nexuzy.assistant.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import java.util.Calendar

/**
 * DeviceControlTool: Handles ALL device actions.
 * Everything here works 100% OFFLINE — no internet, no MLC needed.
 *
 * Features:
 *   ⏰ Alarms          ⏱ Timers           🔦 Flashlight
 *   🎵 Media control   🔊 Volume           ☀️ Brightness
 *   🔋 Battery info     📡 WiFi info        🚀 Open apps
 *   📞 Phone calls      💬 Send SMS         📤 Share text
 *   🌐 Translate        
 */
class DeviceControlTool(private val context: Context) {

    // ── OPEN APP ─────────────────────────────────────────────────────
    fun openApp(userInput: String): String {
        val appName = userInput.lowercase()
            .replace("open", "").replace("launch", "")
            .replace("start app", "").replace("go to", "").trim()
        val pkg = resolvePackageName(appName)
            ?: return "🔍 Could not find app: \"$appName\". Try: open YouTube, open WhatsApp"
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return "🔍 App not installed: $appName"
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "🚀 Opening $appName"
    }

    fun resolvePackageName(app: String): String? = mapOf(
        "youtube"       to "com.google.android.youtube",
        "whatsapp"      to "com.whatsapp",
        "instagram"     to "com.instagram.android",
        "chrome"        to "com.android.chrome",
        "camera"        to "com.android.camera2",
        "maps"          to "com.google.android.apps.maps",
        "google maps"   to "com.google.android.apps.maps",
        "spotify"       to "com.spotify.music",
        "twitter"       to "com.twitter.android",
        "x"             to "com.twitter.android",
        "facebook"      to "com.facebook.katana",
        "gmail"         to "com.google.android.gm",
        "settings"      to "com.android.settings",
        "telegram"      to "org.telegram.messenger",
        "snapchat"      to "com.snapchat.android",
        "tiktok"        to "com.zhiliaoapp.musically",
        "netflix"       to "com.netflix.mediaclient",
        "amazon"        to "com.amazon.mShop.android.shopping",
        "flipkart"      to "com.flipkart.android",
        "paytm"         to "net.one97.paytm",
        "phonepe"       to "com.phonepe.app",
        "gpay"          to "com.google.android.apps.nbu.paisa.user",
        "google pay"    to "com.google.android.apps.nbu.paisa.user",
        "calculator"    to "com.google.android.calculator",
        "calendar"      to "com.google.android.calendar",
        "clock"         to "com.google.android.deskclock",
        "contacts"      to "com.google.android.contacts",
        "photos"        to "com.google.android.apps.photos",
        "drive"         to "com.google.android.apps.docs",
        "maps"          to "com.google.android.apps.maps",
        "meet"          to "com.google.android.apps.tachyon",
        "zoom"          to "us.zoom.videomeetings",
        "linkedin"      to "com.linkedin.android",
        "reddit"        to "com.reddit.frontpage",
        "discord"       to "com.discord"
    )[app.trim()]

    // ── ALARM ───────────────────────────────────────────────────────
    fun setAlarm(userInput: String): String {
        val time = extractHour(userInput)
            ?: return "⚠️ Please say a time. Example: \"Set alarm at 7:30 AM\""
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, time.first)
            putExtra(AlarmClock.EXTRA_MINUTES, time.second)
            putExtra(AlarmClock.EXTRA_MESSAGE, "NexuzyAI Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "⏰ Alarm set for ${formatTime(time.first, time.second)}"
    }

    // ── TIMER ───────────────────────────────────────────────────────
    fun setTimer(userInput: String): String {
        val seconds = extractTimerSeconds(userInput)
            ?: return "⚠️ Please say duration. Example: \"Set timer for 5 minutes\""
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val label = formatDuration(seconds)
        return "⏱️ Timer set for $label"
    }

    private fun extractTimerSeconds(input: String): Int? {
        val lower = input.lowercase()
        var total = 0
        val hrMatch  = Regex("(\\d+)\\s*(?:hour|hr|h)").find(lower)
        val minMatch = Regex("(\\d+)\\s*(?:minute|min|m)").find(lower)
        val secMatch = Regex("(\\d+)\\s*(?:second|sec|s)").find(lower)
        hrMatch?.groupValues?.get(1)?.toIntOrNull()?.let  { total += it * 3600 }
        minMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 60   }
        secMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it        }
        // fallback: plain number → assume minutes
        if (total == 0) Regex("(\\d+)").find(lower)?.groupValues?.get(1)?.toIntOrNull()?.let {
            total = it * 60
        }
        return if (total > 0) total else null
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return listOfNotNull(
            if (h > 0) "${h}h" else null,
            if (m > 0) "${m}m" else null,
            if (s > 0) "${s}s" else null
        ).joinToString(" ").ifBlank { "${seconds}s" }
    }

    // ── FLASHLIGHT ──────────────────────────────────────────────────
    fun toggleFlashlight(on: Boolean): String {
        return try {
            val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id  = cam.cameraIdList.firstOrNull() ?: return "❌ No camera found"
            cam.setTorchMode(id, on)
            if (on) "🔦 Flashlight ON" else "🔦 Flashlight OFF"
        } catch (e: Exception) {
            "❌ Could not toggle flashlight: ${e.message}"
        }
    }

    // ── MEDIA CONTROL ──────────────────────────────────────────────
    fun controlMedia(keyCode: Int): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY      -> "▶️ Playing"
            KeyEvent.KEYCODE_MEDIA_PAUSE     -> "⏸️ Paused"
            KeyEvent.KEYCODE_MEDIA_NEXT      -> "⏭️ Next track"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS  -> "⏮️ Previous track"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE-> "⏯️ Play/Pause toggled"
            else                             -> "⏯️ Media key sent"
        }
    }

    // ── VOLUME ───────────────────────────────────────────────────────
    fun volumeUp(): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return "🔊 Volume raised: $cur / $max"
    }

    fun volumeDown(): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return "🔉 Volume lowered: $cur / $max"
    }

    fun muteVolume(): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return "🔇 Volume muted"
    }

    // ── BRIGHTNESS ─────────────────────────────────────────────────
    fun brightnessUp(): String {
        return try {
            val current = Settings.System.getInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val newVal = (current + 30).coerceAtMost(255)
            Settings.System.putInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
            "☀️ Brightness increased: ${(newVal * 100 / 255)}%"
        } catch (e: Exception) {
            "⚠️ Cannot change brightness — Write Settings permission needed.\n" +
            "Go to Settings → Apps → NexuzyAI → Modify system settings → Allow"
        }
    }

    fun brightnessDown(): String {
        return try {
            val current = Settings.System.getInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val newVal = (current - 30).coerceAtLeast(10)
            Settings.System.putInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
            "🌙 Brightness decreased: ${(newVal * 100 / 255)}%"
        } catch (e: Exception) {
            "⚠️ Cannot change brightness — Write Settings permission needed."
        }
    }

    // ── BATTERY ──────────────────────────────────────────────────────
    fun getBatteryInfo(): String {
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level   = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale   = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status  = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct     = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL
        val emoji = when {
            pct >= 80 -> "🔋"
            pct >= 50 -> "🔋"
            pct >= 20 -> "🪫"
            else      -> "🪫"
        }
        return if (pct >= 0)
            "$emoji Battery: $pct% — ${if (charging) "⚡ Charging" else "🔋 Not charging"}"
        else
            "⚠️ Could not read battery level"
    }

    // ── WIFI INFO ──────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    fun getWifiInfo(): String {
        val wm = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wm.connectionInfo
        val ssid = info?.ssid?.trim('"') ?: ""
        val connected = wm.isWifiEnabled && ssid.isNotBlank() && ssid != "<unknown ssid>"
        return if (connected)
            "📡 Connected to Wi-Fi: \"$ssid\""
        else if (wm.isWifiEnabled)
            "📡 Wi-Fi is ON but not connected to any network"
        else
            "📡 Wi-Fi is OFF"
    }

    // ── PHONE CALL ─────────────────────────────────────────────────
    fun makeCall(userInput: String): String {
        val number = extractPhoneNumber(userInput)
        return if (number != null) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$number")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "📞 Opening dialer for $number"
        } else {
            "⚠️ Please include a phone number. Example: \"Call 9876543210\""
        }
    }

    private fun extractPhoneNumber(input: String): String? {
        return Regex("[+]?[0-9]{7,15}").find(input)?.value
    }

    // ── SEND SMS ───────────────────────────────────────────────────
    fun sendSms(userInput: String): String {
        val number = extractPhoneNumber(userInput)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:${number ?: ""}") 
            putExtra("sms_body", userInput
                .replace(Regex("send (sms|message|text) to [0-9+]+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("message to [0-9+]+", RegexOption.IGNORE_CASE), "")
                .trim()
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "💬 Opening SMS app${if (number != null) " for $number" else ""}"
    }

    // ── SHARE ───────────────────────────────────────────────────────
    fun shareText(text: String): String {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type  = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "📤 Share sheet opened"
    }

    // ── TRANSLATE (opens Google Translate) ──────────────────────────
    fun openTranslate(userInput: String): String {
        val textToTranslate = userInput
            .replace(Regex("translate( this)?( to [a-z]+)?|in [a-z]+|say in [a-z]+",
                RegexOption.IGNORE_CASE), "")
            .trim()
        return try {
            val intent = Intent().apply {
                action  = Intent.ACTION_SEND
                `package` = "com.google.android.apps.translate"
                putExtra(Intent.EXTRA_TEXT, textToTranslate)
                type    = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "🌐 Opening Google Translate"
        } catch (e: Exception) {
            // Google Translate not installed — open in browser
            val encoded = android.net.Uri.encode(textToTranslate)
            val webIntent = Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://translate.google.com/?text=$encoded"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
            "🌐 Opening Google Translate in browser"
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────
    fun extractHour(input: String): Pair<Int, Int>? {
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = regex.find(input.lowercase()) ?: return null
        var hour  = match.groupValues[1].toIntOrNull() ?: return null
        val min   = match.groupValues[2].toIntOrNull() ?: 0
        val ampm  = match.groupValues[3].lowercase()
        if (ampm == "pm" && hour < 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        return Pair(hour, min)
    }

    fun formatTime(h: Int, m: Int): String {
        val ampm = if (h < 12) "AM" else "PM"
        val h12  = if (h % 12 == 0) 12 else h % 12
        return String.format("%d:%02d %s", h12, m, ampm)
    }
}
