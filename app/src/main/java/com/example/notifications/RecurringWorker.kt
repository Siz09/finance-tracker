package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.FinanceDatabase
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecurringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        try {
            val recurringList = dao.getRecurringTransactions().first()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val today = sdf.parse(todayStr) ?: return Result.failure()

            for (tx in recurringList) {
                val txDate = sdf.parse(tx.date) ?: continue
                val cal = Calendar.getInstance().apply { time = txDate }

                // Advance one period at a time until we are in the future,
                // creating a logged copy for every missed interval.
                var lastDate = tx.date
                var didTrigger = false
                while (true) {
                    when (tx.recurrenceFrequency?.lowercase()) {
                        "daily"   -> cal.add(Calendar.DAY_OF_YEAR, 1)
                        "weekly"  -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "monthly" -> cal.add(Calendar.MONTH, 1)
                        else      -> break
                    }
                    // If the next trigger is still in the past/today, log it
                    if (!cal.time.after(today)) {
                        val triggerDateStr = sdf.format(cal.time)
                        val newTx = Transaction(
                            type = tx.type,
                            amount = tx.amount,
                            category = tx.category,
                            date = triggerDateStr,
                            note = tx.note ?: "Recurring transaction auto-log",
                            imagePath = null,
                            receiverName = tx.receiverName,
                            receiverId = tx.receiverId,
                            remarks = tx.remarks,
                            paymentMethod = tx.paymentMethod,
                            transactionCode = null,
                            processedBy = tx.processedBy,
                            purpose = tx.purpose,
                            initiatorName = tx.initiatorName,
                            isRecurring = false,
                            recurrenceFrequency = null,
                            accountId = tx.accountId
                        )
                        dao.insertTransaction(newTx)
                        lastDate = triggerDateStr
                        didTrigger = true
                    } else {
                        break
                    }
                }

                // Update parent's date to the last triggered date so next run
                // starts from the correct baseline
                if (didTrigger) {
                    dao.updateTransaction(tx.copy(date = lastDate))
                }
            }
            // Budget Rollover Logic
            val calToday = Calendar.getInstance()
            // Check if it's the 1st of the month, but only run once per day (Worker frequency ensures this)
            if (calToday.get(Calendar.DAY_OF_MONTH) == 1) {
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calToday.time)
                val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val lastMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(lastMonthCal.time)
                
                val lastMonthBudgets = dao.getBudgetsForMonthSuspend(lastMonthStr)
                if (lastMonthBudgets.isNotEmpty()) {
                    // Only load last month's transactions — avoids a full table scan (#12)
                    val lastMonthTxs = dao.getTransactionsForMonthSuspend(lastMonthStr)
                    for (b in lastMonthBudgets) {
                        if (b.rolloverEnabled) {
                            val spent = lastMonthTxs.filter { it.category == b.category && it.type == "expense" }.sumOf { it.amount }
                            val underspend = (b.monthlyLimit - spent).coerceAtLeast(0.0)
                            if (underspend > 0) {
                                val existingThisMonth = dao.getBudgetByCategoryAndMonth(b.category, currentMonthStr)
                                if (existingThisMonth == null) {
                                    dao.insertBudget(
                                        com.example.data.model.Budget(
                                            category = b.category,
                                            monthlyLimit = b.monthlyLimit,
                                            month = currentMonthStr,
                                            rolloverAmount = underspend,
                                            rolloverEnabled = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RecurringWorker", "doWork failed", e)
            return Result.failure()
        }
    }
}
