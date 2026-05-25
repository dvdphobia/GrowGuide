package com.growguide.app.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.growguide.app.R
import com.growguide.app.network.OllamaApiClient
import com.growguide.app.util.ReminderScheduler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.util.Date

/**
 * Form screen for adding a new plant to the user's collection.
 * Supports photo selection, camera capture, AI identification via Plant.id,
 * watering frequency, and notes.
 */
class AddPlantActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var photoFile: File? = null

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            photoFile = copyUriToLocalFile(uri)
            findViewById<ImageView>(R.id.plantPhotoPreview).setImageURI(uri)
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null && photoFile!!.exists()) {
            findViewById<ImageView>(R.id.plantPhotoPreview).setImageURI(Uri.fromFile(photoFile))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val nameEditText = findViewById<TextInputEditText>(R.id.plantNameEditText)
        val typeEditText = findViewById<TextInputEditText>(R.id.plantTypeEditText)
        val wateringFreqEditText = findViewById<TextInputEditText>(R.id.wateringFreqEditText)
        val notesEditText = findViewById<TextInputEditText>(R.id.notesEditText)
        val saveButton = findViewById<MaterialButton>(R.id.savePlantButton)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val pickPhotoButton = findViewById<MaterialButton>(R.id.pickPhotoButton)
        val takePhotoButton = findViewById<MaterialButton>(R.id.takePhotoButton)
        val identifyButton = findViewById<MaterialButton>(R.id.identifyButton)

        pickPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickPhotoLauncher.launch(intent)
        }

        takePhotoButton.setOnClickListener {
            launchCamera()
        }

        identifyButton.setOnClickListener {
            val file = photoFile
            if (file == null || !file.exists()) {
                Toast.makeText(this, R.string.identify_no_photo, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            identifyButton.isEnabled = false
            saveButton.isEnabled = false

            OllamaApiClient.identifyPlant(file) { result ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    identifyButton.isEnabled = true
                    saveButton.isEnabled = true

                    if (result == null) {
                        Toast.makeText(this, R.string.identify_failed, Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    nameEditText.setText(result.name)
                    if (result.commonName.isNotBlank()) {
                        typeEditText.setText(result.commonName)
                    }
                    if (result.description.isNotBlank() && notesEditText.text.isNullOrBlank()) {
                        notesEditText.setText(result.description)
                    }
                    Toast.makeText(
                        this,
                        getString(R.string.identify_success, result.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val type = typeEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()
            val freqText = wateringFreqEditText.text.toString().trim()
            val wateringFrequency = freqText.toIntOrNull() ?: 0

            if (name.isEmpty()) {
                Toast.makeText(this, "Plant name is required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            saveButton.isEnabled = false

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val photoPath = if (photoFile != null && photoFile!!.exists()) photoFile!!.absolutePath else ""
            savePlant(userId, name, type, notes, wateringFrequency, photoPath, progressBar, saveButton)
        }
    }

    private fun launchCamera() {
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        val file = File(photosDir, "camera_${System.currentTimeMillis()}.jpg")
        photoFile = file
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        takePhotoLauncher.launch(uri)
    }

    /**
     * Copies the selected image URI to a temporary file the app owns.
     * This avoids URI permission issues when uploading to Firebase Storage.
     */
    private fun copyUriToLocalFile(uri: Uri): File {
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        val localFile = File(photosDir, "plant_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }
        return localFile
    }

    private fun savePlant(
        userId: String, name: String, type: String, notes: String,
        wateringFrequency: Int, photoUrl: String,
        progressBar: android.widget.ProgressBar, saveButton: MaterialButton
    ) {
        val plantData = hashMapOf(
            "name" to name,
            "type" to type,
            "notes" to notes,
            "createdAt" to Timestamp(Date()),
            "wateringFrequency" to wateringFrequency,
            "photoUrl" to photoUrl
        )

        db.collection("users")
            .document(userId)
            .collection("plants")
            .add(plantData)
            .addOnSuccessListener { docRef ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, R.string.plant_saved, Toast.LENGTH_SHORT).show()
                ReminderScheduler.scheduleWateringReminder(
                    this,
                    docRef.id,
                    name,
                    null,
                    wateringFrequency
                )
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
