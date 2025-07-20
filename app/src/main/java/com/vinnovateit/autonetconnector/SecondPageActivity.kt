package com.vinnovateit.autonetconnector

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vinnovateit.autonetconnector.functionality2.storage.CredentialDatabase
import com.vinnovateit.autonetconnector.functionality2.storage.CredentialEntity
import kotlinx.coroutines.launch

class SecondPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editMode = intent.getBooleanExtra("editMode", false)
        val db = CredentialDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val existing = db.credentialDao().getCredential()
            if (existing != null && !editMode) {
                // Credentials already exist and not in edit mode, go to MainActivity
                val intent = Intent(this@SecondPageActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Show the UI (for new or edit mode)
                runOnUiThread {
                    setContentView(R.layout.second_page)
                    val saveCredentialsButton = findViewById<Button>(R.id.save_credentials_button)
                    val userIdInput = findViewById<EditText>(R.id.user_id_input)
                    val passwordInput = findViewById<EditText>(R.id.password_input)
                    // Pre-fill fields if editing
                    if (existing != null) {
                        userIdInput.setText(existing.registrationNumber)
                        passwordInput.setText(existing.password)
                    }
                    saveCredentialsButton.setOnClickListener {
                        val regNo = userIdInput.text.toString()
                        val password = passwordInput.text.toString()
                        android.util.Log.d("SecondPageActivity", "Attempting to save credentials: userId=$regNo, password=$password")
                        Toast.makeText(this@SecondPageActivity, "Saving: userId=$regNo, password=$password", Toast.LENGTH_SHORT).show()
                        if (regNo.isNotBlank() && password.isNotBlank()) {
                            lifecycleScope.launch {
                                db.credentialDao().insertCredential(CredentialEntity(regNo, password))
                                runOnUiThread {
                                    Toast.makeText(this@SecondPageActivity, "Credentials saved!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@SecondPageActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        } else {
                            Toast.makeText(this@SecondPageActivity, "Please enter User ID and Password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
} 