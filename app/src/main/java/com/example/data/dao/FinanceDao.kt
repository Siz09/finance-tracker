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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE is_recurring = 1")
    fun getRecurringTransactions(): Flow<List<Transaction>>

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

    @Query("DELETE FROM debt_items WHERE id = :id")
    suspend fun deleteDebtItemById(id: Int)
}
