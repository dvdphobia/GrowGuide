package com.growguide.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Splash screen that checks Firebase auth state and routes accordingly.
 * - Logged in → MainActivity
 * - Not logged in → LoginActivity
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.growguide.app.R.layout.activity_splash)

        // Short delay so the splash branding is visible before navigating
        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // User is already signed in, go straight to the plant list
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // No active session, redirect to login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Remove splash from back stack
        }, 1500)
    }
}
