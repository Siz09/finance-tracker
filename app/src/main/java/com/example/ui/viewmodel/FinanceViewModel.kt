package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.DebtItem
import com.example.data.model.NetWorthItem
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import com.example.notifications.NotificationScheduler
import com.example.utils.ExportHelper
import com.example.utils.FileStorageHelper
import com.example.widget.FinanceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Nepal Fiscal Year View (Phase 5)
    val isNepalFiscalYearActive: StateFlow<Boolean> = repository.getSettingFlow("nepal_fiscal_year")
        .map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setNepalFiscalYearActive(active: Boolean) {
        viewModelScope.launch { repository.updateSetting("nepal_fiscal_year", active.toString()) }
    }

    fun getNepalFiscalYearDateRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val fyStartYear = if (month > 7 || (month == 7 && day >= 16)) year else year - 1
        return Pair("$fyStartYear-07-16", "${fyStartYear + 1}-07-15")
    }

    fun getNepalFiscalYearLabel(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val fyStartYear = if (month > 7 || (month == 7 && day >= 16)) year else year - 1
        val bsStartYear = fyStartYear + 57
        return "FY ${bsStartYear}/${(bsStartYear + 1) % 100} BS"
    }

    val nepalFiscalYearLabel: StateFlow<String> = flow {
        emit(getNepalFiscalYearLabel())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Nepal FY View")

    val nepalFiscalYearTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, isNepalFiscalYearActive
    ) { txs, active ->
        if (!active) emptyList()
        else {
            val range = getNepalFiscalYearDateRange()
            txs.filter { it.date >= range.first && it.date <= range.second }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardTransactions: StateFlow<List<Transaction>> = combine(
        currentMonthTransactions, nepalFiscalYearTransactions, isNepalFiscalYearActive
    ) { monthTxs, fyTxs, active ->
        if (active) fyTxs else monthTxs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current calculations (Month or Nepal FY)
    val totalIncome: StateFlow<Double> = combine(
        currentMonthTransactions, nepalFiscalYearTransactions, isNepalFiscalYearActive
    ) { monthTxs, fyTxs, active ->
        val txs = if (active) fyTxs else monthTxs
        txs.filter { it.type == "income" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = combine(
        currentMonthTransactions, nepalFiscalYearTransactions, isNepalFiscalYearActive
    ) { monthTxs, fyTxs, active ->
        val txs = if (active) fyTxs else monthTxs
        txs.filter { it.type == "expense" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Budgets for selected month
    val budgets: StateFlow<List<Budget>> = selectedMonth.flatMapLatest { month ->
        repository.getBudgetsForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savings goal for selected month (legacy behavior)
    val savingsGoal: StateFlow<SavingsGoal?> = selectedMonth.flatMapLatest { month ->
        repository.getSavingsGoalForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All savings goals
    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminder time configuration (e.g. "20:00")
    val reminderTime: StateFlow<String> = repository.getSettingFlow("notification_time")
        .map { it?.value ?: "20:00" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "20:00")

    // Accounts list
    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Theme Mode ("light", "dark", "system")
    val themeMode: StateFlow<String> = repository.getSettingFlow("theme_mode")
        .map { it?.value ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    // Spending Lock (Phase 4)
    val isSpendingLocked: StateFlow<Boolean> = repository.getSettingFlow("spending_lock")
        .map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSpendingLock(locked: Boolean) {
        viewModelScope.launch { repository.updateSetting("spending_lock", locked.toString()) }
    }

    // Transaction Templates
    val allTransactionTemplates: StateFlow<List<com.example.data.model.TransactionTemplate>> = repository.allTransactionTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        context: Context,
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
        initiatorName: String? = null,
        isRecurring: Boolean = false,
        recurrenceFrequency: String? = null,
        accountId: Int? = null,
        mood: String? = null
    ) {
        val normalizedDate = if (date.contains("/") && !date.contains("-")) {
            date.replace("/", "-")
        } else {
            date
        }
        viewModelScope.launch {
            try {
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
                        initiatorName = initiatorName,
                        isRecurring = isRecurring,
                        recurrenceFrequency = recurrenceFrequency,
                        accountId = accountId,
                        mood = mood
                    )
                )
                // Auto-switch display to the transaction's month so it shows up instantly
                val parsedMonth = if (normalizedDate.length >= 7) normalizedDate.substring(0, 7) else null
                if (parsedMonth != null && parsedMonth.matches(Regex("""^\d{4}-\d{2}$"""))) {
                    selectedMonth.value = parsedMonth
                }
                _events.emit(FinanceEvent.Success("Transaction added successfully"))
                // Refresh home screen widget
                FinanceWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to save transaction: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun updateTransaction(
        context: Context,
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
        initiatorName: String? = null,
        isRecurring: Boolean = false,
        recurrenceFrequency: String? = null,
        accountId: Int? = null,
        mood: String? = null
    ) {
        val normalizedDate = date.replace("/", "-")
        viewModelScope.launch {
            try {
                val existing = repository.getTransactionById(id) ?: run {
                    _events.emit(FinanceEvent.Error("Transaction not found — it may have been deleted."))
                    return@launch
                }
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
                        isRecurring = isRecurring,
                        recurrenceFrequency = recurrenceFrequency,
                        accountId = accountId,
                        createdAt = existing.createdAt,
                        mood = mood
                    )
                )
                // Auto-switch display to the transaction's month so it shows up instantly
                val parsedMonth = if (normalizedDate.length >= 7) normalizedDate.substring(0, 7) else null
                if (parsedMonth != null && parsedMonth.matches(Regex("""^\d{4}-\d{2}$"""))) {
                    selectedMonth.value = parsedMonth
                }
                _events.emit(FinanceEvent.Success("Transaction updated successfully"))
                // Refresh home screen widget
                FinanceWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to update transaction: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun deleteTransaction(context: Context, id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                _events.emit(FinanceEvent.Success("Transaction deleted successfully"))
                // Refresh home screen widget
                FinanceWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to delete transaction: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun addTransactionTemplate(name: String, amount: Double, category: String, type: String, note: String?) {
        viewModelScope.launch {
            try {
                repository.insertTransactionTemplate(com.example.data.model.TransactionTemplate(
                    name = name, amount = amount, category = category, type = type, note = note
                ))
                _events.emit(FinanceEvent.Success("Template saved successfully"))
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to save template: ${e.message}"))
            }
        }
    }

    fun deleteTransactionTemplate(id: Int) {
        viewModelScope.launch { repository.deleteTransactionTemplateById(id) }
    }

    // Budget Operations
    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            try {
                val currentMonth = selectedMonth.value
                val existing = repository.getBudgetByCategoryAndMonth(category, currentMonth)
                val newBudget = if (existing != null) {
                    existing.copy(monthlyLimit = limit)
                } else {
                    Budget(category = category, monthlyLimit = limit, month = currentMonth)
                }
                repository.insertBudget(newBudget)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to save budget: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteBudgetById(id)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to delete budget: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    // Savings Operations
    fun saveSavingsGoal(target: Double) {
        viewModelScope.launch {
            try {
                val currentMonth = selectedMonth.value
                val existing = repository.getSavingsGoalForMonthSuspend(currentMonth)
                val newGoal = if (existing != null) {
                    existing.copy(target = target)
                } else {
                    SavingsGoal(name = "Monthly Goal", target = target, month = currentMonth)
                }
                repository.insertSavingsGoal(newGoal)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to save savings goal: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.insertSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(id: Int) {
        viewModelScope.launch {
            repository.deleteSavingsGoalById(id)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.insertSavingsGoal(goal) // Uses REPLACE on conflict
        }
    }

    // Notifications configuration
    fun saveReminderTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                repository.updateSetting("notification_time", timeStr)
                NotificationScheduler.scheduleDailyNotification(context, hour, minute)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to save reminder: ${e.message ?: "Unknown error"}"))
            }
        }
    }



    // Theme Configuration
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            try {
                repository.updateSetting("theme_mode", mode)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to update theme: ${e.message}"))
            }
        }
    }

    // ── Wallets / Accounts Operations ─────────────────────────────────────────
    fun addAccount(name: String, type: String, emoji: String) {
        viewModelScope.launch {
            try {
                repository.insertAccount(Account(name = name, type = type, emoji = emoji))
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to create wallet: ${e.message}"))
            }
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteAccountById(id)
            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Failed to delete wallet: ${e.message}"))
            }
        }
    }

    // ── JSON Import / Restore ─────────────────────────────────────────────────

    fun importFromJSON(jsonContent: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    ExportHelper.parseImportedJSON(jsonContent)
                }

                if (result.transactions.isEmpty() && result.budgets.isEmpty() && result.savingsGoals.isEmpty()) {
                    val msg = if (result.errors.isNotEmpty()) result.errors.first()
                              else "Nothing to import — the file appears to be empty."
                    _events.emit(FinanceEvent.Error(msg))
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    if (result.transactions.isNotEmpty())
                        repository.insertTransactions(result.transactions)
                    if (result.budgets.isNotEmpty())
                        repository.insertBudgets(result.budgets)
                    if (result.savingsGoals.isNotEmpty())
                        repository.insertSavingsGoals(result.savingsGoals)
                }

                val summary = buildString {
                    append("Import complete: ")
                    append("${result.transactions.size} transactions")
                    if (result.budgets.isNotEmpty()) append(", ${result.budgets.size} budgets")
                    if (result.savingsGoals.isNotEmpty()) append(", ${result.savingsGoals.size} savings goals")
                    if (result.errors.isNotEmpty()) append(" (${result.errors.size} skipped)")
                }
                _events.emit(FinanceEvent.Success(summary))

            } catch (e: Exception) {
                _events.emit(FinanceEvent.Error("Import failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    // ── Net Worth Tracker ──────────────────────────────────────────────────────
    val allNetWorthItems: StateFlow<List<NetWorthItem>> = repository.allNetWorthItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAssets: StateFlow<Double> = allNetWorthItems.map { items ->
        items.filter { it.type == "asset" }.sumOf { it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilities: StateFlow<Double> = allNetWorthItems.map { items ->
        items.filter { it.type == "liability" }.sumOf { it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netWorth: StateFlow<Double> = combine(totalAssets, totalLiabilities) { assets, liabilities ->
        assets - liabilities
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addNetWorthItem(item: NetWorthItem) {
        viewModelScope.launch { repository.insertNetWorthItem(item) }
    }

    fun deleteNetWorthItem(id: Int) {
        viewModelScope.launch { repository.deleteNetWorthItemById(id) }
    }

    // ── Debt Payoff Tracker ────────────────────────────────────────────────────
    val allDebtItems: StateFlow<List<DebtItem>> = repository.allDebtItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDebtItem(item: DebtItem) {
        viewModelScope.launch { repository.insertDebtItem(item) }
    }

    fun updateDebtItem(item: DebtItem) {
        viewModelScope.launch { repository.insertDebtItem(item) } // REPLACE on conflict
    }

    fun deleteDebtItem(id: Int) {
        viewModelScope.launch { repository.deleteDebtItemById(id) }
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
