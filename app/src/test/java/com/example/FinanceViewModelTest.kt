package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        FinanceDatabase.setTestInstance(db)
        viewModel = FinanceViewModel(context as android.app.Application)
    }

    @After
    fun tearDown() {
        try {
            val superClass = viewModel.javaClass.superclass
            val vmClass = superClass.superclass
            val method = vmClass.getDeclaredMethod("onCleared")
            method.isAccessible = true
            method.invoke(viewModel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        db.close()
        FinanceDatabase.setTestInstance(null)
    }
    private suspend fun waitTransactions(expectedSize: Int) {
        val start = System.currentTimeMillis()
        while (viewModel.allTransactions.value.size != expectedSize && System.currentTimeMillis() - start < 3000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(50)
        }
    }

    private suspend fun waitThemeMode(expected: String) {
        val start = System.currentTimeMillis()
        while (viewModel.themeMode.value != expected && System.currentTimeMillis() - start < 3000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(50)
        }
    }

    @Test
    fun `addTransaction - income transaction updates netBalance and totalIncome`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.allTransactions.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalIncome.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalExpense.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.netBalance.collect {}
        }

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

        waitTransactions(1)

        val txs = viewModel.allTransactions.value
        assertEquals(1, txs.size)
        assertEquals("income", txs[0].type)
        assertEquals(5000.0, txs[0].amount, 0.001)

        // Check dashboard summary states
        val income = viewModel.totalIncome.value
        val expense = viewModel.totalExpense.value
        val net = viewModel.netBalance.value

        assertEquals(5000.0, income, 0.001)
        assertEquals(0.0, expense, 0.001)
        assertEquals(5000.0, net, 0.001)

        org.robolectric.shadows.ShadowLooper.getShadowMainLooper().runToEndOfTasks()
    }

    @Test
    fun `addTransaction - expense transaction updates netBalance and totalExpense`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.allTransactions.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalIncome.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.totalExpense.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.netBalance.collect {}
        }

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

        waitTransactions(1)

        val txs = viewModel.allTransactions.value
        assertEquals(1, txs.size)
        assertEquals("expense", txs[0].type)

        val income = viewModel.totalIncome.value
        val expense = viewModel.totalExpense.value
        val net = viewModel.netBalance.value

        assertEquals(0.0, income, 0.001)
        assertEquals(1200.0, expense, 0.001)
        assertEquals(-1200.0, net, 0.001)

        org.robolectric.shadows.ShadowLooper.getShadowMainLooper().runToEndOfTasks()
    }

    @Test
    fun `settings - themeMode flow updates reactively on setThemeMode call`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.themeMode.collect {}
        }

        waitThemeMode("system")
        val initialTheme = viewModel.themeMode.value
        assertEquals("system", initialTheme)

        viewModel.setThemeMode("dark")
        waitThemeMode("dark")
        val updatedTheme = viewModel.themeMode.value
        assertEquals("dark", updatedTheme)

        viewModel.setThemeMode("light")
        waitThemeMode("light")
        val finalTheme = viewModel.themeMode.value
        assertEquals("light", finalTheme)

        org.robolectric.shadows.ShadowLooper.getShadowMainLooper().runToEndOfTasks()
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
