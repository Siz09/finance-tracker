# Kharcha — Full App Audit
> Personal Finance Tracker · Android · Kotlin + Jetpack Compose + Room + ML Kit OCR
> Audit date: May 2026

---

## App Health Snapshot

| Area | Status | Summary |
|---|---|---|
| Data layer | ✅ Solid | Room with proper migrations, MVVM, Repository, Flow-based reactive state. Production-grade architecture. |
| OCR engine | ✅ Fixed | Bundled ML Kit, full-res camera, label-aware parser with eSewa/Fonepay field extraction. Review panel prevents silent overwrites. |
| UI completeness | ⚠️ Gaps remain | Core screens exist but several are functional stubs. No empty states, no onboarding, limited feedback. Charts are basic. No home screen widget. |
| Data safety | ⚠️ Partial | Export works. But there is no import/restore path — reinstall or app data clear means total data loss despite having a backup file. |
| Testing | ❌ Minimal | Only boilerplate tests. ReceiptParser, FinanceViewModel, and ExportHelper have zero unit tests. OCR regressions will go undetected. |
| Performance | ⚠️ Watch list | Logcat shows "Skipped 40 frames" on OCR completion — heavy work hitting the main thread. Image loading and DB queries need audit. |

---

## Code Quality — Things to Fix Now

### 1. Main thread jank on OCR completion
**Priority: High**

Logcat shows 35–40 skipped frames when OCR completes and the confirmation dialog appears. `ReceiptParser.parse()` is called inside the `addOnSuccessListener` callback directly on the main thread. Move the parse call inside `withContext(Dispatchers.Default)` in a coroutine, then switch back to main for UI state updates.

**Files:** `TransactionFormScreen.kt`, `ReceiptParser.kt`

```kotlin
// In processImageForOcr:
.addOnSuccessListener { visionText ->
    viewModelScope.launch {
        val parsed = withContext(Dispatchers.Default) {
            ReceiptParser.parse(visionText.text)
        }
        // Now back on main thread
        isScanning = false
        if (parsed.amount != null || ...) {
            pendingOcrResult = parsed
            showOcrConfirmationDialog = true
        }
    }
}
```

---

### 2. No import / restore from backup
**Priority: High**

You can export JSON but there is no import path. If the user reinstalls, clears app storage, or switches phones, all data is gone even with a backup file. Implement a JSON importer in `BackupScreen` — parse the exported format, validate it, and insert via Room with `OnConflictStrategy.IGNORE` to avoid duplicates on partial restore.

**Files:** `BackupScreen.kt`, `ExportHelper.kt`, `FinanceDao.kt`

---

### 3. Image storage grows forever
**Priority: High**

Receipt images in `filesDir/receipts/` are only deleted when a transaction is deleted. But if a user takes a photo then discards the form without saving, the temp image file is never cleaned up. Also no upper bound on total storage used.

**Fix:** Add a cleanup pass on app start that deletes orphaned images — files in `receipts/` with no matching `imagePath` in the DB. Also show a storage size indicator in `BackupScreen`.

**Files:** `FinanceApplication.kt`, `BackupScreen.kt`, `FileStorageHelper.kt`

```kotlin
// In FinanceApplication.onCreate():
fun cleanupOrphanedImages() {
    val receiptsDir = File(filesDir, "receipts")
    val knownPaths = repository.getAllImagePathsSync().toSet()
    receiptsDir.listFiles()?.forEach { file ->
        if (file.absolutePath !in knownPaths) file.delete()
    }
}
```

---

### 4. ViewModel has no error states for DB operations
**Priority: Medium**

All `viewModelScope.launch` blocks have no `try/catch`. A Room exception (constraint violation, disk full) is silently swallowed. Add specific error handling around `insertTransaction`, `updateTransaction`, and `deleteTransaction` with meaningful error strings the user can act on.

**Files:** `FinanceViewModel.kt`

---

### 5. Amount field has no inline validation feedback
**Priority: Medium**

The amount `OutlinedTextField` uses `KeyboardType.Decimal` but shows no error state. A user can type `0`, `-50`, `.`, or `1.2.3` — the form appears valid but the save button is disabled with no explanation. Add a red border + helper text when the amount is invalid.

**Files:** `TransactionFormScreen.kt`

---

### 6. No unit tests for ReceiptParser
**Priority: Medium**

The parser now handles 5 date formats, 3-level amount fallback, multi-line name joining, email reconstruction, and category suggestion. Any regex change can silently break a working receipt format. Add `ReceiptParserTest.kt` with one test per receipt type using the actual raw OCR text from Logcat as test fixtures.

**Files:** `ReceiptParser.kt`, `test/ReceiptParserTest.kt` (new)

```kotlin
@Test
fun `parse eSewa send money receipt`() {
    val raw = """
        Send Money
        1750
        2025-11-17 10:52 PM
        ...
        Receiver Name:
        Bhagya Narayan
        Choudhari
        Receiver Esewa ld:
        amitjay230 @gmail
        .Com
    """.trimIndent()
    val result = ReceiptParser.parse(raw)
    assertEquals(1750.0, result.amount)
    assertEquals("2025-11-17", result.date)
    assertEquals("Bhagya Narayan Choudhari", result.receiverName)
    assertEquals("amitjay230@gmail.com", result.receiverId)
}
```

