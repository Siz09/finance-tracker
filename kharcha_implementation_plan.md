# Kharcha — Implementation Plan

> Mapped against the existing codebase as of June 2026.
> Package: `com.example` · 36 Kotlin source files already in place.

---

## Existing Codebase Snapshot

| Layer | Files Already Present |
|---|---|
| Data models | `Transaction.kt`, `Budget.kt`, `SavingsGoal.kt`, `Category.kt`, `Account.kt`, `AppSetting.kt` |
| Database | `FinanceDatabase.kt`, `FinanceDao.kt` |
| Repository | `FinanceRepository.kt` |
| ViewModel | `FinanceViewModel.kt` |
| Screens | `DashboardScreen.kt`, `TransactionsScreen.kt`, `TransactionFormScreen.kt`, `ReportsScreen.kt`, `SettingsScreen.kt`, `BudgetScreen.kt`, `SavingsGoalScreen.kt`, `AccountsScreen.kt`, `BackupScreen.kt`, `NotificationsSettingsScreen.kt` |
| Components | `TransactionComponents.kt`, `FormDialogs.kt` |
| Utils | `ReceiptParser.kt`, `ExportHelper.kt`, `CurrencyFormatter.kt`, `BiometricHelper.kt`, `FileStorageHelper.kt` |
| Notifications | `AlarmReceiver.kt`, `NotificationScheduler.kt`, `RecurringWorker.kt` |
| Widget | `FinanceWidgetProvider.kt` |

---

## Phase 1 — Immediate Wins (Low effort, high visibility)

These require **no new DB tables** and use existing data/StateFlows.

### 1.1 Average Daily Spend Card
**Effort: ~1–2 hours** | **Files:** `FinanceViewModel.kt`, `DashboardScreen.kt`

- Add `averageDailySpend: StateFlow<Double>` to FinanceViewModel combining `totalExpense` + `selectedMonth`
- Add a metric card on DashboardScreen next to the balance card
- Show projected month-end total as a secondary label

```kotlin
val averageDailySpend = combine(totalExpense, selectedMonth) { expense, month ->
    val daysElapsed = calculateDaysElapsed(month)
    if (daysElapsed > 0) expense / daysElapsed else 0.0
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
```

---

### 1.2 Expandable Transaction Cards
**Effort: ~2 hours** | **Files:** `TransactionComponents.kt`, `DashboardScreen.kt`

- Add an `expanded: Boolean` local state per card
- Use `AnimatedVisibility` (already in codebase) to reveal hidden OCR fields:
  `receiverName`, `transactionCode`, `paymentMethod`
- No new data needed — fields already stored in Room

---

### 1.3 Transaction Filter Chips
**Effort: ~3 hours** | **Files:** `TransactionsScreen.kt`, `FinanceViewModel.kt`

- Add filter state to ViewModel: `filterType`, `filterCategories`, `filterDateRange`
- Derive `filteredTransactions: StateFlow` from `allTransactions`
- Add chip row below the search bar:
  - **Type chip**: All / Income / Expense
  - **Category chip**: multi-select bottom sheet
  - **Date chip**: start + end date pickers

---

### 1.4 Month-over-Month Comparison
**Effort: ~3–4 hours** | **Files:** `ReportsScreen.kt`, `FinanceViewModel.kt`

- Add `CategoryComparison` data class to ViewModel
- Derive comparison StateFlow from transactions grouped by current vs. prior month
- Render as a list of rows with `↑ red` / `↓ green` percent change badges

```kotlin
data class CategoryComparison(
    val category: String,
    val thisMonth: Double,
    val lastMonth: Double,
    val percentChange: Double = if (lastMonth == 0.0) 100.0
        else ((thisMonth - lastMonth) / lastMonth) * 100
)
```

---

## Phase 2 — High Value Features

These may need **new DB tables** or **significant new screens**.

