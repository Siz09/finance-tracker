package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import java.io.File
import java.io.FileWriter

data class ImportResult(
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val savingsGoals: List<SavingsGoal>,
    val errors: List<String>
)

object ExportHelper {


    fun exportToJSON(
        context: Context,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        savingsGoals: List<SavingsGoal>
    ): Uri? {
        try {
            val file = File(context.cacheDir, "finance_data_export_${System.currentTimeMillis()}.json")
            val writer = FileWriter(file)
            
            val sb = StringBuilder()
            sb.append("{\n")
            
            // Transactions
            sb.append("  \"transactions\": [\n")
            for ((index, tx) in transactions.withIndex()) {
                // Fix #15: properly escape all JSON control characters in string fields.
                val noteVal = if (tx.note == null) "null" else "\"${jsonEscape(tx.note)}\""
                // Fix #6: export only the filename, not the full internal path.
                // On import the app resolves it against its own receipts directory.
                val imgVal = if (tx.imagePath == null) "null"
                             else "\"${jsonEscape(File(tx.imagePath).name)}\""

                val recNameVal  = if (tx.receiverName == null)     "null" else "\"${jsonEscape(tx.receiverName)}\""
                val recIdVal    = if (tx.receiverId == null)        "null" else "\"${jsonEscape(tx.receiverId)}\""
                val remVal      = if (tx.remarks == null)           "null" else "\"${jsonEscape(tx.remarks)}\""
                val payVal      = if (tx.paymentMethod == null)     "null" else "\"${jsonEscape(tx.paymentMethod)}\""
                val txnCodeVal  = if (tx.transactionCode == null)   "null" else "\"${jsonEscape(tx.transactionCode)}\""
                val procByVal   = if (tx.processedBy == null)       "null" else "\"${jsonEscape(tx.processedBy)}\""
                val purposeVal  = if (tx.purpose == null)           "null" else "\"${jsonEscape(tx.purpose)}\""
                val initNameVal = if (tx.initiatorName == null)     "null" else "\"${jsonEscape(tx.initiatorName)}\""
                // Always export amount with 2 decimal places for precision and interoperability
                val amountStr = String.format("%.2f", tx.amount)

                sb.append("    {\n")
                sb.append("      \"id\": ${tx.id},\n")
                sb.append("      \"type\": \"${tx.type}\",\n")
                sb.append("      \"amount\": $amountStr,\n")
                sb.append("      \"category\": \"${tx.category}\",\n")
                sb.append("      \"date\": \"${tx.date}\",\n")
                sb.append("      \"note\": $noteVal,\n")
                sb.append("      \"imagePath\": $imgVal,\n")
                sb.append("      \"receiverName\": $recNameVal,\n")
                sb.append("      \"receiverId\": $recIdVal,\n")
                sb.append("      \"remarks\": $remVal,\n")
                sb.append("      \"paymentMethod\": $payVal,\n")
                sb.append("      \"transactionCode\": $txnCodeVal,\n")
                sb.append("      \"processedBy\": $procByVal,\n")
                sb.append("      \"purpose\": $purposeVal,\n")
                sb.append("      \"initiatorName\": $initNameVal,\n")
                sb.append("      \"createdAt\": ${tx.createdAt}\n")
                if (index < transactions.size - 1) {
                    sb.append("    },\n")
                } else {
                    sb.append("    }\n")
                }
            }
            sb.append("  ],\n")

            // Budgets
            sb.append("  \"budgets\": [\n")
            for ((index, bg) in budgets.withIndex()) {
                sb.append("    {\n")
                sb.append("      \"id\": ${bg.id},\n")
                sb.append("      \"category\": \"${bg.category}\",\n")
                sb.append("      \"monthlyLimit\": ${String.format("%.2f", bg.monthlyLimit)},\n")
                sb.append("      \"month\": \"${bg.month}\"\n")
                if (index < budgets.size - 1) {
                    sb.append("    },\n")
                } else {
                    sb.append("    }\n")
                }
            }
            sb.append("  ],\n")

            // Savings Goals
            sb.append("  \"savingsGoals\": [\n")
            for ((index, sg) in savingsGoals.withIndex()) {
                sb.append("    {\n")
                sb.append("      \"id\": ${sg.id},\n")
                sb.append("      \"target\": ${String.format("%.2f", sg.target)},\n")
                sb.append("      \"month\": \"${sg.month}\"\n")
                if (index < savingsGoals.size - 1) {
                    sb.append("    },\n")
                } else {
                    sb.append("    }\n")
                }
            }
            sb.append("  ]\n")
            sb.append("}")

            writer.write(sb.toString())
            writer.flush()
            writer.close()
            return FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        } catch (e: Exception) {
            Log.e("ExportHelper", "exportToJSON failed", e)
            return null
        }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Data"))
    }

