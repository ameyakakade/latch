package com.vinnovateit.autonetconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Intent
import android.widget.Button

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landing_page)

        val getStartedButton = findViewById<Button>(R.id.get_started_button)
        getStartedButton.setOnClickListener {
            val intent = Intent(this, NewPageActivity::class.java)
            startActivity(intent)
        }
    }
}