### 2.1 Spending Trend Line Chart (6-month)
**Effort: ~4 hours** | **Files:** `ReportsScreen.kt`
**Dependency:** A charting library (e.g. `Vico` or `MPAndroidChart`)

- Group `allTransactions` by `date.substring(0,7)` (yyyy-MM) and sum expenses
- Render a smooth line chart in ReportsScreen as the primary chart above the donut

> [!IMPORTANT]
> Check `app/build.gradle.kts` — add `implementation("com.patrykandpatrick.vico:compose:x.x.x")` or equivalent.

---

### 2.2 Net Worth Tracker
**Effort: ~6 hours** | **New files:** `Asset.kt`, `Liability.kt`, `NetWorthScreen.kt`
**DB:** `MIGRATION_3_4` — add `assets` and `liabilities` tables

- Two-tab screen: Assets | Liabilities
- Running net worth total at the top
- FAB to add asset/liability with type dropdown

---

### 2.3 Voice-to-Transaction
**Effort: ~5 hours** | **New file:** `VoiceParser.kt` | **Modified:** `TransactionFormScreen.kt`
**Permission:** `RECORD_AUDIO` in AndroidManifest

- Mic FAB on `TransactionFormScreen`
- Uses `SpeechRecognizer` (on-device, no API key)
- `VoiceParser.parse(speech)` extracts amount + note, pre-fills the form

---

### 2.4 Smart Spending Insights
**Effort: ~4 hours** | **New file:** `InsightEngine.kt` | **Modified:** `ReportsScreen.kt`

- Pure Kotlin, fully offline rule engine
- Surface 2–3 auto-generated strings in a card at the top of ReportsScreen
- Rules: biggest expense, category overspend (>20% MoM), under-budget streaks

---

## Phase 3 — Medium Effort, High Impact

### 3.1 Multiple Savings Goals
**Effort: ~5 hours** | **Files:** `SavingsGoal.kt`, `SavingsGoalScreen.kt`
**DB:** `MIGRATION_3_4` — add `name` + `deadline` + `current_amount` columns

- Refactor SavingsGoalScreen to a list of goal cards
- Each card has a progress bar, name, deadline countdown
- FAB to create a new goal

---

### 3.2 Debt Payoff Tracker
**Effort: ~6 hours** | **New files:** `Debt.kt`, `DebtScreen.kt`
**DB:** `MIGRATION_3_4` — add `debts` table

- List debts with payoff timeline
- Toggle between **Avalanche** (highest interest first) and **Snowball** (smallest balance first) strategies
- Show total interest to be paid under each strategy

---

### 3.3 50/30/20 Calculator
**Effort: ~2 hours** | **Files:** `ReportsScreen.kt`, `FinanceViewModel.kt`

- A section in ReportsScreen: enter monthly income → get recommended split
- Compare against actual spending pulled from Room
- Colour-coded: green = within allocation, red = over

---

### 3.4 Envelope Budgeting Mode
**Effort: ~8 hours** | **New files:** `Envelope.kt`, `BudgetScreen.kt` (overhaul)
**DB:** `MIGRATION_3_4` — add `envelopes` table

- At month start, allocate income across envelopes
- Each transaction deducts from its envelope
- Visual "emptying" progress bar; zero-envelope warning on transaction entry

---

## Phase 4 — Unique / Differentiating

### 4.1 Spending Mood Tag
**Effort: ~3 hours** | **Files:** `Transaction.kt`, `TransactionFormScreen.kt`, `ReportsScreen.kt`
**DB:** `MIGRATION_3_4` — add `mood` column (nullable String)

- 4-option emoji picker on the transaction form (Necessary / Happy / Regret / Impulse)
- Regret % report card in ReportsScreen

---

### 4.2 Spending Lock / Pause Mode
**Effort: ~2 hours** | **Files:** `SettingsScreen.kt`, `TransactionFormScreen.kt`

