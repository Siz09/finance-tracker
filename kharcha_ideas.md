# Kharcha — Feature Ideas & Research
> Personal Finance Tracker · Android · Kotlin + Jetpack Compose
> Research date: June 2026

---

## Budgeting Methods

### 1. Envelope budgeting mode
**Priority: High value**

Used by Goodbudget, YNAB, and every top budgeting app. Digital envelopes per category — allocate income into envelopes at month start, each transaction pulls from its envelope. Visual "envelope emptying" as you spend. Most effective method for overspending control and directly addresses the overspending pain point.

**Files:** new `Envelope.kt`, `BudgetScreen.kt`, `MIGRATION_3_4`

**How it works:**
1. At month start, user allocates income across envelopes (Food: Rs.5,000 / Transport: Rs.2,000 / etc.)
2. Each expense transaction pulls from the matching envelope
3. Envelope shows remaining balance with a progress bar
4. When an envelope hits zero, a warning prevents further spending in that category

---

### 2. 50/30/20 rule calculator
**Priority: Medium value**

The most popular budgeting framework — 50% needs, 30% wants, 20% savings. A one-screen calculator inside `ReportsScreen`: enter monthly income, get recommended allocations, and compare against your actual spending. Pure offline calculation from Room data. Takes about 2 hours to build.

**Files:** `ReportsScreen.kt`, `FinanceViewModel.kt`

```kotlin
data class BudgetAllocation(
    val needs: Double,     // 50% — rent, food, utilities, transport
    val wants: Double,     // 30% — entertainment, shopping, dining out
    val savings: Double    // 20% — savings goal, investments
)

fun calculate5030(income: Double) = BudgetAllocation(
    needs = income * 0.50,
    wants = income * 0.30,
    savings = income * 0.20
)
```

---

### 3. Budget rollover
**Priority: Medium value**

PocketGuard's most praised feature. If you underspend in a category (e.g. spent Rs.800 of Rs.1,500 food budget), the Rs.700 surplus rolls into next month's envelope or a savings pool. Requires a `rollover` column in the budgets table and logic in `RecurringWorker` to carry balances forward each month.

**Files:** `Budget.kt`, `RecurringWorker.kt`, `MIGRATION_3_4`

Schema addition:
```kotlin
@ColumnInfo(name = "rollover_amount") val rolloverAmount: Double = 0.0
@ColumnInfo(name = "rollover_enabled") val rolloverEnabled: Boolean = false
```

---

### 4. Zero-based budgeting mode
**Priority: Nice to have**

Give every rupee a job — income minus all category allocations must equal zero. A "unallocated" counter at the top of `BudgetScreen` shows how much income is still unassigned. Turns the existing budget screen into a proper zero-based planner with minimal schema changes.

**Files:** `BudgetScreen.kt`, `FinanceViewModel.kt`

---

### 5. Pay-yourself-first mode
**Priority: Nice to have**

Set a fixed savings amount that transfers to your savings goal before anything else. When income is logged, the savings goal is auto-credited first and the remainder is available for budgeting. One toggle in `SavingsGoalScreen`.

**Files:** `SavingsGoalScreen.kt`, `FinanceViewModel.kt`

---

## Financial Tracking

### 1. Net worth tracker
**Priority: High value**

Assets (savings, shares, property value) minus liabilities (loans, dues) = net worth. Manual entry, updated whenever you want. A simple Assets/Liabilities screen with a running total. Offline, no schema complexity. Watching net worth grow monthly is more motivationally powerful than tracking spending alone.

**Files:** new `NetWorthScreen.kt`, new `Asset.kt`, new `Liability.kt`, `MIGRATION_3_4`

```kotlin
@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "type") val type: String, // "cash" | "shares" | "property" | "other"
    @ColumnInfo(name = "updated_at") val updatedAt: String
)

@Entity(tableName = "liabilities")
data class Liability(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "type") val type: String // "loan" | "credit" | "due" | "other"
)
```

---

### 2. Debt payoff tracker
**Priority: High value**

