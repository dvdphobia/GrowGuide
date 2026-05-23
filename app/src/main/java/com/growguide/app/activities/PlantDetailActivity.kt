package com.growguide.app.activities

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.growguide.app.R
import com.growguide.app.adapters.ChatAdapter
import com.growguide.app.adapters.LogAdapter
import com.growguide.app.models.ChatMessage
import com.growguide.app.models.LogEntry
import com.growguide.app.network.OllamaApiClient
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detail screen for a single plant. Sections:
 * 1. Plant Info - displays photo, name, type, date, watering status, and notes
 * 2. Growth Log - add, edit, and delete log entries
 * 3. AI Chat - converse with Ollama AI about this plant with context
 */
class PlantDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var plantId: String = ""
    private var plantName: String = ""
    private var plantType: String = ""
    private var plantNotes: String = ""
    private var plantPhotoUrl: String = ""
    private var plantWateringFreq: Int = 0
    private var plantLastWateredSeconds: Long = 0L
    private var logListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null

    // Log section
    private lateinit var logAdapter: LogAdapter
    private val logList = mutableListOf<LogEntry>()

    // Chat section
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()

    // UI references for updates
    private lateinit var nameText: TextView
    private lateinit var typeText: TextView
    private lateinit var notesText: TextView
    private lateinit var wateringStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        plantId = intent.getStringExtra("PLANT_ID") ?: ""
        plantName = intent.getStringExtra("PLANT_NAME") ?: ""
        plantType = intent.getStringExtra("PLANT_TYPE") ?: ""
        plantNotes = intent.getStringExtra("PLANT_NOTES") ?: ""
        plantPhotoUrl = intent.getStringExtra("PLANT_PHOTO_URL") ?: ""
        plantLastWateredSeconds = intent.getLongExtra("PLANT_LAST_WATERED", 0L)
        plantWateringFreq = intent.getIntExtra("PLANT_WATERING_FREQ", 0)

        supportActionBar?.title = plantName

        // --- Section 1: Plant Info ---
        val photoImage = findViewById<ImageView>(R.id.plantDetailPhoto)
        if (plantPhotoUrl.isNotBlank() && plantPhotoUrl.startsWith("/")) {
            val file = java.io.File(plantPhotoUrl)
            if (file.exists()) {
                photoImage.setImageURI(Uri.fromFile(file))
            }
        }

        nameText = findViewById(R.id.plantNameText)
        typeText = findViewById(R.id.plantTypeText)
        notesText = findViewById(R.id.plantNotesText)
        wateringStatusText = findViewById(R.id.wateringStatusText)
        nameText.text = plantName
        typeText.text = "Type: $plantType"
        notesText.text = plantNotes
        updateWateringStatus()

        findViewById<Button>(R.id.waterNowButton).setOnClickListener {
            waterPlantNow()
        }

        findViewById<Button>(R.id.editPlantButton).setOnClickListener {
            showEditPlantDialog()
        }
        findViewById<Button>(R.id.deletePlantButton).setOnClickListener {
            showDeletePlantDialog()
        }

        // --- Section 2: Growth Log ---
        setupGrowthLog()

        // --- Section 3: AI Chat ---
        setupChat()
    }

    private fun updateWateringStatus() {
        if (plantWateringFreq <= 0) {
            wateringStatusText.text = "No watering schedule"
            return
        }

        val lastWatered = if (plantLastWateredSeconds > 0) {
            Date(plantLastWateredSeconds * 1000)
        } else null

        if (lastWatered == null) {
            wateringStatusText.text = "Never watered"
            return
        }

        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        val daysSince = ((System.currentTimeMillis() - lastWatered.time) / 86400000).toInt()
        val daysUntil = plantWateringFreq - daysSince

        wateringStatusText.text = when {
            daysUntil < 0 -> "Overdue by ${-daysUntil} day(s)!"
            daysUntil == 0 -> "Water today"
            else -> "Next watering in $daysUntil day(s)"
        }
    }

    private fun waterPlantNow() {
        val userId = auth.currentUser?.uid ?: return
        val now = Timestamp(Date())
        db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .update("lastWatered", now)
            .addOnSuccessListener {
                plantLastWateredSeconds = now.seconds
                updateWateringStatus()
                Toast.makeText(this, "Plant watered!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ──────────────────────────────────────────────
    //  Growth Log Setup
    // ──────────────────────────────────────────────

    private fun setupGrowthLog() {
        val logRecyclerView = findViewById<RecyclerView>(R.id.logRecyclerView)
        val logEntryEditText = findViewById<EditText>(R.id.logEntryEditText)
        val addLogButton = findViewById<android.widget.Button>(R.id.addLogButton)
        val noLogsText = findViewById<TextView>(R.id.noLogsText)

        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(logList,
            onEdit = { log -> showEditLogDialog(log) },
            onDelete = { log -> showDeleteLogDialog(log) }
        )
        logRecyclerView.adapter = logAdapter

        addLogButton.setOnClickListener {
            val entry = logEntryEditText.text.toString().trim()
            if (entry.isEmpty()) {
                Toast.makeText(this, "Enter a log entry first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val logData = hashMapOf(
                "entry" to entry,
                "createdAt" to Timestamp(Date())
            )

            db.collection("users").document(userId)
                .collection("plants").document(plantId)
                .collection("logs")
                .add(logData)
                .addOnSuccessListener {
                    logEntryEditText.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        val userId = auth.currentUser?.uid ?: return
        logListener = db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .collection("logs")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                logList.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val log = doc.toObject(LogEntry::class.java)
                        if (log != null) {
                            log.id = doc.id
                            logList.add(log)
                        }
                    }
                }
                logAdapter.notifyDataSetChanged()
                noLogsText.visibility = if (logList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun showEditLogDialog(log: LogEntry) {
        val editText = EditText(this)
        editText.setText(log.entry)

        AlertDialog.Builder(this)
            .setTitle("Edit Log Entry")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isEmpty()) return@setPositiveButton
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .collection("plants").document(plantId)
                    .collection("logs").document(log.id)
                    .update("entry", newText)
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteLogDialog(log: LogEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Log Entry")
            .setMessage("Delete this log entry?")
            .setPositiveButton("Delete") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .collection("plants").document(plantId)
                    .collection("logs").document(log.id)
                    .delete()
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ──────────────────────────────────────────────
    //  AI Chat Setup
    // ──────────────────────────────────────────────

    private lateinit var chatInputEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatProgressBar: android.widget.ProgressBar

    private fun setupChat() {
        val chatRecyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatInputEditText = findViewById(R.id.chatInputEditText)
        sendButton = findViewById(R.id.sendMessageButton)
        chatProgressBar = findViewById(R.id.chatProgressBar)
        val chatWelcomeText = findViewById<TextView>(R.id.chatWelcomeText)
        val chatSuggestionsScroll = findViewById<View>(R.id.chatSuggestionsScroll)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(chatList)
        chatRecyclerView.adapter = chatAdapter

        sendButton.setOnClickListener {
            val message = chatInputEditText.text.toString().trim()
            if (message.isNotEmpty()) sendChatMessage(message)
        }

        findViewById<TextView>(R.id.chipCare).setOnClickListener {
            sendChatMessage("How do I care for this plant?")
        }
        findViewById<TextView>(R.id.chipWater).setOnClickListener {
            sendChatMessage("When should I water it?")
        }
        findViewById<TextView>(R.id.chipSunlight).setOnClickListener {
            sendChatMessage("How much sunlight does it need?")
        }
        findViewById<TextView>(R.id.chipProblems).setOnClickListener {
            sendChatMessage("What are common problems?")
        }

        val userId = auth.currentUser?.uid ?: return
        chatListener = db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .collection("chats")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                chatList.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val msg = doc.toObject(ChatMessage::class.java)
                        if (msg != null) chatList.add(msg)
                    }
                }
                chatAdapter.notifyDataSetChanged()

                val hasMessages = chatList.isNotEmpty()
                chatWelcomeText.visibility = if (hasMessages) View.GONE else View.VISIBLE
                chatSuggestionsScroll.visibility = if (hasMessages) View.GONE else View.VISIBLE

                if (hasMessages) {
                    chatRecyclerView.smoothScrollToPosition(chatList.size - 1)
                }
            }
    }

    private fun sendChatMessage(message: String) {
        val userId = auth.currentUser?.uid ?: return

        val userMessage = hashMapOf(
            "role" to "user",
            "content" to message,
            "createdAt" to Timestamp(Date())
        )

        db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .collection("chats")
            .add(userMessage)
            .addOnSuccessListener {
                chatInputEditText.text.clear()
                chatProgressBar.visibility = View.VISIBLE
                sendButton.isEnabled = false

                OllamaApiClient.sendMessage(
                    userMessage = message,
                    plantName = plantName,
                    plantType = plantType,
                    plantNotes = plantNotes
                ) { reply ->
                    runOnUiThread {
                        chatProgressBar.visibility = View.GONE
                        sendButton.isEnabled = true

                        val aiMessage = hashMapOf(
                            "role" to "assistant",
                            "content" to reply,
                            "createdAt" to Timestamp(Date())
                        )

                        db.collection("users").document(userId)
                            .collection("plants").document(plantId)
                            .collection("chats")
                            .add(aiMessage)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    override fun onStop() {
        super.onStop()
        logListener?.remove()
        chatListener?.remove()
    }

    // ──────────────────────────────────────────────
    //  Update / Delete Plant
    // ──────────────────────────────────────────────

    private fun showEditPlantDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_edit_plant, null)
        val nameEdit = layout.findViewById<TextInputEditText>(R.id.editPlantName)
        val typeEdit = layout.findViewById<TextInputEditText>(R.id.editPlantType)
        val notesEdit = layout.findViewById<TextInputEditText>(R.id.editPlantNotes)

        nameEdit.setText(nameText.text)
        typeEdit.setText(typeText.text.removePrefix("Type: "))
        notesEdit.setText(notesText.text)

        AlertDialog.Builder(this)
            .setTitle("Edit Plant")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEdit.text.toString().trim()
                val newType = typeEdit.text.toString().trim()
                val newNotes = notesEdit.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Plant name is required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updatePlant(newName, newType, newNotes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePlant(name: String, type: String, notes: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .update(
                mapOf(
                    "name" to name,
                    "type" to type,
                    "notes" to notes
                )
            )
            .addOnSuccessListener {
                plantName = name
                plantType = type
                plantNotes = notes
                nameText.text = name
                typeText.text = "Type: $type"
                notesText.text = notes
                supportActionBar?.title = name
                Toast.makeText(this, "Plant updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeletePlantDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Plant")
            .setMessage("Are you sure you want to delete this plant and all its logs and chat history?")
            .setPositiveButton("Delete") { _, _ -> deletePlant() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePlant() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Plant deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
