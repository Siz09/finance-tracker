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
import java.util.Calendar
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
    fun `nepal fiscal year - edge cases for label offset`() {
        // Test July 15, 2026 -> FY 2025 (BS 2082/83)
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15) // Note: Calendar.JULY is 6 (0-indexed)
        }
        val label1 = viewModel.getNepalFiscalYearLabel(cal1)
        assertEquals("FY 2082/83 BS", label1)

        // Test July 16, 2026 -> FY 2026 (BS 2083/84)
        val cal2 = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 16)
        }
        val label2 = viewModel.getNepalFiscalYearLabel(cal2)
        assertEquals("FY 2083/84 BS", label2)
        
        // Test January 1, 2027 -> FY 2026 (BS 2083/84)
        val cal3 = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 1) // Note: Calendar.JANUARY is 0
        }
        val label3 = viewModel.getNepalFiscalYearLabel(cal3)
        assertEquals("FY 2083/84 BS", label3)
        
        // Test August 1, 2026 -> FY 2026 (BS 2083/84)
        val cal4 = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 1)
        }
        val label4 = viewModel.getNepalFiscalYearLabel(cal4)
        assertEquals("FY 2083/84 BS", label4)
    }
}