One of the most-downloaded finance features in 2025. Track loans and dues with a payoff plan. Supports both the avalanche method (highest interest first) and snowball method (smallest balance first). Each debt has a name, principal, interest rate, and minimum payment. Shows payoff timeline and total interest paid.

**Files:** new `Debt.kt`, new `DebtScreen.kt`, `MIGRATION_3_4`

```kotlin
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,           // "NIC Asia loan"
    @ColumnInfo(name = "principal") val principal: Double,
    @ColumnInfo(name = "interest_rate") val interestRate: Double, // annual %
    @ColumnInfo(name = "minimum_payment") val minimumPayment: Double,
    @ColumnInfo(name = "due_date") val dueDate: String?
)
```

**Payoff methods:**
- Avalanche: sort by highest interest rate → saves the most money
- Snowball: sort by smallest balance → fastest psychological wins

---

### 3. Cash flow calendar
**Priority: Medium value**

Show transactions on a calendar view — each day shows net cash flow. Green for income days, red for expense days, neutral for empty days. Uses existing transaction data, no new schema. Particularly useful for freelancers and irregular income earners.

**Files:** new `CalendarScreen.kt`, existing `allTransactions StateFlow`

---

### 4. Bill & subscription tracker
**Priority: Medium value**

A manual "upcoming bills" list with due dates, amounts, and reminder flags. Common fixed monthly expenses: NTC/Ncell recharge, WorldLink internet, DishHome, NEA electricity. Integrates with the existing recurring transactions system — a bill is just a recurring expense with a due date reminder.

**Files:** `RecurringWorker.kt`, new `BillsScreen.kt`

---

### 5. Multiple savings goals
**Priority: Medium value**

Currently only one savings goal per month. Real-world use needs multiple concurrent goals — emergency fund, phone upgrade, vacation, bike service. Each goal has a name, target amount, deadline, and dedicated progress bar.

Schema change: `savings_goals` becomes a multi-row table with a `name` column rather than one row per month.

**Files:** `SavingsGoal.kt`, `SavingsGoalScreen.kt`, `MIGRATION_3_4`

```kotlin
// Add to SavingsGoal:
@ColumnInfo(name = "name") val name: String = "Monthly Savings",
@ColumnInfo(name = "deadline") val deadline: String? = null,
@ColumnInfo(name = "current_amount") val currentAmount: Double = 0.0
```

---

### 6. Tax year summary export
**Priority: Nice to have**

Nepal's fiscal year runs Shrawan to Ashad (mid-July to mid-July). Add a fiscal year grouping option in `ExportHelper` — exports all transactions between two custom dates into a single CSV formatted for tax reference. Useful for self-employed users or freelancers filing income tax.

**Files:** `ExportHelper.kt`, `BackupScreen.kt`

---

## UX & Interactions

### 1. Voice-to-transaction
**Priority: High value**

"Spent 150 on momo at New Road" → parses amount, category, note. Uses Android's built-in `SpeechRecognizer` — works offline with on-device recognition on most Android devices. No API needed. Faster than any form for common transactions. Voice input is increasingly considered a fundamental finance app feature.

**Files:** `TransactionFormScreen.kt`, new `VoiceParser.kt`

```kotlin
// Voice input trigger
val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
}

// Simple parser for "spent 150 on food at new road"
object VoiceParser {
    fun parse(speech: String): VoiceTransaction? {
        val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
        val amount = amountRegex.find(speech)?.value?.toDoubleOrNull() ?: return null
        val note = speech
            .replace(Regex("""(?i)(spent|paid|bought|purchased)"""), "")
            .replace(amountRegex, "")
            .replace(Regex("""(?i)(on|at|for|rupees|rs|npr)"""), "")
            .trim()
        return VoiceTransaction(amount = amount, note = note)
    }
}
```

---

### 2. Spending streaks & habits
**Priority: Medium value**

Gamification that works for finance. Track positive habits:
- "5-day logging streak"
- "Under budget for 3 weeks in a row"
- "Saved Rs.500 more than last month"

