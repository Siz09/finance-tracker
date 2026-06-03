package com.example.data.dao

import androidx.room.*
import com.example.data.model.Account
import com.example.data.model.AppSetting
import com.example.data.model.Budget
import com.example.data.model.DebtItem
import com.example.data.model.NetWorthItem
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSuspend(): List<Transaction>

    // Efficient date-range query — avoids loading all rows for budget-rollover checks.
    @Query("SELECT * FROM transactions WHERE date LIKE :monthPrefix || '%'")
    suspend fun getTransactionsForMonthSuspend(monthPrefix: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE is_recurring = 1")
    fun getRecurringTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date = :date")
    suspend fun getTransactionsByDateSync(date: String): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // Bulk-insert for JSON restore — IGNORE prevents duplicating existing records.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // Budgets
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getBudgetsForMonthSuspend(month: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(category: String, month: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    // Savings Goals
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE month = :month LIMIT 1")
    fun getSavingsGoalForMonth(month: String): Flow<SavingsGoal?>

    @Query("SELECT * FROM savings_goals WHERE month = :month LIMIT 1")
    suspend fun getSavingsGoalForMonthSuspend(month: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavingsGoals(savingsGoals: List<SavingsGoal>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Int)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    // ── Accounts / Wallets ────────────────────────────────────────────────────
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Int)

    // Net Worth Items
    @Query("SELECT * FROM net_worth_items ORDER BY createdAt DESC")
    fun getAllNetWorthItems(): Flow<List<NetWorthItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetWorthItem(item: NetWorthItem)

    @Query("DELETE FROM net_worth_items WHERE id = :id")
    suspend fun deleteNetWorthItemById(id: Int)

    // Debt Items
    @Query("SELECT * FROM debt_items")
    fun getAllDebtItems(): Flow<List<DebtItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtItem(item: DebtItem)

    @Update
    suspend fun updateDebtItem(item: DebtItem)

    @Query("DELETE FROM debt_items WHERE id = :id")
    suspend fun deleteDebtItemById(id: Int)

    // --- Transaction Templates ---
    @Query("SELECT * FROM transaction_templates")
    fun getAllTransactionTemplates(): Flow<List<com.example.data.model.TransactionTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTemplate(template: com.example.data.model.TransactionTemplate)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteTransactionTemplateById(id: Int)

    // --- Bills ---
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<com.example.data.model.Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: com.example.data.model.Bill)

    @Update
    suspend fun updateBill(bill: com.example.data.model.Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Int)

    // --- Journal Entries ---
    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<com.example.data.model.JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: com.example.data.model.JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntryById(id: Int)
}
