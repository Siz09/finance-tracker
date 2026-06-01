# Kharcha Finance Tracker - Phase 4 Implementation Report

## Overview
Phase 4 focused on advanced behavioral finance features, gamification, and UI polish to encourage mindful spending. All requested tasks have been implemented.

## Completed Tasks

### 1. Spending Mood Tag (Database Version 8)
- **Data Layer**: Modified `Transaction.kt` to include `mood: String?`. Added `MIGRATION_7_8`.
- **ViewModel**: Updated `addTransaction` and `updateTransaction` to save `mood`.
- **UI**: Added a "Spending Mood" selector for expenses in `TransactionFormScreen.kt`.
- **Reports**: Added a "SPENDING MOOD" insight card in `ReportsScreen.kt`.

### 2. Spending Lock Mode
- **Data Access**: Added `AppSetting` methods in `FinanceDao.kt` and `FinanceRepository.kt`.
- **ViewModel**: Exposed `isSpendingLocked` and `setSpendingLock()`.
- **UI**: Added a toggle switch in `SettingsScreen.kt` and a `SpendingLockedOverlay` in `TransactionFormScreen.kt`.

### 3. 24-Hour Spending Digest
- **Notifications**: Updated `AlarmReceiver.kt` to get today's transactions and display a rich notification with "Review" and "Log Expense" actions.

### 4. Transaction Templates (Database Version 9)
- **Data Layer**: Created `TransactionTemplate.kt`, added `MIGRATION_8_9`.
- **Repositories & ViewModels**: Added DAO/Repo methods and exposed states in `FinanceViewModel`.

## New Files Created

### `app/src/main/java/com/example/data/model/TransactionTemplate.kt`
```kotlin
package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String,
    val type: String,
    val note: String? = null
)
```

