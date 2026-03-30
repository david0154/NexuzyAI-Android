package ai.david.ai

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ai.david.ai.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            val v = packageManager.getPackageInfo(packageName, 0).versionName
            binding.versionText.text = "Version $v"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.versionText.text = "Version 1.0.0"
        }
        binding.backBtn.setOnClickListener { finish() }
        binding.emailNexuzy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:nexuzylab@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "David AI Support")
            })
        }
        binding.emailDavid.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:davidk76011@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "David AI Support")
            })
        }
        binding.privacyPolicyBtn.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
        binding.githubBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/david0154/NexuzyAI-Android")))
        }
    }
}
