package com.growguide.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.growguide.app.R
import com.growguide.app.activities.MainActivity
import java.util.Date

/**
 * BroadcastReceiver that fires watering reminder notifications.
 * Triggered by AlarmManager at scheduled intervals per plant.
 * Supports a "Mark as Watered" action that updates Firestore and reschedules.
 */
class WateringReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "watering_reminders"
        const val EXTRA_PLANT_NAME = "plant_name"
        const val EXTRA_PLANT_ID = "plant_id"
        const val EXTRA_WATERING_FREQUENCY = "watering_frequency"
        const val ACTION_MARK_WATERED = "com.growguide.app.ACTION_MARK_WATERED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME) ?: "Your plant"
        val plantId = intent.getStringExtra(EXTRA_PLANT_ID) ?: ""
        val wateringFrequency = intent.getIntExtra(EXTRA_WATERING_FREQUENCY, 0)

        if (intent.action == ACTION_MARK_WATERED) {
            markAsWatered(context, plantId, plantName, wateringFrequency)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(plantId.hashCode())
            return
        }

        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, plantId.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markWateredIntent = Intent(context, WateringReminderReceiver::class.java).apply {
            action = ACTION_MARK_WATERED
            putExtra(EXTRA_PLANT_ID, plantId)
            putExtra(EXTRA_PLANT_NAME, plantName)
            putExtra(EXTRA_WATERING_FREQUENCY, wateringFrequency)
        }
        val markWateredPendingIntent = PendingIntent.getBroadcast(
            context, plantId.hashCode() + 1, markWateredIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("Watering Reminder")
            .setContentText("Time to water $plantName!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Mark as Watered",
                markWateredPendingIntent
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(plantId.hashCode(), notification)
    }

    private fun markAsWatered(context: Context, plantId: String, plantName: String, frequency: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = Timestamp(Date())
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("plants").document(plantId)
            .update("lastWatered", now)
            .addOnSuccessListener {
                ReminderScheduler.scheduleWateringReminder(
                    context, plantId, plantName, now, frequency
                )
            }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watering Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you when it's time to water your plants"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
