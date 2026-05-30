package com.example

import android.app.Application
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.getDatabase(this) }
    val repository by lazy { FinanceRepository(database.financeDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
