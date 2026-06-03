package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.model.Account
import com.example.data.model.AppSetting
import com.example.data.model.Budget
import com.example.data.model.DebtItem
import com.example.data.model.NetWorthItem
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.io.File

class FinanceRepository(private val financeDao: FinanceDao) {

    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    suspend fun getTransactionById(id: Int): Transaction? {
        return financeDao.getTransactionById(id)
    }

    suspend fun getTransactionsByDateSync(date: String): List<Transaction> {
        return financeDao.getTransactionsByDateSync(date)
    }

    // Efficient month-scoped query — avoids a full table scan during budget rollover checks.
    suspend fun getTransactionsForMonthSuspend(monthPrefix: String): List<Transaction> {
        return financeDao.getTransactionsForMonthSuspend(monthPrefix)
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

    suspend fun getBudgetByCategoryAndMonth(category: String, month: String): Budget? {
        return financeDao.getBudgetByCategoryAndMonth(category, month)
    }

    suspend fun insertBudget(budget: Budget) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudgetById(id: Int) {
        financeDao.deleteBudgetById(id)
    }

    val allSavingsGoals: Flow<List<SavingsGoal>> = financeDao.getAllSavingsGoals()

    fun getSavingsGoalForMonth(month: String): Flow<SavingsGoal?> {
        return financeDao.getSavingsGoalForMonth(month)
    }

    suspend fun getSavingsGoalForMonthSuspend(month: String): SavingsGoal? {
        return financeDao.getSavingsGoalForMonthSuspend(month)
    }

    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal) {
        financeDao.insertSavingsGoal(savingsGoal)
    }

    suspend fun deleteSavingsGoalById(id: Int) {
        financeDao.deleteSavingsGoalById(id)
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

    // ── Recurring Transactions ────────────────────────────────────────────────
    fun getRecurringTransactions(): Flow<List<Transaction>> {
        return financeDao.getRecurringTransactions()
    }

    // ── Accounts / Wallets ────────────────────────────────────────────────────
    val allAccounts: Flow<List<Account>> = financeDao.getAllAccounts()

    suspend fun getAccountById(id: Int): Account? {
        return financeDao.getAccountById(id)
    }

    suspend fun insertAccount(account: Account) {
        financeDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        financeDao.updateAccount(account)
    }

    suspend fun deleteAccountById(id: Int) {
        financeDao.deleteAccountById(id)
    }

    // Net Worth
    val allNetWorthItems: Flow<List<NetWorthItem>> = financeDao.getAllNetWorthItems()

    suspend fun insertNetWorthItem(item: NetWorthItem) {
        financeDao.insertNetWorthItem(item)
    }

    suspend fun deleteNetWorthItemById(id: Int) {
        financeDao.deleteNetWorthItemById(id)
    }

    // Debt Payoff Tracker
    val allDebtItems: Flow<List<DebtItem>> = financeDao.getAllDebtItems()

    suspend fun insertDebtItem(item: DebtItem) {
        financeDao.insertDebtItem(item)
    }

    suspend fun updateDebtItem(item: DebtItem) {
        financeDao.updateDebtItem(item)
    }

    suspend fun deleteDebtItemById(id: Int) {
        financeDao.deleteDebtItemById(id)
    }

    // ── Transaction Templates ────────────────────────────────────────────────
    val allTransactionTemplates: Flow<List<com.example.data.model.TransactionTemplate>> = financeDao.getAllTransactionTemplates()

    suspend fun insertTransactionTemplate(template: com.example.data.model.TransactionTemplate) {
        financeDao.insertTransactionTemplate(template)
    }

    suspend fun deleteTransactionTemplateById(id: Int) {
        financeDao.deleteTransactionTemplateById(id)
    }

    // ── Bills ────────────────────────────────────────────────────────────────
    val allBills: Flow<List<com.example.data.model.Bill>> = financeDao.getAllBills()

    suspend fun insertBill(bill: com.example.data.model.Bill) {
        financeDao.insertBill(bill)
    }

    suspend fun updateBill(bill: com.example.data.model.Bill) {
        financeDao.updateBill(bill)
    }

    suspend fun deleteBillById(id: Int) {
        financeDao.deleteBillById(id)
    }

    // ── Journal Entries ──────────────────────────────────────────────────────
    val allJournalEntries: Flow<List<com.example.data.model.JournalEntry>> = financeDao.getAllJournalEntries()

    suspend fun insertJournalEntry(entry: com.example.data.model.JournalEntry) {
        financeDao.insertJournalEntry(entry)
    }

    suspend fun deleteJournalEntryById(id: Int) {
        financeDao.deleteJournalEntryById(id)
    }
}
