package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = FinanceDatabase.getDatabase(context)
            val repo = FinanceRepository(db.financeDao())
            CoroutineScope(Dispatchers.IO).launch {
                val timeStr = repo.getSetting("notification_time") ?: "20:00"
                val parts = timeStr.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                NotificationScheduler.scheduleDailyNotification(context, hour, minute)
            }
            return
        }

        val db = FinanceDatabase.getDatabase(context)
        val repo = FinanceRepository(db.financeDao())

        CoroutineScope(Dispatchers.IO).launch {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todaysTransactions = repo.getTransactionsByDateSync(todayStr)
            
            showNotification(context, todaysTransactions)
            
            // Reschedule alarm for next day
            val timeStr = repo.getSetting("notification_time") ?: "20:00"
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            NotificationScheduler.scheduleDailyNotification(context, hour, minute)
        }
    }

    private fun showNotification(context: Context, transactions: List<com.example.data.model.Transaction>) {
        val channelId = "daily_finance_reminder"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Expense Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log today's financial transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val logIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "add_transaction") // Optional deep link info
        }
        val logPendingIntent = PendingIntent.getActivity(
            context,
            1,
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val logAction = NotificationCompat.Action(
            com.example.R.drawable.ic_launcher_foreground,
            "Log Expense",
            logPendingIntent
        )

        val reviewAction = NotificationCompat.Action(
            com.example.R.drawable.ic_launcher_foreground,
            "Review",
            pendingIntent
        )

        val expenses = transactions.filter { it.type == "expense" }
        val totalSpent = expenses.sumOf { it.amount }
        
        val contentTitle = if (expenses.isNotEmpty()) "Daily Spending Digest" else "Log Today's Expenses"
        val contentText = if (expenses.isNotEmpty()) {
            "You spent Rs. ${String.format("%.2f", totalSpent)} across ${expenses.size} transactions today."
        } else {
            "Don't forget to keep your budget on track by logging today's finances!"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(reviewAction)
            .addAction(logAction)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
