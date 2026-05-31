package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceViewModelTest {

    private lateinit var db: FinanceDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db.financeDao())
        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `addTransaction - income transaction updates netBalance and totalIncome`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        viewModel.addTransaction(
            context = context,
            type = "income",
            amount = 5000.0,
            category = "Salary",
            date = "2026-05-15",
            note = "Monthly salary pay",
            imagePath = null
        )

        // Give flows a brief moment to process
        val txs = viewModel.allTransactions.first()
        assertEquals(1, txs.size)
        assertEquals("income", txs[0].type)
        assertEquals(5000.0, txs[0].amount, 0.001)

        // Check dashboard summary states
        val income = viewModel.totalIncome.first()
        val expense = viewModel.totalExpense.first()
        val net = viewModel.netBalance.first()

        assertEquals(5000.0, income, 0.001)
        assertEquals(0.0, expense, 0.001)
        assertEquals(5000.0, net, 0.001)
    }

    @Test
    fun `addTransaction - expense transaction updates netBalance and totalExpense`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        viewModel.addTransaction(
            context = context,
            type = "expense",
            amount = 1200.0,
            category = "Food & Drinks",
            date = "2026-05-16",
            note = "Team lunch",
            imagePath = null
        )

        val txs = viewModel.allTransactions.first()
        assertEquals(1, txs.size)
        assertEquals("expense", txs[0].type)

        val income = viewModel.totalIncome.first()
        val expense = viewModel.totalExpense.first()
        val net = viewModel.netBalance.first()

        assertEquals(0.0, income, 0.001)
        assertEquals(1200.0, expense, 0.001)
        assertEquals(-1200.0, net, 0.001)
    }

    @Test
    fun `settings - themeMode flow updates reactively on setThemeMode call`() = runTest {
        val initialTheme = viewModel.themeMode.first()
        assertEquals("system", initialTheme)

        viewModel.setThemeMode("dark")
        val updatedTheme = viewModel.themeMode.first()
        assertEquals("dark", updatedTheme)

        viewModel.setThemeMode("light")
        val finalTheme = viewModel.themeMode.first()
        assertEquals("light", finalTheme)
    }

    @Test
    fun `settings - biometricLock flow updates reactively on setAppLockEnabled call`() = runTest {
        val initialLock = viewModel.isAppLockEnabled.first()
        assertFalse(initialLock)

        viewModel.setAppLockEnabled(true)
        val updatedLock = viewModel.isAppLockEnabled.first()
        assertTrue(updatedLock)
    }
}
