package com.vinnovateit.autonetconnector

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vinnovateit.autonetconnector.functionality2.storage.CredentialDatabase
import kotlinx.coroutines.launch

class LandingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = CredentialDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val existing = db.credentialDao().getCredential()
            if (existing != null) {
                // Credentials exist, go straight to MainActivity
                val intent = Intent(this@LandingPageActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // No credentials, show landing page UI
                runOnUiThread {
                    setContentView(R.layout.landing_page)
                    val getStartedButton = findViewById<Button>(R.id.get_started_button)
                    getStartedButton.setOnClickListener {
                        val intent = Intent(this@LandingPageActivity, SecondPageActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }
} 