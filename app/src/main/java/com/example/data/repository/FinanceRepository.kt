package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.model.AppSetting
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.io.File

class FinanceRepository(private val financeDao: FinanceDao) {

    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    suspend fun getTransactionById(id: Int): Transaction? {
        return financeDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        val transaction = financeDao.getTransactionById(id)
        if (transaction != null) {
            // Delete associated file if it exists
            transaction.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            financeDao.deleteTransaction(transaction)
        }
    }

    fun getBudgetsForMonth(month: String): Flow<List<Budget>> {
        return financeDao.getBudgetsForMonth(month)
    }

    suspend fun getBudgetsForMonthSuspend(month: String): List<Budget> {
        return financeDao.getBudgetsForMonthSuspend(month)
    }

    suspend fun insertBudget(budget: Budget) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudgetById(id: Int) {
        financeDao.deleteBudgetById(id)
    }

    fun getSavingsGoalForMonth(month: String): Flow<SavingsGoal?> {
        return financeDao.getSavingsGoalForMonth(month)
    }

    suspend fun getSavingsGoalForMonthSuspend(month: String): SavingsGoal? {
        return financeDao.getSavingsGoalForMonthSuspend(month)
    }

    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal) {
        financeDao.insertSavingsGoal(savingsGoal)
    }

    // ── Bulk-import helpers (used by JSON restore) ────────────────────────────
    suspend fun insertTransactions(transactions: List<com.example.data.model.Transaction>) {
        financeDao.insertTransactions(transactions)
    }

    suspend fun insertBudgets(budgets: List<Budget>) {
        financeDao.insertBudgets(budgets)
    }

    suspend fun insertSavingsGoals(savingsGoals: List<SavingsGoal>) {
        financeDao.insertSavingsGoals(savingsGoals)
    }

    fun getSettingFlow(key: String): Flow<AppSetting?> {
        return financeDao.getSettingFlow(key)
    }

    suspend fun getSetting(key: String): String? {
        return financeDao.getSetting(key)?.value
    }

    suspend fun updateSetting(key: String, value: String) {
        financeDao.insertSetting(AppSetting(key, value))
    }
}
