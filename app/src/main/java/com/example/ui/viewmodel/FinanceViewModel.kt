package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import com.example.notifications.NotificationScheduler
import com.example.utils.FileStorageHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class FinanceEvent {
    data class Success(val message: String) : FinanceEvent()
    data class Error(val message: String) : FinanceEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _events = MutableSharedFlow<FinanceEvent>()
    val events = _events.asSharedFlow()

    // Selected month in "YYYY-MM" format
    val selectedMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )

    // All transactions ordered by date descending
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions filtered for the currently selected month
    val currentMonthTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, selectedMonth
    ) { txs, month ->
        txs.filter { it.date.startsWith(month) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month calculations
    val totalIncome: StateFlow<Double> = currentMonthTransactions.map { txs ->
        txs.filter { it.type == "income" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = currentMonthTransactions.map { txs ->
        txs.filter { it.type == "expense" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Budgets for selected month
    val budgets: StateFlow<List<Budget>> = selectedMonth.flatMapLatest { month ->
        repository.getBudgetsForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savings goal for selected month
    val savingsGoal: StateFlow<SavingsGoal?> = selectedMonth.flatMapLatest { month ->
        repository.getSavingsGoalForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reminder time configuration (e.g. "20:00")
    val reminderTime: StateFlow<String> = repository.getSettingFlow("notification_time")
        .map { it?.value ?: "20:00" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "20:00")

    // Navigation and month alteration
    fun selectPreviousMonth() {
        adjustMonth(-1)
    }

    fun selectNextMonth() {
        adjustMonth(1)
    }

    private fun adjustMonth(amount: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(selectedMonth.value) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, amount)
            }
            selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Transaction CRUD
    suspend fun getTransactionById(id: Int): Transaction? {
        return repository.getTransactionById(id)
    }

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        date: String,
        note: String?,
        imagePath: String?,
        receiverName: String? = null,
        receiverId: String? = null,
        remarks: String? = null,
        paymentMethod: String? = null,
        transactionCode: String? = null,
        processedBy: String? = null,
        purpose: String? = null,
        initiatorName: String? = null
    ) {
        val normalizedDate = if (date.contains("/") && !date.contains("-")) {
            date.replace("/", "-")
        } else {
            date
        }
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    category = category,
                    date = normalizedDate,
                    note = note,
                    imagePath = imagePath,
                    receiverName = receiverName,
                    receiverId = receiverId,
                    remarks = remarks,
                    paymentMethod = paymentMethod,
                    transactionCode = transactionCode,
                    processedBy = processedBy,
                    purpose = purpose,
                    initiatorName = initiatorName
                )
            )
            // Auto-switch display to the transaction's month so it shows up instantly
            val parsedMonth = if (normalizedDate.length >= 7) normalizedDate.substring(0, 7) else null
            if (parsedMonth != null && parsedMonth.matches(Regex("""^\d{4}-\d{2}$"""))) {
                selectedMonth.value = parsedMonth
            }
            _events.emit(FinanceEvent.Success("Transaction added successfully"))
        }
    }

    fun updateTransaction(
        id: Int,
        type: String,
        amount: Double,
        category: String,
        date: String,
        note: String?,
        imagePath: String?,
        receiverName: String? = null,
        receiverId: String? = null,
        remarks: String? = null,
        paymentMethod: String? = null,
        transactionCode: String? = null,
        processedBy: String? = null,
        purpose: String? = null,
        initiatorName: String? = null
    ) {
        val normalizedDate = date.replace("/", "-")
        viewModelScope.launch {
            val existing = repository.getTransactionById(id) ?: return@launch
            if (existing.imagePath != null && existing.imagePath != imagePath) {
                FileStorageHelper.deleteImage(existing.imagePath)
            }
            repository.updateTransaction(
                Transaction(
                    id = id,
                    type = type,
                    amount = amount,
                    category = category,
                    date = normalizedDate,
                    note = note,
                    imagePath = imagePath,
                    receiverName = receiverName,
                    receiverId = receiverId,
                    remarks = remarks,
                    paymentMethod = paymentMethod,
                    transactionCode = transactionCode,
                    processedBy = processedBy,
                    purpose = purpose,
                    initiatorName = initiatorName,
                    createdAt = existing.createdAt
                )
            )
            // Auto-switch display to the transaction's month so it shows up instantly
            val parsedMonth = if (normalizedDate.length >= 7) normalizedDate.substring(0, 7) else null
            if (parsedMonth != null && parsedMonth.matches(Regex("""^\d{4}-\d{2}$"""))) {
                selectedMonth.value = parsedMonth
            }
            _events.emit(FinanceEvent.Success("Transaction updated successfully"))
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _events.emit(FinanceEvent.Success("Transaction deleted successfully"))
        }
    }

    // Budget Operations
    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val currentMonth = selectedMonth.value
            val list = repository.getBudgetsForMonthSuspend(currentMonth)
            val existing = list.firstOrNull { it.category == category }
            val newBudget = if (existing != null) {
                existing.copy(monthlyLimit = limit)
            } else {
                Budget(category = category, monthlyLimit = limit, month = currentMonth)
            }
            repository.insertBudget(newBudget)
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
        }
    }

    // Savings Operations
    fun saveSavingsGoal(target: Double) {
        viewModelScope.launch {
            val currentMonth = selectedMonth.value
            // Use proper suspend DAO query instead of firstOrNull() on Flow
            val existing = repository.getSavingsGoalForMonthSuspend(currentMonth)
            val newGoal = if (existing != null) {
                existing.copy(target = target)
            } else {
                SavingsGoal(target = target, month = currentMonth)
            }
            repository.insertSavingsGoal(newGoal)
        }
    }

    // Notifications configuration
    fun saveReminderTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            repository.updateSetting("notification_time", timeStr)
            NotificationScheduler.scheduleDailyNotification(context, hour, minute)
        }
    }

    // Factory Class pattern
    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