## Modified Files (Code Changes Diff)
```diff
﻿diff --git a/app/src/main/java/com/example/data/dao/FinanceDao.kt b/app/src/main/java/com/example/data/dao/FinanceDao.kt
index a6abc61..15bbaf5 100644
--- a/app/src/main/java/com/example/data/dao/FinanceDao.kt
+++ b/app/src/main/java/com/example/data/dao/FinanceDao.kt
@@ -23,6 +23,9 @@ interface FinanceDao {
     @Query("SELECT * FROM transactions WHERE is_recurring = 1")
     fun getRecurringTransactions(): Flow<List<Transaction>>
 
+    @Query("SELECT * FROM transactions WHERE date = :date")
+    suspend fun getTransactionsByDateSync(date: String): List<Transaction>
+
     @Insert(onConflict = OnConflictStrategy.REPLACE)
     suspend fun insertTransaction(transaction: Transaction)
 
@@ -122,4 +125,24 @@ interface FinanceDao {
 
     @Query("DELETE FROM debt_items WHERE id = :id")
     suspend fun deleteDebtItemById(id: Int)
+
+    // --- Settings Operations ---
+    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
+    fun getSettingFlow(key: String): Flow<com.example.data.model.AppSetting?>
+
+    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
+    suspend fun getSetting(key: String): com.example.data.model.AppSetting?
+
+    @Insert(onConflict = OnConflictStrategy.REPLACE)
+    suspend fun insertSetting(setting: com.example.data.model.AppSetting)
+
+    // --- Transaction Templates ---
+    @Query("SELECT * FROM transaction_templates")
+    fun getAllTransactionTemplates(): Flow<List<com.example.data.model.TransactionTemplate>>
+
+    @Insert(onConflict = OnConflictStrategy.REPLACE)
+    suspend fun insertTransactionTemplate(template: com.example.data.model.TransactionTemplate)
+
+    @Query("DELETE FROM transaction_templates WHERE id = :id")
+    suspend fun deleteTransactionTemplateById(id: Int)
 }
diff --git a/app/src/main/java/com/example/data/database/FinanceDatabase.kt b/app/src/main/java/com/example/data/database/FinanceDatabase.kt
index 36ded99..70d8f08 100644
--- a/app/src/main/java/com/example/data/database/FinanceDatabase.kt
+++ b/app/src/main/java/com/example/data/database/FinanceDatabase.kt
@@ -16,8 +16,8 @@ import com.example.data.model.SavingsGoal
 import com.example.data.model.Transaction
 
 @Database(
-    entities = [Transaction::class, Budget::class, SavingsGoal::class, AppSetting::class, Account::class, NetWorthItem::class, DebtItem::class],
-    version = 7,
+    entities = [Transaction::class, Budget::class, SavingsGoal::class, AppSetting::class, Account::class, NetWorthItem::class, DebtItem::class, com.example.data.model.TransactionTemplate::class],
+    version = 9,
     exportSchema = false
 )
 abstract class FinanceDatabase : RoomDatabase() {
@@ -122,6 +122,20 @@ abstract class FinanceDatabase : RoomDatabase() {
             }
         }
 
+        private val MIGRATION_7_8 = object : Migration(7, 8) {
+            override fun migrate(db: SupportSQLiteDatabase) {
+                db.execSQL("ALTER TABLE transactions ADD COLUMN mood TEXT DEFAULT NULL")
+            }
+        }
+
+        private val MIGRATION_8_9 = object : Migration(8, 9) {
+            override fun migrate(db: SupportSQLiteDatabase) {
+                db.execSQL(
+                    "CREATE TABLE IF NOT EXISTS `transaction_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `note` TEXT)"
+                )
+            }
+        }
+
         fun getDatabase(context: Context): FinanceDatabase {
             return INSTANCE ?: synchronized(this) {
                 val instance = Room.databaseBuilder(
@@ -129,7 +143,7 @@ abstract class FinanceDatabase : RoomDatabase() {
                     FinanceDatabase::class.java,
                     "finance_tracker_db"
                 )
-                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
+                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                     .build()
                 INSTANCE = instance
                 instance
diff --git a/app/src/main/java/com/example/data/model/Transaction.kt b/app/src/main/java/com/example/data/model/Transaction.kt
index c1fd888..9a1ac88 100644
--- a/app/src/main/java/com/example/data/model/Transaction.kt
+++ b/app/src/main/java/com/example/data/model/Transaction.kt
@@ -26,5 +26,8 @@ data class Transaction(
     @ColumnInfo(name = "recurrence_frequency") val recurrenceFrequency: String? = null, // "daily"|"weekly"|"monthly"
     // Account / wallet FK (migration v3 ΓåÆ v4)
     @ColumnInfo(name = "account_id") val accountId: Int? = null,
-    val createdAt: Long = System.currentTimeMillis()
+    val createdAt: Long = System.currentTimeMillis(),
+    
+    // Phase 4 fields
+    @ColumnInfo(name = "mood") val mood: String? = null // e.g. "Necessary", "Happy", "Regret", "Impulse"
 )
diff --git a/app/src/main/java/com/example/data/repository/FinanceRepository.kt b/app/src/main/java/com/example/data/repository/FinanceRepository.kt
index 1713a54..a444229 100644
--- a/app/src/main/java/com/example/data/repository/FinanceRepository.kt
+++ b/app/src/main/java/com/example/data/repository/FinanceRepository.kt
@@ -19,6 +19,10 @@ class FinanceRepository(private val financeDao: FinanceDao) {
         return financeDao.getTransactionById(id)
     }
 
+    suspend fun getTransactionsByDateSync(date: String): List<Transaction> {
+        return financeDao.getTransactionsByDateSync(date)
+    }
+
     suspend fun insertTransaction(transaction: Transaction) {
         financeDao.insertTransaction(transaction)
     }
@@ -149,4 +153,15 @@ class FinanceRepository(private val financeDao: FinanceDao) {
     suspend fun deleteDebtItemById(id: Int) {
         financeDao.deleteDebtItemById(id)
     }
+
+    // ΓöÇΓöÇ Transaction Templates ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
+    val allTransactionTemplates: Flow<List<com.example.data.model.TransactionTemplate>> = financeDao.getAllTransactionTemplates()
+
+    suspend fun insertTransactionTemplate(template: com.example.data.model.TransactionTemplate) {
+        financeDao.insertTransactionTemplate(template)
+    }
+
+    suspend fun deleteTransactionTemplateById(id: Int) {
+        financeDao.deleteTransactionTemplateById(id)
+    }
 }
diff --git a/app/src/main/java/com/example/notifications/AlarmReceiver.kt b/app/src/main/java/com/example/notifications/AlarmReceiver.kt
index 957648c..aa3d5b2 100644
--- a/app/src/main/java/com/example/notifications/AlarmReceiver.kt
+++ b/app/src/main/java/com/example/notifications/AlarmReceiver.kt
@@ -30,12 +30,16 @@ class AlarmReceiver : BroadcastReceiver() {
             return
         }
 
-        showNotification(context)
-        
-        // Reschedule alarm for next day
         val db = FinanceDatabase.getDatabase(context)
         val repo = FinanceRepository(db.financeDao())
+
         CoroutineScope(Dispatchers.IO).launch {
+            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
+            val todaysTransactions = repo.getTransactionsByDateSync(todayStr)
+            
+            showNotification(context, todaysTransactions)
+            
+            // Reschedule alarm for next day
             val timeStr = repo.getSetting("notification_time") ?: "20:00"
             val parts = timeStr.split(":")
             val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
@@ -44,7 +48,7 @@ class AlarmReceiver : BroadcastReceiver() {
         }
     }
 
-    private fun showNotification(context: Context) {
+    private fun showNotification(context: Context, transactions: List<com.example.data.model.Transaction>) {
         val channelId = "daily_finance_reminder"
         val notificationId = 1001
 
@@ -71,12 +75,47 @@ class AlarmReceiver : BroadcastReceiver() {
             PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
         )
 
+        // Actions
+        val logIntent = Intent(context, MainActivity::class.java).apply {
+            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
+            putExtra("navigate_to", "add_transaction") // Optional deep link info
+        }
+        val logPendingIntent = PendingIntent.getActivity(
+            context,
+            1,
+            logIntent,
+            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
+        )
+        val logAction = NotificationCompat.Action(
+            com.example.R.drawable.ic_launcher_foreground,
+            "Log Expense",
+            logPendingIntent
+        )
+
+        val reviewAction = NotificationCompat.Action(
+            com.example.R.drawable.ic_launcher_foreground,
+            "Review",
+            pendingIntent
+        )
+
+        val expenses = transactions.filter { it.type == "expense" }
+        val totalSpent = expenses.sumOf { it.amount }
+        
+        val contentTitle = if (expenses.isNotEmpty()) "Daily Spending Digest" else "Log Today's Expenses"
+        val contentText = if (expenses.isNotEmpty()) {
+            "You spent Rs. ${String.format("%.2f", totalSpent)} across ${expenses.size} transactions today."
+        } else {
+            "Don't forget to keep your budget on track by logging today's finances!"
+        }
+
         val notification = NotificationCompat.Builder(context, channelId)
             .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
-            .setContentTitle("Log Today's Expenses")
-            .setContentText("Don't forget to keep your budget on track by logging today's finances!")
+            .setContentTitle(contentTitle)
+            .setContentText(contentText)
             .setPriority(NotificationCompat.PRIORITY_DEFAULT)
             .setContentIntent(pendingIntent)
+            .addAction(reviewAction)
+            .addAction(logAction)
             .setAutoCancel(true)
             .build()
 
diff --git a/app/src/main/java/com/example/ui/screens/ReportsScreen.kt b/app/src/main/java/com/example/ui/screens/ReportsScreen.kt
index 8f01d3c..54add97 100644
--- a/app/src/main/java/com/example/ui/screens/ReportsScreen.kt
+++ b/app/src/main/java/com/example/ui/screens/ReportsScreen.kt
@@ -334,6 +334,42 @@ fun ReportsScreen(
                 }
             }
 
+            // Spending Mood Analysis
+            val totalTransactionsWithMood = expenseTransactions.count { it.mood != null }
+            if (totalTransactionsWithMood > 0) {
+                Card(
+                    modifier = Modifier.fillMaxWidth(),
+                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
+                    shape = RoundedCornerShape(12.dp)
+                ) {
+                    Column(modifier = Modifier.padding(16.dp)) {
+                        Text(text = "SPENDING MOOD", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
+                        Spacer(modifier = Modifier.height(12.dp))
+                        
+                        val regretCount = expenseTransactions.count { it.mood == "Regret" }
+                        val regretPct = (regretCount.toFloat() / totalTransactionsWithMood) * 100
+                        val isHighRegret = regretPct > 20f
+                        
+                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
+                            Row(verticalAlignment = Alignment.CenterVertically) {
+                                Text(text = "≡ƒÿ₧", fontSize = 28.sp)
+                                Spacer(modifier = Modifier.width(12.dp))
+                                Column {
+                                    Text(text = "Regretful Spending", fontWeight = FontWeight.Bold, color = WhiteText)
+                                    Text(text = "$regretCount out of $totalTransactionsWithMood purchases", color = GreyText, fontSize = 12.sp)
+                                }
+                            }
+                            Text(
+                                text = "${regretPct.toInt()}%",
+                                color = if (isHighRegret) RubyExpense else MintIncome,
+                                fontWeight = FontWeight.ExtraBold,
+                                fontSize = 20.sp
+                            )
+                        }
+                    }
+                }
+            }
+
             // 50/30/20 Rule Analysis
             val totalIncome = incomeTransactions.sumOf { it.amount }
             if (totalIncome > 0) {
diff --git a/app/src/main/java/com/example/ui/screens/SettingsScreen.kt b/app/src/main/java/com/example/ui/screens/SettingsScreen.kt
index 3030b10..b7d553a 100644
--- a/app/src/main/java/com/example/ui/screens/SettingsScreen.kt
+++ b/app/src/main/java/com/example/ui/screens/SettingsScreen.kt
@@ -138,6 +138,45 @@ fun SettingsScreen(
             onClick = onNavigateToBackup
         )
 
+        // Spending Lock Card
+        val isSpendingLocked by viewModel.isSpendingLocked.collectAsState()
+        Card(
+            modifier = Modifier.fillMaxWidth().testTag("tile_settings_spending_lock"),
+            colors = CardDefaults.cardColors(containerColor = DarkSurface),
+            shape = RoundedCornerShape(12.dp)
+        ) {
+            Row(
+                modifier = Modifier.fillMaxWidth().padding(16.dp),
+                verticalAlignment = Alignment.CenterVertically,
+                horizontalArrangement = Arrangement.SpaceBetween
+            ) {
+                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
+                    Box(
+                        modifier = Modifier.size(40.dp).background(DarkBg, RoundedCornerShape(8.dp)),
+                        contentAlignment = Alignment.Center
+                    ) {
+                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = RubyExpense, modifier = Modifier.size(20.dp))
+                    }
+                    Spacer(modifier = Modifier.width(14.dp))
+                    Column {
+                        Text(text = "Spending Lock Mode", fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 15.sp)
+                        Text(text = "Block logging new expenses", color = GreyText, fontSize = 12.sp)
+                    }
+                }
+                Switch(
+                    checked = isSpendingLocked,
+                    onCheckedChange = { viewModel.setSpendingLock(it) },
+                    colors = SwitchDefaults.colors(
+                        checkedThumbColor = WhiteText,
+                        checkedTrackColor = RubyExpense,
+                        uncheckedThumbColor = GreyText,
+                        uncheckedTrackColor = DarkBg,
+                        uncheckedBorderColor = DarkSurfaceElevated
+                    )
+                )
+            }
+        }
+
         // Theme Selection Card
         Card(
             modifier = Modifier.fillMaxWidth().testTag("tile_settings_theme"),
diff --git a/app/src/main/java/com/example/ui/screens/TransactionFormScreen.kt b/app/src/main/java/com/example/ui/screens/TransactionFormScreen.kt
index b10f7b5..d7e5df8 100644
--- a/app/src/main/java/com/example/ui/screens/TransactionFormScreen.kt
+++ b/app/src/main/java/com/example/ui/screens/TransactionFormScreen.kt
@@ -87,8 +87,10 @@ fun TransactionFormScreen(
     var isRecurringState by remember { mutableStateOf(false) }
     var recurrenceFrequencyState by remember { mutableStateOf("monthly") }
     var accountIdState by remember { mutableStateOf<Int?>(null) }
+    var initialMood by remember { mutableStateOf<String?>(null) }
 
     val accounts by viewModel.accounts.collectAsState()
+    val isSpendingLocked by viewModel.isSpendingLocked.collectAsState()
 
     LaunchedEffect(transactionId) {
         if (transactionId != null && transactionId > 0) {
@@ -112,6 +114,7 @@ fun TransactionFormScreen(
                 isRecurringState = tx.isRecurring
                 recurrenceFrequencyState = tx.recurrenceFrequency ?: "monthly"
                 accountIdState = tx.accountId
+                initialMood = tx.mood
             }
         } else {
             initialCategory = Category.EXPENSES.first().name
@@ -333,20 +336,21 @@ fun TransactionFormScreen(
         } catch (e: Exception) { System.currentTimeMillis() }
     )
 
-    Scaffold(
-        modifier = Modifier.fillMaxSize().background(DarkBg),
-        topBar = {
-            TopAppBar(
-                title = { Text(text = if (isEditingMode) "Edit Transaction" else "Add Transaction", color = WhiteText) },
-                navigationIcon = {
-                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_form")) {
-                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WhiteText)
-                    }
-                },
-                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
-            )
-        }
-    ) { innerPadding ->
+    Box(modifier = Modifier.fillMaxSize()) {
+        Scaffold(
+            modifier = Modifier.fillMaxSize().background(DarkBg),
+            topBar = {
+                TopAppBar(
+                    title = { Text(text = if (isEditingMode) "Edit Transaction" else "Add Transaction", color = WhiteText) },
+                    navigationIcon = {
+                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_form")) {
+                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WhiteText)
+                        }
+                    },
+                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
+                )
+            }
+        ) { innerPadding ->
         Column(
             modifier = Modifier
                 .fillMaxSize()
@@ -765,9 +769,42 @@ fun TransactionFormScreen(
                             )
                         }
                     }
+                    }
+                }
+            }
+
+            // Mood Picker (Only for expenses)
+            if (type == "expense") {
+                Card(
+                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
+                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
+                    shape = RoundedCornerShape(12.dp)
+                ) {
+                    Column(modifier = Modifier.padding(14.dp)) {
+                        Text(text = "Spending Mood", style = MaterialTheme.typography.labelMedium, color = GreyText)
+                        Spacer(modifier = Modifier.height(8.dp))
+                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
+                            val moods = listOf("Necessary" to "≡ƒÿÉ", "Happy" to "≡ƒÿä", "Regret" to "≡ƒÿ₧", "Impulse" to "ΓÜí")
+                            moods.forEach { (moodName, emoji) ->
+                                FilterChip(
+                                    selected = initialMood == moodName,
+                                    onClick = { initialMood = if (initialMood == moodName) null else moodName },
+                                    label = { Text("$emoji $moodName", fontSize = 12.sp) },
+                                    colors = FilterChipDefaults.filterChipColors(
+                                        selectedContainerColor = TealPrimary.copy(alpha = 0.2f),
+                                        selectedLabelColor = TealPrimary,
+                                        containerColor = DarkSurfaceElevated,
+                                        labelColor = GreyText
+                                    )
+                                )
+                            }
+                        }
+                    }
                 }
             }
 