---

### 7. Dashboard recent transactions ignores selected month
**Priority: Medium**

`recentLogs` filters `allTransactions` for the last 7 calendar days. When you change the selected month on the dashboard to review an old month, the recent list stays anchored to today minus 7 days. Either make it respect `selectedMonth`, or label the section clearly as "Last 7 days" to set accurate expectations.

**Files:** `DashboardScreen.kt`

---

### 8. Export uses raw Double — inconsistent decimal formatting
**Priority: Low**

`CurrencyFormatter` omits decimals for whole numbers which is correct for display, but the CSV/JSON export outputs the raw `Double` as `1750.0`. Standardise export to always use 2 decimal places for interoperability with spreadsheets and future import logic.

**Files:** `ExportHelper.kt`

```kotlin
// Instead of: sb.append("\"amount\": ${tx.amount},\n")
sb.append("\"amount\": ${"%.2f".format(tx.amount)},\n")
```

---

## New Features — High Value Additions

### 1. Recurring transactions
**Priority: High value**

Most personal finance pain comes from fixed recurring costs — rent, internet, subscriptions. Add a `recurring` flag and `frequency` field to `Transaction`, then use `WorkManager` to auto-insert on schedule. This is the single most-requested feature in personal finance apps and directly addresses the "forget to log" pain point.

**Files:** `Transaction.kt`, `FinanceDao.kt`, `MIGRATION_2_3`, new `RecurringWorker.kt`

Schema addition:
```kotlin
@ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
@ColumnInfo(name = "recurrence_frequency") val recurrenceFrequency: String? = null, // "monthly" | "weekly"
```

---

### 2. Android home screen widget
**Priority: High value**

A 2×2 glanceable widget showing this month's balance, income, and expense — without opening the app. Uses `AppWidgetProvider` + `RemoteViews`. Completely offline, no new dependencies. Particularly useful since the whole point of a finance tracker is quick awareness.

**Files:** new `FinanceWidget.kt`, `AndroidManifest.xml`, `res/layout/widget_finance.xml`, `res/xml/widget_info.xml`

---

### 3. Import / restore from JSON backup
**Priority: High value**

A file picker in `BackupScreen` that accepts a previously exported JSON file, validates the schema version, and inserts transactions/budgets/savings goals using `OnConflictStrategy.IGNORE` to avoid duplicates on partial restore. Prevents total data loss on reinstall or phone change. No internet needed.

**Files:** `BackupScreen.kt`, `ExportHelper.kt`, `FinanceDao.kt`

---

### 4. Quick-add shortcut
**Priority: Medium value**

The biggest friction in expense tracking is navigating to the add form. Add an Android app shortcut (long-press launcher icon) or a persistent notification action that jumps directly to `add_transaction`. Reduces logging friction enough to meaningfully improve consistency.

**Files:** new `QuickAddShortcut.kt`, `AndroidManifest.xml`, `res/xml/shortcuts.xml`

---

### 5. Monthly spending report screen
**Priority: Medium value**

A dedicated report screen showing: top 3 spending categories, month-over-month comparison (% change), biggest single expense, and daily average spend. Pure offline calculation from existing Room data — no new schema needed. Gives the analytics layer the app currently lacks.

**Files:** new `ReportsScreen.kt`, `FinanceViewModel.kt`

---

### 6. Biometric app lock
**Priority: Medium value**

A personal finance app with real transaction data on a shared or unlocked phone is a privacy risk. Add an optional `BiometricPrompt` gate on app open — one toggle in Settings. Uses Android's built-in biometric API, no extra dependencies, no internet. Approximately 2 hours to implement properly.

**Files:** new `BiometricHelper.kt`, `SettingsScreen.kt`, `MainActivity.kt`

```kotlin
val biometricPrompt = BiometricPrompt(this, executor,
    object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            showApp()
        }
        override fun onAuthenticationFailed() { finish() }
    }
)
```

---

### 7. Transaction search and filter
**Priority: Medium value**

The transactions screen has a search bar but no filter by type (income/expense), category, or date range. Adding filter chips and a date range picker would make the history screen genuinely useful for reviewing spending patterns.

**Implementation:** Filter chips row below the search bar — `All / Income / Expense` — plus a category dropdown. Filter applied in ViewModel as a derived `StateFlow` from `allTransactions`.

**Files:** `TransactionsScreen.kt`, `FinanceViewModel.kt`

---

### 8. Custom categories
**Priority: Nice to have**

Categories are currently hardcoded in `Category.kt` as data objects. Moving them to a `categories` Room table with a simple CRUD screen would let users adapt the app to their own spending habits and also fix OCR category suggestions when they guess wrong.

**Files:** `Category.kt`, `FinanceDao.kt`, new `CategoryScreen.kt`, `MIGRATION_2_3`

