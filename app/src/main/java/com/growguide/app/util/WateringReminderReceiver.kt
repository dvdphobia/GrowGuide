package com.growguide.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.growguide.app.R
import com.growguide.app.activities.MainActivity

/**
 * BroadcastReceiver that fires watering reminder notifications.
 * Triggered by AlarmManager at scheduled intervals per plant.
 */
class WateringReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "watering_reminders"
        const val EXTRA_PLANT_NAME = "plant_name"
        const val EXTRA_PLANT_ID = "plant_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME) ?: "Your plant"
        val plantId = intent.getStringExtra(EXTRA_PLANT_ID) ?: ""

        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, plantId.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("Watering Reminder")
            .setContentText("Time to water $plantName!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(plantId.hashCode(), notification)
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