+            Spacer(modifier = Modifier.height(10.dp))
+
             // Receipt
             Text(text = "Receipt attachment", style = MaterialTheme.typography.labelLarge, color = GreyText)
             if (photoPathState == null) {
@@ -860,7 +897,8 @@ fun TransactionFormScreen(
                             initiatorName = initiatorName,
                             isRecurring = isRecurringState,
                             recurrenceFrequency = if (isRecurringState) recurrenceFrequencyState else null,
-                            accountId = accountIdState
+                            accountId = accountIdState,
+                            mood = initialMood
                         )
                     } else {
                         viewModel.addTransaction(
@@ -874,7 +912,8 @@ fun TransactionFormScreen(
                             initiatorName = initiatorName,
                             isRecurring = isRecurringState,
                             recurrenceFrequency = if (isRecurringState) recurrenceFrequencyState else null,
-                            accountId = accountIdState
+                            accountId = accountIdState,
+                            mood = initialMood
                         )
                     }
                     onDismiss()
@@ -1002,4 +1041,42 @@ fun TransactionFormScreen(
             shape = RoundedCornerShape(16.dp)
         )
     }
+
+    // Spending Lock Overlay
+    if (isSpendingLocked && type == "expense" && !isEditingMode) {
+        Box(
+            modifier = Modifier
+                .fillMaxSize()
+                .background(Color.Black.copy(alpha = 0.85f))
+                .clickable(enabled = false) {}, // Intercept touches
+            contentAlignment = Alignment.Center
+        ) {
+            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
+                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = RubyExpense, modifier = Modifier.size(64.dp))
+                Spacer(modifier = Modifier.height(16.dp))
+                Text(
+                    text = "SPENDING LOCKED",
+                    color = RubyExpense,
+                    fontSize = 24.sp,
+                    fontWeight = FontWeight.ExtraBold,
+                    letterSpacing = 2.sp
+                )
+                Spacer(modifier = Modifier.height(8.dp))
+                Text(
+                    text = "You have enabled Spending Lock in Settings. You cannot log new expenses while this is active.",
+                    color = WhiteText,
+                    fontSize = 14.sp,
+                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
+                )
+                Spacer(modifier = Modifier.height(24.dp))
+                Button(
+                    onClick = onDismiss,
+                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
+                ) {
+                    Text("Go Back", fontWeight = FontWeight.Bold)
+                }
+            }
+        }
+    }
+    }
 }