Small achievement badges stored locally in the settings table. Addresses the "forget to log" pain point better than daily notifications alone — positive reinforcement vs. reminders.

**Files:** new `StreakHelper.kt`, `DashboardScreen.kt`, `settings table`

---

### 3. Split transaction
**Priority: Medium value**

A single eSewa payment that covers food + transport. Add a "split" option on the form — one total amount divided across multiple categories. Each split becomes a separate transaction row with the same date and receipt image. No new schema needed — just a batch insert of linked transactions.

**Files:** `TransactionFormScreen.kt`, `FinanceViewModel.kt`

```kotlin
fun addSplitTransaction(
    context: Context,
    totalAmount: Double,
    splits: List<Pair<String, Double>>, // category → amount
    date: String,
    imagePath: String?,
    note: String?
) {
    viewModelScope.launch {
        splits.forEach { (category, amount) ->
            repository.insertTransaction(Transaction(
                type = "expense",
                amount = amount,
                category = category,
                date = date,
                note = note,
                imagePath = imagePath
            ))
        }
    }
}
```

---

### 4. Transaction filter chips
**Priority: Medium value**

The `TransactionsScreen` search bar is wired but there are no filter controls. Add filter chips below the search bar:

- Type: `All / Income / Expense`
- Category: multi-select dropdown
- Date range: start + end date pickers

Filter applied as a derived `StateFlow` in the ViewModel. Transforms the history screen from a scroll list into something genuinely useful.

**Files:** `TransactionsScreen.kt`, `FinanceViewModel.kt`

---

### 5. Expandable transaction cards
**Priority: Medium value**

OCR-extracted fields (`receiverName`, `transactionCode`, `paymentMethod`) are saved to Room but never shown in the UI without opening the edit form. Tap a card to expand inline and reveal digital payment metadata. Uses `AnimatedVisibility` already available in the codebase. No new data needed.

**Files:** `TransactionComponents.kt`, `DashboardScreen.kt`

---

### 6. Onboarding flow
**Priority: Nice to have**

First-time users see a blank dashboard with no guidance. Three onboarding screens:
1. Welcome + pick your primary wallets (eSewa / Khalti / Cash / Bank)
2. Set your first monthly budget
3. Enable the daily reminder

Stored as a flag in the settings table — shows once, never again.

**Files:** new `OnboardingScreen.kt`, `MainActivity.kt`, `settings table`

---

## Data & Analytics

### 1. Spending trend line chart
**Priority: High value**

The donut chart shows what you spent per category this month. A line chart shows how total monthly spending changed over the last 6–12 months — the most valuable view for understanding whether habits are improving. Uses existing Room data across all months. Add to `ReportsScreen` as the primary chart.

**Files:** `ReportsScreen.kt`, `allTransactions StateFlow`

Data preparation:
```kotlin
// Group all transactions by month and sum expenses
val monthlyTotals: Map<String, Double> = allTransactions
    .groupBy { it.date.substring(0, 7) } // "yyyy-MM"
    .mapValues { (_, txs) -> txs.filter { it.type == "expense" }.sumOf { it.amount } }
    .toSortedMap()
```

---

### 2. Month-over-month comparison
**Priority: High value**

For each category: this month vs last month with a % change indicator. A red up-arrow means you spent more. A green down-arrow means you improved. Pure calculation from Room data — no new schema. The single most actionable view for changing spending behaviour.

**Files:** `ReportsScreen.kt`, `FinanceViewModel.kt`

```kotlin
data class CategoryComparison(
    val category: String,
    val thisMonth: Double,
    val lastMonth: Double,
    val percentChange: Double = if (lastMonth == 0.0) 100.0 else ((thisMonth - lastMonth) / lastMonth) * 100
)
```

---

### 3. Time-of-day spending pattern
**Priority: Medium value**

Group transactions by time of day — Morning / Afternoon / Evening / Night. Most people don't realise they spend most impulsively at a specific time. A simple bar chart in `ReportsScreen` grouped by time segments. Requires adding an optional time field to the transaction form.

