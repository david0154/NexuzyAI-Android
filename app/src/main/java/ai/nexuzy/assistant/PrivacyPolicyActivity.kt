package ai.nexuzy.assistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ai.nexuzy.assistant.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backBtn.setOnClickListener { finish() }
    }
}