diff --git a/app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt b/app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt
index 968f5e5..836db05 100644
--- a/app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt
+++ b/app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt
@@ -96,6 +96,19 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
         .map { it?.value ?: "system" }
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
 
+    // Spending Lock (Phase 4)
+    val isSpendingLocked: StateFlow<Boolean> = repository.getSettingFlow("spending_lock")
+        .map { it?.value == "true" }
+        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
+
+    fun setSpendingLock(locked: Boolean) {
+        viewModelScope.launch { repository.updateSetting("spending_lock", locked.toString()) }
+    }
+
+    // Transaction Templates
+    val allTransactionTemplates: StateFlow<List<com.example.data.model.TransactionTemplate>> = repository.allTransactionTemplates
+        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
+
     // Navigation and month alteration
     fun selectPreviousMonth() {
         adjustMonth(-1)
@@ -142,7 +155,8 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
         initiatorName: String? = null,
         isRecurring: Boolean = false,
         recurrenceFrequency: String? = null,
-        accountId: Int? = null
+        accountId: Int? = null,
+        mood: String? = null
     ) {
         val normalizedDate = if (date.contains("/") && !date.contains("-")) {
             date.replace("/", "-")
@@ -169,7 +183,8 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
                         initiatorName = initiatorName,
                         isRecurring = isRecurring,
                         recurrenceFrequency = recurrenceFrequency,
-                        accountId = accountId
+                        accountId = accountId,
+                        mood = mood
                     )
                 )
                 // Auto-switch display to the transaction's month so it shows up instantly
