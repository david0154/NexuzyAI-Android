package ai.nexuzy.assistant

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ai.nexuzy.assistant.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Version name from manifest
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            binding.versionText.text = "Version $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.versionText.text = "Version 1.0"
        }

        binding.backBtn.setOnClickListener { finish() }

        // Email: Nexuzy Lab
        binding.emailNexuzy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:nexuzylab@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "NexuzyAI Support")
            })
        }

        // Email: David
        binding.emailDavid.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:davidk76011@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "NexuzyAI Support")
            })
        }

        // Privacy Policy
        binding.privacyPolicyBtn.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        // GitHub
        binding.githubBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/david0154/NexuzyAI-Android")))
        }
    }
}
