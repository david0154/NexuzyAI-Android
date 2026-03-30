package ai.nexuzy.assistant

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import ai.nexuzy.assistant.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    companion object {
        private const val PREFS_NAME  = "nexuzy_prefs"
        private const val KEY_FIRST   = "first_launch_done"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs       = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val firstDone   = prefs.getBoolean(KEY_FIRST, false)

            if (!firstDone) {
                // First install — show first-launch setup screen
                startActivity(Intent(this, FirstLaunchActivity::class.java))
            } else {
                // Returning user — go straight to chat
                startActivity(Intent(this, ChatActivity::class.java))
            }
            finish()
        }, 1800)
    }
}
