package com.example.act_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay the transition to the WifiScanActivity
        Handler().postDelayed({
            startActivity(Intent(this, WifiScanActivity::class.java))
            finish()
        }, 2000) // 2 seconds delay
    }
}
