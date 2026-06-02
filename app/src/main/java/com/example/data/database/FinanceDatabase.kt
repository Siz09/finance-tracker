package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.model.Account
import com.example.data.model.AppSetting
import com.example.data.model.Budget
import com.example.data.model.DebtItem
import com.example.data.model.NetWorthItem
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.model.JournalEntry
import com.example.data.model.Bill

@Database(
    entities = [Transaction::class, Budget::class, SavingsGoal::class, AppSetting::class, Account::class, NetWorthItem::class, DebtItem::class, com.example.data.model.TransactionTemplate::class, JournalEntry::class, Bill::class],
    version = 10,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        /**
         * Migration 1 → 2: Added receiverName, receiverId, remarks, paymentMethod columns.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiverName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiverId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN remarks TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT")
            }
        }

        /**
         * Migration 2 → 3: Added transactionCode, processedBy, purpose, initiatorName columns.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionCode TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN processedBy TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN purpose TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN initiatorName TEXT")
            }
        }

        /**
         * Migration 3 → 4:
         * - Adds recurring transaction support (is_recurring, recurrence_frequency)
         * - Adds account_id FK column on transactions
         * - Creates new `accounts` table for wallet management
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recurring transaction fields
                db.execSQL("ALTER TABLE transactions ADD COLUMN is_recurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurrence_frequency TEXT")
                // Account/wallet FK
                db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER")
                // Accounts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '💳',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS net_worth_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        value REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN name TEXT NOT NULL DEFAULT 'General Goal'")
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN savedAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN deadline INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debt_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        paidAmount REAL NOT NULL,
                        interestRate REAL NOT NULL,
                        minPayment REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN mood TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transaction_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `note` TEXT)"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN rolloverAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE budgets ADD COLUMN rolloverEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE savings_goals ADD COLUMN autoCreditEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN time TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN parentTransactionId INTEGER DEFAULT NULL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `content` TEXT NOT NULL, `mood` TEXT)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDate` TEXT NOT NULL, `isPaid` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
