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
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction

@Database(
    entities = [Transaction::class, Budget::class, SavingsGoal::class, AppSetting::class, Account::class],
    version = 4,
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

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
