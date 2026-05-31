package com.example

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import com.example.data.database.FinanceDatabase
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.getDatabase(this) }
    val repository by lazy { FinanceRepository(database.financeDao()) }

    // Application-level scope that survives for the lifetime of the process.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Disable Coil's network observer — this is a fully offline app;
        // the observer fires registerNetworkCallback on every image load unnecessarily.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .networkObserverEnabled(false)
                .build()
        )

        // Orphaned image cleanup: delete receipt files in filesDir/receipts/ that have
        // no matching imagePath in the DB. This prevents the receipts folder growing
        // forever when the user captures a photo then discards the transaction form.
        appScope.launch {
            cleanupOrphanedImages()
        }
    }

    private suspend fun cleanupOrphanedImages() {
        try {
            val receiptsDir = File(filesDir, "receipts")
            if (!receiptsDir.exists()) return

            // Collect all image paths currently referenced by transactions.
            val allTx = database.financeDao().getAllTransactions().firstOrNull() ?: return
            val referencedPaths = allTx.mapNotNull { it.imagePath }.toSet()

            val files = receiptsDir.listFiles() ?: return
            var deletedCount = 0
            for (file in files) {
                if (!referencedPaths.contains(file.absolutePath)) {
                    val deleted = file.delete()
                    if (deleted) deletedCount++
                }
            }
            if (deletedCount > 0) {
                Log.i("FinanceApplication", "Orphaned receipt cleanup: removed $deletedCount file(s).")
            }
        } catch (e: Exception) {
            Log.e("FinanceApplication", "Orphaned image cleanup failed", e)
        }
    }
}
