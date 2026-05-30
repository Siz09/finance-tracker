package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.model.AppSetting
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction

@Database(
    entities = [Transaction::class, Budget::class, SavingsGoal::class, AppSetting::class],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        /**
         * Migration 1 → 2: Added receiverName, receiverId, remarks, paymentMethod columns to transactions.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiverName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiverId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN remarks TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionCode TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN processedBy TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN purpose TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN initiatorName TEXT")
            }
        }

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
