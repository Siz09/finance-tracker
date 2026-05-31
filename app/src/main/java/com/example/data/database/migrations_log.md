# Database Migration & Schema Log

This log documents the schema evolution and migration history of the Room database (`finance_tracker_db`) in the Personal Finance Tracker application.

## Schema Version History

### Version 4
- **Date:** May 2026
- **Changes:**
  - Multi-wallet/accounts architecture support.
  - Added new `accounts` table.
  - Added `is_recurring` (Boolean/Int flag), `recurrence_frequency` (String) columns to `transactions` table.
  - Added `account_id` (Integer foreign-key) column to `transactions` table.
- **Migration Path:** `MIGRATION_3_4`

### Version 3
- **Date:** May 2026
- **Changes:**
  - Expanded OCR advanced fonepay/eSewa Nepalese fields support.
  - Added `transactionCode` (String), `processedBy` (String), `purpose` (String), `initiatorName` (String) columns to `transactions` table.
- **Migration Path:** `MIGRATION_2_3`

### Version 2
- **Date:** May 2026
- **Changes:**
  - Standard Nepali digital wallet screenshot field mapping.
  - Added `receiverName` (String), `receiverId` (String), `remarks` (String), `paymentMethod` (String) columns to `transactions` table.
- **Migration Path:** `MIGRATION_1_2`

### Version 1
- **Date:** May 2026
- **Changes:**
  - Initial database setup.
  - Entities: `transactions`, `budgets`, `savings_goals`, `settings`.

---

## Important Rules for Schema Changes

> [!WARNING]
> Failing to follow Room database schema upgrade procedures will result in crashes on client install/startup due to mismatched schemas!

Whenever you add, remove, or modify any database field or table:
1. **Bump Version:** Increment the `version` property in the `@Database` annotation inside `FinanceDatabase.kt` (e.g. from `4` to `5`).
2. **Write Migration:** Create a new `MIGRATION_X_Y` object (inheriting from `Migration(X, Y)`) implementing the exact SQL statements needed for changes.
3. **Register Migration:** Add the new migration object into the `addMigrations(...)` builder list inside the `getDatabase(...)` function in `FinanceDatabase.kt`.
4. **Document Changes:** Update this `migrations_log.md` file with the exact version, changes, date, and SQL statements used.