**Files:** `ReportsScreen.kt`, `Transaction.kt` (add optional time field)

```kotlin
// Add to Transaction:
@ColumnInfo(name = "time") val time: String? = null // "HH:mm" optional

// Group into segments:
fun timeToSegment(time: String?): String = when (time?.substring(0, 2)?.toIntOrNull() ?: -1) {
    in 5..11 -> "Morning"
    in 12..16 -> "Afternoon"
    in 17..20 -> "Evening"
    else -> "Night"
}
```

---

### 4. Average daily spend
**Priority: Medium value**

Monthly expense ÷ days elapsed this month = average daily spend. If you've spent Rs.12,000 in 15 days, you're on track for Rs.24,000 this month. Compare to last month's daily average. Add as a metric card on the Dashboard next to the balance card.

**Files:** `DashboardScreen.kt`, `FinanceViewModel.kt`

```kotlin
val averageDailySpend: StateFlow<Double> = combine(
    totalExpense, selectedMonth
) { expense, month ->
    val daysElapsed = calculateDaysElapsed(month)
    if (daysElapsed > 0) expense / daysElapsed else 0.0
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
```

---

### 5. Smart spending insights (rule-based)
**Priority: Medium value**

No AI required — rule-based insights generated from Room data. Examples:
- "You spent 34% more on food this month vs last month."
- "Your biggest single expense was Rs.3,500 on Shopping."
- "You've been under budget for 3 consecutive months."
- "You typically spend the most on Fridays."

Surface 2–3 auto-generated insight strings in `ReportsScreen`. Pure Kotlin logic, fully offline.

**Files:** new `InsightEngine.kt`, `ReportsScreen.kt`

```kotlin
object InsightEngine {
    fun generate(
        currentMonth: List<Transaction>,
        lastMonth: List<Transaction>,
        budgets: List<Budget>
    ): List<String> {
        val insights = mutableListOf<String>()

        // Biggest expense
        val biggest = currentMonth.filter { it.type == "expense" }.maxByOrNull { it.amount }
        if (biggest != null) {
            insights.add("Your biggest expense this month was ${CurrencyFormatter.format(biggest.amount)} on ${biggest.category}.")
        }

        // Category overspend
        val currentByCategory = currentMonth.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        val lastByCategory = lastMonth.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        currentByCategory.forEach { (cat, current) ->
            val last = lastByCategory[cat] ?: return@forEach
            if (last > 0 && current > last * 1.2) {
                val pct = ((current - last) / last * 100).toInt()
                insights.add("You spent $pct% more on $cat compared to last month.")
            }
        }

        return insights.take(3)
    }
}
```

---

### 6. Nepal fiscal year view
**Priority: Nice to have**

Nepal's fiscal year is Shrawan–Ashad (mid-July to mid-July), not January–December. Add a "Fiscal year" toggle in the month selector that groups data by Nepal's fiscal calendar instead of the Gregorian one. Useful for self-employed users or freelancers aligning expenses to the national budget cycle.

**Files:** `FinanceViewModel.kt`, `DashboardScreen.kt`

---

## Wildcards

### 1. Spending lock / pause mode
**Priority: Unique**

A "spending lock" toggle that blocks adding new expense transactions until manually unlocked. Useful for the last few days of the month when you're over budget — the app becomes a visual barrier to impulse spending. A local flag in the settings table. Inspired by screen time features on Android.

**Files:** `SettingsScreen.kt`, `TransactionFormScreen.kt`, `settings table`

Implementation — check lock state at form open:
```kotlin
val isSpendingLocked by viewModel.isSpendingLocked.collectAsState()

if (isSpendingLocked && type == "expense") {
    // Show lock screen overlay instead of form
    SpendingLockedOverlay(onUnlock = { viewModel.setSpendingLock(false) })
}
```

---

### 2. Spending mood tag
**Priority: Unique**

Tag each transaction with how you felt when spending:

| Tag | Meaning |
|---|---|
| Necessary | Had to spend this |
| Happy | Glad I spent this |
| Regret | Wish I hadn't |
| Impulse | Unplanned, bought on a whim |