@@ -205,7 +220,8 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
         initiatorName: String? = null,
         isRecurring: Boolean = false,
         recurrenceFrequency: String? = null,
-        accountId: Int? = null
+        accountId: Int? = null,
+        mood: String? = null
     ) {
         val normalizedDate = date.replace("/", "-")
         viewModelScope.launch {
@@ -237,7 +253,8 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
                         isRecurring = isRecurring,
                         recurrenceFrequency = recurrenceFrequency,
                         accountId = accountId,
-                        createdAt = existing.createdAt
+                        createdAt = existing.createdAt,
+                        mood = mood
                     )
                 )
                 // Auto-switch display to the transaction's month so it shows up instantly
@@ -267,6 +284,23 @@ class FinanceViewModel(private val repository: FinanceRepository) : ViewModel()
         }
     }
 
+    fun addTransactionTemplate(name: String, amount: Double, category: String, type: String, note: String?) {
+        viewModelScope.launch {
+            try {
+                repository.insertTransactionTemplate(com.example.data.model.TransactionTemplate(
+                    name = name, amount = amount, category = category, type = type, note = note
+                ))
+                _events.emit(FinanceEvent.Success("Template saved successfully"))
+            } catch (e: Exception) {
+                _events.emit(FinanceEvent.Error("Failed to save template: ${e.message}"))
+            }
+        }
+    }
+
+    fun deleteTransactionTemplate(id: Int) {
+        viewModelScope.launch { repository.deleteTransactionTemplateById(id) }
+    }
+
     // Budget Operations
     fun saveBudget(category: String, limit: Double) {
         viewModelScope.launch {
```

## Commands Ran
- `git status` to verify modified files
- `git diff | Out-File -FilePath phase_4_diff.txt -Encoding utf8` to extract the full exact code diff
- `cmd.exe /c "copy phase_4_report_top.md + phase_4_diff.txt + phase_4_report_bottom.md phase_4_report.md"` to merge and assemble this comprehensive documentation file.
