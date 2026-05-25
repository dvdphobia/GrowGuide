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
 * Register screen for creating a new Firebase user account.
 * Validates that passwords match before calling Firebase.
 * On success, navigates directly to MainActivity.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)
        val loginLink = findViewById<android.widget.TextView>(R.id.loginLink)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that password and confirm password match before hitting Firebase
            if (password != confirmPassword) {
                Toast.makeText(this, R.string.passwords_must_match, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading spinner during registration
            progressBar.visibility = android.view.View.VISIBLE
            registerButton.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = android.view.View.GONE
                    registerButton.isEnabled = true

                    if (task.isSuccessful) {
                        // Registration success - go to plant list
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: getString(R.string.auth_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // "Already have an account?" link → navigate to Login
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