---

### 9. Multiple wallets / accounts
**Priority: Nice to have**

A lightweight `Account` concept (Cash, eSewa, Bank, Khalti) lets you track balances per payment method — which is how most Nepali users mentally bucket money. Requires a new `Account` table and a foreign key on `Transaction`, plus an account selector on the add form.

**Files:** new `Account.kt`, `Transaction.kt`, `FinanceDao.kt`, `MIGRATION_2_3`

---

## Design Improvements

### Missing entirely

**Onboarding flow**
A first-time user sees an empty dashboard with no guidance. Two or three onboarding screens (set a monthly budget → add your first transaction → enable reminders) would dramatically improve the first-run experience. Implement as a flag in the Room `settings` table — show once, never again.

**Empty states on every screen**
No transactions, no budgets, no savings goal — all show blank space. Each screen needs a contextual illustration + call-to-action:
- Transactions: "No transactions yet — tap + to log your first expense"
- Budget: "No budget set — tap a category to set a monthly limit"
- Dashboard chart: "Add transactions to see your spending breakdown"

---

### Polish improvements

**Chart interactivity**
The donut chart is custom Canvas-drawn but has no interactive tooltips, no legend labels on segments, and no load animation. Add tap-to-highlight on segments with a tooltip showing category name and exact amount. The `animateFloatAsState` already used elsewhere in the codebase can drive the highlight transition.

**Haptic feedback**
Destructive actions (delete transaction, clear savings goal) and successful saves feel weightless. Add `LocalHapticFeedback` on button presses — light click on save, heavier on delete:
```kotlin
val haptic = LocalHapticFeedback.current
Button(onClick = {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    viewModel.deleteTransaction(id)
})
```

**Loading states**
OCR scanning shows a spinner but saving, exporting, and initial dashboard load have no indicators. Add `isLoading: StateFlow<Boolean>` to the ViewModel with a `LinearProgressIndicator` at the top of affected screens.

**Transaction card expansion**
Cards show icon, category, note, and amount. OCR-extracted fields (receiver name, payment method, transaction code) are never visible without opening the edit form. Add a compact expandable card — tap to expand and reveal digital payment metadata inline.

**Dashboard layout hierarchy**
Currently everything stacks vertically with equal visual weight. Prioritise the layout:
1. Large balance card at top (dominant)
2. Income / expense in a 2-column row
3. Savings goal progress bar
4. Spending chart
5. Recent transactions list

**Theme toggle**
The app is currently dark-only (hardcoded `DarkBg`, `WhiteText`). A system-default / dark / light toggle in Settings requires a proper `ColorScheme` abstraction using Material3's theme system — non-trivial but a common user expectation.

---

## Technical Debt to Watch

### Room schema — currently at version 2
Any new column (recurring flag, account ID, custom category FK) requires `MIGRATION_2_3`. Keep a migrations log file alongside `FinanceDatabase.kt`. The most dangerous moment is adding a field and forgetting to bump the version — Room crashes on install rather than migrating silently.

### `TransactionFormScreen.kt` is too large
The form screen is a single massive Composable handling camera, gallery, OCR, date picker, delete dialog, receipt viewer, and metadata fields. Extract each concern:
- `ReceiptAttachment.kt` — camera/gallery/image display
- `OcrReviewDialog.kt` — confirmation overlay
- `MetadataCard.kt` — collapsible digital payment fields
- `TransactionDeleteDialog.kt` — delete confirmation

### Categories are a sealed class, not a DB entity
`Category.kt` uses hardcoded data objects. This blocks custom categories entirely. Moving to Room now, before the schema has complex foreign key relationships, is much easier than retrofitting later when thousands of transactions reference hardcoded category names.

### Amounts stored as `Double`
Floating point amounts accumulate rounding errors over hundreds of transactions. For currency, store as `Long` paisa (multiply by 100 on input, divide on display) or use `BigDecimal` for calculations. This is a schema migration — worth doing before the dataset grows large.

```kotlin
// Current — risky
@ColumnInfo(name = "amount") val amount: Double

// Better — exact
@ColumnInfo(name = "amount_paisa") val amountPaisa: Long // store as paisa (×100)

// Display helper
fun formatAmount(paisa: Long): String = CurrencyFormatter.format(paisa / 100.0)
```

---

## Recommended Build Order

### Immediate (reliability)
1. Wrap ReceiptParser in background coroutine — fixes 40-frame jank
2. Orphaned image cleanup on app start — silent storage leak
3. Import/restore from JSON — backup is useless without it

### Next sprint (highest value features)
4. Recurring transactions
5. Home screen widget
6. Transaction search + filter chips

### Polish sprint
7. Empty states on all screens
8. Expandable transaction cards
9. Haptic feedback on destructive actions
10. Onboarding flow

### Before adding more features (technical debt)
11. Move categories to Room
12. Migrate amounts to Long paisa
13. Add `ReceiptParserTest.kt`

---

*Audit produced May 2026. Schema at Room version 2. 30-issue production audit completed across 3 rounds prior to this document.*