Over time, a report shows what % of your spending you regret — the most honest metric for financial wellbeing. A simple enum field on `Transaction`, rendered as a 4-option picker on the form.

**Files:** `Transaction.kt`, `TransactionFormScreen.kt`, `ReportsScreen.kt`, `MIGRATION_3_4`

```kotlin
// Add to Transaction:
@ColumnInfo(name = "mood") val mood: String? = null // "necessary" | "happy" | "regret" | "impulse"
```

---

### 3. 24-hour spending digest notification
**Priority: Unique**

A daily digest notification at 10pm showing:
- Today's total spend
- Top category
- Whether you're on pace for your monthly budget

One tap to review, one tap to log anything missed. More informative than the current "don't forget to log" reminder. Runs entirely offline using existing Room data in `AlarmReceiver`.

**Files:** `AlarmReceiver.kt`, `NotificationScheduler.kt`

```kotlin
// In AlarmReceiver — build contextual notification text
val todayTotal = transactions.filter { it.date == todayStr }
    .sumOf { it.amount }
val topCategory = transactions
    .groupBy { it.category }
    .maxByOrNull { it.value.sumOf { tx -> tx.amount } }?.key

val message = "Spent ${CurrencyFormatter.format(todayTotal)} today" +
    if (topCategory != null) " — mostly on $topCategory." else "."
```

---

### 4. Financial journal
**Priority: Unique**

A freeform daily note attached to dates — "Payday arrived, cleared WorldLink bill, set aside Rs.2,000 for bike service." Not a transaction, just a note. Shown in the calendar view alongside spending data. A simple journal table with date + text.

**Files:** new `Journal.kt`, new `JournalScreen.kt`, `MIGRATION_3_4`

```kotlin
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "created_at") val createdAt: String
)
```

---

### 5. Transaction templates
**Priority: Practical**

Save a transaction as a template — "WorldLink Rs.1,500 monthly" or "Lunch at canteen Rs.120". One tap to create a new transaction pre-filled from the template. Faster than recurring (which auto-inserts) — templates are manual but instant. Stored as a separate `templates` table.

**Files:** new `Template.kt`, `TransactionFormScreen.kt`, `MIGRATION_3_4`

```kotlin
@Entity(tableName = "templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,          // "WorldLink bill"
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "note") val note: String?
)
```

---

### 6. Lock screen widget (Android 13+)
**Priority: Nice to have**

Android 13 supports lock screen widgets. A single-line widget showing today's spend — "Spent Rs.840 today" — visible without unlocking the phone. Even more glanceable than the home screen widget. Uses the same `RemoteViews` infrastructure already in `FinanceWidgetProvider`.

**Files:** `FinanceWidgetProvider.kt`, `widget_info.xml`

---

## Recommended Build Order

### Immediate wins (low effort, high visibility)
1. Average daily spend metric card on Dashboard
2. Expandable transaction cards (show OCR metadata)
3. Transaction filter chips (type + category)
4. Month-over-month comparison in ReportsScreen

### High value features
5. Spending trend line chart (6-month view)
6. Net worth tracker (Assets / Liabilities screen)
7. Voice-to-transaction
8. Smart spending insights (rule-based InsightEngine)

### Medium effort, high impact
9. Multiple savings goals
10. Debt payoff tracker (avalanche + snowball)
11. 50/30/20 rule calculator
12. Envelope budgeting mode

### Unique / differentiating
13. Spending mood tag
14. Spending lock / pause mode
15. 24-hour spending digest notification
16. Transaction templates

### Long term
17. Cash flow calendar
18. Bill & subscription tracker
19. Budget rollover
20. Financial journal
21. Nepal fiscal year view
22. Zero-based budgeting mode
23. Pay-yourself-first mode
24. Lock screen widget
25. Onboarding flow

---

*Research conducted June 2026. Based on global personal finance app market analysis, Nepal digital payment ecosystem landscape (eSewa, IME Khalti, Fonepay), and offline-first Android app best practices.*
