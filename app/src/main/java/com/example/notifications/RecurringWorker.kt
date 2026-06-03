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

                when (tx.recurrenceFrequency?.lowercase()) {
                    "daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "monthly" -> cal.add(Calendar.MONTH, 1)
                    else -> continue
                }

                // If next trigger date is today or in the past, trigger it!
                if (!cal.time.after(today)) {
                    // Create copy for today
                    val newTx = Transaction(
                        type = tx.type,
                        amount = tx.amount,
                        category = tx.category,
                        date = todayStr,
                        note = tx.note ?: "Recurring transaction auto-log",
                        imagePath = null, // don't copy receipt photo
                        receiverName = tx.receiverName,
                        receiverId = tx.receiverId,
                        remarks = tx.remarks,
                        paymentMethod = tx.paymentMethod,
                        transactionCode = null, // unique per transaction
                        processedBy = tx.processedBy,
                        purpose = tx.purpose,
                        initiatorName = tx.initiatorName,
                        isRecurring = false, // the logged one is standard
                        recurrenceFrequency = null,
                        accountId = tx.accountId
                    )
                    dao.insertTransaction(newTx)

                    // Update parent transaction's date to today to reset the cycle
                    val updatedParent = tx.copy(date = todayStr)
                    dao.updateTransaction(updatedParent)
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
