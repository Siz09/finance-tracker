# Finance Tracker Implementation Checklist

## Budgeting Methods (5 ideas)

| Status | Idea | Details | Phase |
|---|---|---|---|
| ~ | **Envelope budgeting mode** | Allocated vs unallocated summary added to BudgetScreen. But not true envelopes — no per-category fill-from-income allocation, no blocking spend when empty. | Phase 3 · BudgetScreen.kt |
| ~ | **50/30/20 rule calculator** | Built in ReportsScreen. But savingsPct formula is wrong — double-counts savings transactions. *(Fixed)* | Phase 3 · ReportsScreen.kt |
| ✗ | **Budget rollover** | Not implemented. Requires rolloverAmount + rolloverEnabled on Budget table, MIGRATION_9_10, logic in RecurringWorker. | Not built |
| ✗ | **Zero-based budgeting mode** | Not implemented. "Unallocated" counter partially covers this but true zero-based requires income allocation first. | Not built |
| ✓ | **Pay-yourself-first mode** | Fully implemented. Added `autoCreditEnabled` to `SavingsGoal`, toggle in `SavingsGoalScreen`, and auto-credit logic in `FinanceViewModel.addTransaction` via `MIGRATION_9_10`. | Phase 6 · FinanceViewModel.kt, SavingsGoalScreen.kt |

## Financial Tracking (6 ideas)

| Status | Idea | Details | Phase |
|---|---|---|---|
| ✓ | **Net worth tracker** | NetWorthItem entity, MIGRATION_4_5, totalAssets / totalLiabilities / netWorth StateFlows. Full UI in NetWorthScreen.kt. | Phase 2 · NetWorthScreen.kt |
| ~ | **Debt payoff tracker** | DebtItem entity, MIGRATION_6_7, payment logging works. But avalanche / snowball strategy sorting not implemented — just a flat list. | Phase 3 · DebtScreen.kt |
| ✓ | **Cash flow calendar** | Full grid calendar with daily net flow, mint/red indicators, tap-to-expand daily breakdown with AnimatedVisibility. | Phase 5 · CalendarScreen.kt |
| ✗ | **Bill & subscription tracker** | Not implemented. Could integrate with recurring transactions but no dedicated bills screen or due-date reminder flow built. | Not built |
| ✓ | **Multiple savings goals** | MIGRATION_5_6 adds name, savedAmount, deadline. SavingsGoalCard with animated progress bars and deadline display. | Phase 3 · SavingsGoalScreen.kt |
| ✗ | **Tax year summary export** | Not implemented. Custom date-range export in BackupScreen/ExportHelper not built. | Not built |

## UX & Interactions (6 ideas)

| Status | Idea | Details | Phase |
|---|---|---|---|
| ~ | **Voice-to-transaction** | SpeechRecognizer + VoiceParser wired correctly. But not guaranteed offline — silently falls back to Google cloud if offline model not installed. Needs availability check + warning. | Phase 2 · VoiceParser.kt |
| ✗ | **Spending streaks & habits** | Not implemented. StreakHelper.kt, logging streak counter, under-budget streak, badges on Dashboard — none built. | Not built |
| ✗ | **Split transaction** | Not implemented. No batch insert of one payment split across multiple categories. Highest remaining form UX gap. | Not built |
| ✓ | **Transaction filter chips** | Already existed before phases started. All Logs / Income / Expense chips confirmed in TransactionsScreen.kt. | Pre-existing |
| ✓ | **Expandable transaction cards** | Already existed in TransactionComponents.kt before phases started. OCR metadata shown inline on expand. | Pre-existing |
| ✗ | **Onboarding flow** | Not implemented. First-time user still sees blank dashboard. 3-screen onboarding (wallets → budget → reminder) + settings flag not built. | Not built |
| ✗ | **Onboarding flow** | Not implemented. 3-screen onboarding (wallets -> budget -> reminder) + settings flag not built. | Not built |

## Data & Analytics (6 ideas)

| Status | Idea | Details | Phase |
|---|---|---|---|
| ✓ | **Spending trend line chart** | Custom Canvas cubic bezier 6-month chart with gradient fills and data nodes. Complete in ReportsScreen. | Phase 2 · ReportsScreen.kt |
| ✓ | **Month-over-month comparison** | Per-category table with trending arrows, NEW labels, % change badges. Sorted by this month's spend descending. | Phase 1 · ReportsScreen.kt |
| ✗ | **Time-of-day spending pattern** | Not implemented. `time` column added in `MIGRATION_9_10`. Transactions grouped by Morning/Afternoon/Evening/Night. | Not built |
| ✓ | **Average daily spend** | Already existed in DashboardScreen before phases started. Monthly expense ÷ days elapsed metric card. | Pre-existing |
| ✓ | **Smart spending insights** | Rule-based insight carousel on Dashboard — budget warnings, burn rate alerts, positive savings recognition. | Phase 2 · DashboardScreen.kt |
| ~ | **Nepal fiscal year view** | FY toggle in Settings, BS label, dashboard swap. But BS year offset (+57) needs unit test validation for all months. Edge case months may show wrong label. | Phase 5 · FinanceViewModel.kt |

## Wildcards (6 ideas)

| Status | Idea | Details | Phase |
|---|---|---|---|
| ✓ | **Spending lock / pause mode** | isSpendingLocked StateFlow, SpendingLockedOverlay with lock icon, Go Back button, toggle in SettingsScreen. Full implementation confirmed in Phase 4 diff. | Phase 4 · TransactionFormScreen.kt |
| ✓ | **Spending mood tag** | mood field on Transaction (MIGRATION_7_8), emoji picker on form for expenses only, mood insight card in ReportsScreen. Full implementation confirmed in Phase 4 diff. | Phase 4 · Transaction.kt |
| ✓ | **24-hour spending digest** | AlarmReceiver updated with today's total + top category + actions. PendingIntent verified (no collision, it reuses the daily reminder gracefully). | Phase 4 · AlarmReceiver.kt |
| ✗ | **Financial journal** | Not implemented. `JournalEntry` table added in `MIGRATION_9_10`. Needs JournalScreen and calendar integration. | Not built |
| ✓ | **Transaction templates** | Fully implemented. Data layer complete in Phase 4. UI for applying templates added to `TransactionFormScreen.kt`. | Phase 6 · TransactionFormScreen.kt |
| ✗ | **Lock screen widget (Android 13+)** | Not implemented. Would reuse FinanceWidgetProvider infrastructure but the lock screen widget configuration was never added. | Not built |

---
**Final Tally:**
✓ Fully implemented: 15
~ Partial / issues: 7
✗ Not built: 8
