package com.example

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository
import com.example.notifications.RecurringWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("unused") // Referenced by android:name in AndroidManifest.xml
class KharchaApp : Application() {

    val database by lazy { FinanceDatabase.getDatabase(this) }
    val repository by lazy { FinanceRepository(database.financeDao()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Disable Coil's network observer to prevent unnecessary wake-locks
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .networkObserverEnabled(false)
                .build()
        )

        // Background: clean up orphaned receipt images
        appScope.launch {
            cleanupOrphanedImages()
        }

        // Schedule recurring transaction processing via WorkManager
        scheduleRecurringWorker()
    }

    private fun scheduleRecurringWorker() {
        try {
            val recurringRequest = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recurring_transactions_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest
            )
        } catch (e: Exception) {
            Log.e("KharchaApp", "Failed to schedule WorkManager task", e)
        }
    }

    private suspend fun cleanupOrphanedImages() {
        try {
            val receiptsDir = File(filesDir, "receipts")
            if (!receiptsDir.exists()) return
            val allTx = database.financeDao().getAllTransactions().firstOrNull() ?: return
            val referencedPaths = allTx.mapNotNull { it.imagePath }.toSet()
            val files = receiptsDir.listFiles() ?: return
            var deletedCount = 0
            for (file in files) {
                if (!referencedPaths.contains(file.absolutePath)) {
                    if (file.delete()) deletedCount++
                }
            }
            if (deletedCount > 0) {
                Log.i("KharchaApp", "Orphaned receipt cleanup: removed $deletedCount file(s).")
            }
        } catch (e: Exception) {
            Log.e("KharchaApp", "Orphaned image cleanup failed", e)
        }
    }
}
