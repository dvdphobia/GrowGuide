package com.growguide.app.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.growguide.app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays the current user's profile information from FirebaseAuth.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val user = auth.currentUser ?: return

        findViewById<TextView>(R.id.profileEmail).text = user.email ?: "No email"
        findViewById<TextView>(R.id.profileDisplayName).text = user.displayName ?: "No display name"
        findViewById<TextView>(R.id.profileUid).text = user.uid

        val metadata = user.metadata
        if (metadata != null) {
            val creationDate = Date(metadata.creationTimestamp)
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            findViewById<TextView>(R.id.profileCreatedAt).text = formatter.format(creationDate)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
