package com.growguide.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.growguide.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Login screen using Firebase email & password authentication.
 * On success, navigates to MainActivity.
 * Provides a link to the Register screen for new users.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val registerLink = findViewById<android.widget.TextView>(R.id.registerLink)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        // Login button click - authenticate with Firebase
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading spinner while Firebase processes the login
            progressBar.visibility = android.view.View.VISIBLE
            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = android.view.View.GONE
                    loginButton.isEnabled = true

                    if (task.isSuccessful) {
                        // Auth succeeded, go to plant list
                        startActivity(Intent(this, MainActivity::class.java))
                        finish() // Don't let user press back to login
                    } else {
                        // Show the Firebase error message
                        Toast.makeText(
                            this,
                            task.exception?.message ?: getString(R.string.auth_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // "Don't have an account?" link → navigate to Register screen
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
