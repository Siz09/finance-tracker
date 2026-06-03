package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {

    // Distinct action strings ensure each PendingIntent is unique and never collides.
    const val ACTION_DAILY_REMINDER = "com.example.ACTION_DAILY_REMINDER"
    const val ACTION_DIGEST_REMINDER = "com.example.ACTION_DIGEST_REMINDER"

    // Request code 1 — user-configured daily reminder
    private const val REQUEST_CODE_DAILY  = 1
    // Request code 2 — fixed 10 pm digest notification
    private const val REQUEST_CODE_DIGEST = 2

    /** Schedules the user-configured daily reminder at [hour]:[minute]. */
    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_DAILY, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    /** Schedules the fixed 10 pm spending-digest notification. */
    fun scheduleDigestNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DIGEST_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_DIGEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22) // 10 pm
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun cancelNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel daily reminder
        val dailyIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_DAILY, dailyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Cancel digest reminder
        val digestIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DIGEST_REMINDER
        }
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_DIGEST, digestIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Inexact alarm — may be delayed by the OS but doesn't require SCHEDULE_EXACT_ALARM permission
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