    // ── JSON Import / Restore ─────────────────────────────────────────────────

    /**
     * Parses a JSON string exported by [exportToJSON] and returns an [ImportResult].
     * Uses manual JSON tokenisation — no external library needed.
     * Returns [ImportResult.errors] non-empty on partial parse failures.
     */
    fun parseImportedJSON(jsonContent: String): ImportResult {
        // Fix #8: guard against OOM from very large files before any heap allocation.
        if (jsonContent.length > 10_000_000) { // 10 MB limit
            return ImportResult(
                emptyList(), emptyList(), emptyList(),
                listOf("Import file too large. Maximum supported size is 10 MB.")
            )
        }

        val transactions = mutableListOf<Transaction>()
        val budgets      = mutableListOf<Budget>()
        val savingsGoals = mutableListOf<SavingsGoal>()
        val errors       = mutableListOf<String>()

        try {
            // Validate top-level structure
            if (!jsonContent.trim().startsWith("{")) {
                return ImportResult(emptyList(), emptyList(), emptyList(),
                    listOf("Invalid JSON: expected a JSON object at the root level."))
            }

            // ── Transactions ──────────────────────────────────────────────────
            val txArray = extractJsonArray(jsonContent, "transactions")
            txArray?.let { arr ->
                val objects = splitJsonObjects(arr)
                objects.forEachIndexed { idx, obj ->
                    try {
                        val id            = extractInt(obj, "id") ?: 0
                        val type          = extractString(obj, "type") ?: return@forEachIndexed
                        val amount        = extractDouble(obj, "amount") ?: return@forEachIndexed
                        val category      = extractString(obj, "category") ?: ""
                        val date          = extractString(obj, "date") ?: ""
                        val note          = extractNullableString(obj, "note")
                        val imagePath     = extractNullableString(obj, "imagePath")
                        val receiverName  = extractNullableString(obj, "receiverName")
                        val receiverId    = extractNullableString(obj, "receiverId")
                        val remarks       = extractNullableString(obj, "remarks")
                        val paymentMethod = extractNullableString(obj, "paymentMethod")
                        val txnCode       = extractNullableString(obj, "transactionCode")
                        val processedBy   = extractNullableString(obj, "processedBy")
                        val purpose       = extractNullableString(obj, "purpose")
                        val initiatorName = extractNullableString(obj, "initiatorName")
                        val createdAt     = extractLong(obj, "createdAt") ?: System.currentTimeMillis()

                        if (type != "income" && type != "expense") {
                            errors.add("Transaction[$idx]: unknown type '$type', skipping.")
                            return@forEachIndexed
                        }
                        if (amount <= 0) {
                            errors.add("Transaction[$idx]: amount must be > 0, skipping.")
                            return@forEachIndexed
                        }

                        transactions.add(Transaction(
                            id = id,
                            type = type,
                            amount = amount,
                            category = category,
                            date = date,
                            note = note,
                        // imagePath is intentionally set to null on import.
                        // The export writes only the filename (not the full internal path)
                        // so the path cannot be resolved on a different device or after reinstall.
                        // Transaction data is fully restored; receipt thumbnails are not portable.
                        imagePath = null,
                            receiverName = receiverName,
                            receiverId = receiverId,
                            remarks = remarks,
                            paymentMethod = paymentMethod,
                            transactionCode = txnCode,
                            processedBy = processedBy,
                            purpose = purpose,
                            initiatorName = initiatorName,
                            createdAt = createdAt
                        ))
                    } catch (e: Exception) {
                        errors.add("Transaction[$idx]: parse error — ${e.message}")
                    }
                }
            } ?: errors.add("No 'transactions' array found in JSON.")

            // ── Budgets ───────────────────────────────────────────────────────
            val bgArray = extractJsonArray(jsonContent, "budgets")
            bgArray?.let { arr ->
                val objects = splitJsonObjects(arr)
                objects.forEachIndexed { idx, obj ->
                    try {
                        val id    = extractInt(obj, "id") ?: 0
                        val cat   = extractString(obj, "category") ?: return@forEachIndexed
                        val limit = extractDouble(obj, "monthlyLimit") ?: return@forEachIndexed
                        val month = extractString(obj, "month") ?: return@forEachIndexed
                        budgets.add(Budget(id = id, category = cat, monthlyLimit = limit, month = month))
                    } catch (e: Exception) {
                        errors.add("Budget[$idx]: parse error — ${e.message}")
                    }
                }
            }

            // ── Savings Goals ─────────────────────────────────────────────────
            val sgArray = extractJsonArray(jsonContent, "savingsGoals")
            sgArray?.let { arr ->
                val objects = splitJsonObjects(arr)
                objects.forEachIndexed { idx, obj ->
                    try {
                        val id     = extractInt(obj, "id") ?: 0
                        val target = extractDouble(obj, "target") ?: return@forEachIndexed
                        val month  = extractString(obj, "month") ?: return@forEachIndexed
                        savingsGoals.add(SavingsGoal(id = id, target = target, month = month))
                    } catch (e: Exception) {
                        errors.add("SavingsGoal[$idx]: parse error — ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            errors.add("Fatal JSON parse error: ${e.message}")
        }

        return ImportResult(transactions, budgets, savingsGoals, errors)
    }

    /** Returns total bytes occupied by files in filesDir/receipts/. */
    fun getReceiptsDirSizeBytes(context: Context): Long {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576     -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024         -> String.format("%.1f KB", bytes / 1_024.0)
            else                   -> "$bytes B"
        }
    }

    // ── Private JSON parsing helpers ──────────────────────────────────────────

    /**
     * Properly escapes a string for embedding in a JSON value.
     * Handles the full set of control characters required by RFC 8259:
     *   backslash, double-quote, newline, carriage-return, tab, backspace.
     */
    private fun jsonEscape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")

    /** Extracts the content of a JSON array for the given key. */
    private fun extractJsonArray(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*\[""")
        val match = pattern.find(json) ?: return null
        val start = match.range.last // position of '['
        var depth = 0
        var inString = false
        for (i in start until json.length) {
            when {
                json[i] == '"' && (i == 0 || json[i - 1] != '\\') -> inString = !inString
                !inString && json[i] == '[' -> depth++
                !inString && json[i] == ']' -> {
                    depth--
                    if (depth == 0) return json.substring(start + 1, i)
                }
            }
        }
        return null
    }

    /** Splits a JSON array body (between [ and ]) into individual object strings. */
    private fun splitJsonObjects(arrayBody: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var inString = false
        var start = -1
        for (i in arrayBody.indices) {
            when {
                arrayBody[i] == '"' && (i == 0 || arrayBody[i - 1] != '\\') -> inString = !inString
                !inString && arrayBody[i] == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                !inString && arrayBody[i] == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        objects.add(arrayBody.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractString(obj: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(obj)?.groupValues?.get(1)
    }

    private fun extractNullableString(obj: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*(?:"((?:[^"\\]|\\.)*)"|null)""")
        val m = regex.find(obj) ?: return null
        return if (m.groupValues[1].isEmpty() && m.value.contains("null")) null
        else m.groupValues[1].ifEmpty { null }
    }

    private fun extractDouble(obj: String, key: String): Double? {
        val regex = Regex(""""$key"\s*:\s*(-?[\d.]+)""")
        return regex.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractInt(obj: String, key: String): Int? {
        val regex = Regex(""""$key"\s*:\s*(\d+)""")
        return regex.find(obj)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(obj: String, key: String): Long? {
        val regex = Regex(""""$key"\s*:\s*(\d+)""")
        return regex.find(obj)?.groupValues?.get(1)?.toLongOrNull()
    }
}