- Toggle in Settings → stored in `AppSetting` table (key: `spending_lock`)
- On TransactionFormScreen: if locked + type == expense → show `SpendingLockedOverlay`

---

### 4.3 24-Hour Spending Digest Notification
**Effort: ~3 hours** | **Files:** `AlarmReceiver.kt`, `NotificationScheduler.kt`

- Schedule a daily alarm at 22:00
- `AlarmReceiver` queries today's transactions → builds digest string
- Rich notification with "Review" + "Log expense" actions

---

### 4.4 Transaction Templates
**Effort: ~4 hours** | **New file:** `Template.kt` | **Modified:** `TransactionFormScreen.kt`
**DB:** `MIGRATION_3_4` — add `templates` table

- "Save as template" option on the form
- A template picker sheet at the top of the form for one-tap fill

---

## Phase 5 — Long Term

| Feature | Key files | DB change |
|---|---|---|
| Cash flow calendar | new `CalendarScreen.kt` | None |
| Bill & subscription tracker | new `BillsScreen.kt`, `RecurringWorker.kt` | Optional |
| Budget rollover | `Budget.kt`, `RecurringWorker.kt` | `rollover_amount` column |
| Financial journal | new `Journal.kt`, `JournalScreen.kt` | new `journal_entries` table |
| Nepal fiscal year view | `FinanceViewModel.kt`, `DashboardScreen.kt` | None |
| Zero-based budgeting | `BudgetScreen.kt`, `FinanceViewModel.kt` | None |
| Pay-yourself-first | `SavingsGoalScreen.kt`, `FinanceViewModel.kt` | None |
| Lock screen widget | `FinanceWidgetProvider.kt`, `widget_info.xml` | None |
| Onboarding flow | new `OnboardingScreen.kt`, `MainActivity.kt` | settings flag |
| Time-of-day pattern | `ReportsScreen.kt`, `Transaction.kt` | `time` column |

---

## Database Migration Strategy

All schema changes should land in a **single `MIGRATION_3_4`** to avoid multiple migration hops. Planned additions:

```sql
-- Assets & liabilities (Net Worth)
CREATE TABLE assets (id INTEGER PRIMARY KEY, name TEXT, value REAL, type TEXT, updated_at TEXT);
CREATE TABLE liabilities (id INTEGER PRIMARY KEY, name TEXT, amount REAL, type TEXT);

-- Debt payoff
CREATE TABLE debts (id INTEGER PRIMARY KEY, name TEXT, principal REAL, interest_rate REAL, minimum_payment REAL, due_date TEXT);

-- Envelopes
CREATE TABLE envelopes (id INTEGER PRIMARY KEY, category TEXT, allocated REAL, month TEXT);

-- Templates
CREATE TABLE templates (id INTEGER PRIMARY KEY, name TEXT, type TEXT, amount REAL, category TEXT, note TEXT);

-- Existing table alterations
ALTER TABLE transactions ADD COLUMN mood TEXT;
ALTER TABLE transactions ADD COLUMN time TEXT;
ALTER TABLE savings_goals ADD COLUMN name TEXT DEFAULT 'Monthly Savings';
ALTER TABLE savings_goals ADD COLUMN deadline TEXT;
ALTER TABLE savings_goals ADD COLUMN current_amount REAL DEFAULT 0.0;
ALTER TABLE budgets ADD COLUMN rollover_amount REAL DEFAULT 0.0;
ALTER TABLE budgets ADD COLUMN rollover_enabled INTEGER DEFAULT 0;
```

---

## Recommended Next Step

Start with **Phase 1** — all four features there take under a day combined and will immediately make the app feel significantly more polished. Confirm the order below, or pick individual features to start with:

1. ☐ Average daily spend card (Dashboard)
2. ☐ Expandable transaction cards (OCR metadata reveal)
3. ☐ Transaction filter chips
4. ☐ Month-over-month comparison (Reports)
