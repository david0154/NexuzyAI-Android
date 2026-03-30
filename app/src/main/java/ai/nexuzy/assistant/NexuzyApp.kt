package ai.nexuzy.assistant

import android.app.Application
import com.google.android.gms.ads.MobileAds

class NexuzyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
