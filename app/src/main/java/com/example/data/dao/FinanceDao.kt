package com.example.data.dao

import androidx.room.*
import com.example.data.model.AppSetting
import com.example.data.model.Budget
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

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

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    // Savings Goals
    @Query("SELECT * FROM savings_goals WHERE month = :month LIMIT 1")
    fun getSavingsGoalForMonth(month: String): Flow<SavingsGoal?>

    @Query("SELECT * FROM savings_goals WHERE month = :month LIMIT 1")
    suspend fun getSavingsGoalForMonthSuspend(month: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}
