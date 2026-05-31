package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.FinanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, FinanceWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            val intent = Intent(context, FinanceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_finance)

            // Setup click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_balance, pendingIntent)

            // Fetch DB calculations off-thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FinanceDatabase.getDatabase(context)
                    val txs = db.financeDao().getAllTransactions().first()
                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                    val monthTxs = txs.filter { it.date.startsWith(currentMonth) }
                    val income = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
                    val expense = monthTxs.filter { it.type == "expense" }.sumOf { it.amount }
                    val balance = income - expense

                    views.setTextViewText(R.id.widget_balance, String.format("Rs. %.2f", balance))
                    views.setTextViewText(R.id.widget_income, String.format("Rs. %.2f", income))
                    views.setTextViewText(R.id.widget_expense, String.format("Rs. %.2f", expense))

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
