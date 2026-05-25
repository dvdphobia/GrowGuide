package com.growguide.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.firebase.Timestamp

/**
 * Schedules and cancels watering reminder alarms via AlarmManager.
 * Alarms fire at: lastWatered + wateringFrequency days.
 */
object ReminderScheduler {

    private const val EXTRA_PLANT_NAME = "plant_name"
    private const val EXTRA_PLANT_ID = "plant_id"
    private const val EXTRA_WATERING_FREQUENCY = "watering_frequency"

    fun scheduleWateringReminder(
        context: Context,
        plantId: String,
        plantName: String,
        lastWatered: Timestamp?,
        wateringFrequency: Int
    ) {
        if (wateringFrequency <= 0) {
            cancelWateringReminder(context, plantId)
            return
        }

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WateringReminderReceiver::class.java).apply {
            putExtra(EXTRA_PLANT_NAME, plantName)
            putExtra(EXTRA_PLANT_ID, plantId)
            putExtra(EXTRA_WATERING_FREQUENCY, wateringFrequency)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plantId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val baseTime = lastWatered?.toDate()?.time ?: System.currentTimeMillis()
        val triggerAt = baseTime + wateringFrequency * 86_400_000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmMgr.canScheduleExactAlarms()) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelWateringReminder(context: Context, plantId: String) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WateringReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plantId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
