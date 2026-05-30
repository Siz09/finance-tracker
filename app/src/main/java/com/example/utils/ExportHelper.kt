package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import java.io.File
import java.io.FileWriter

object ExportHelper {
    fun exportToCSV(context: Context, transactions: List<Transaction>): Uri? {
        try {
            val file = File(context.cacheDir, "transactions_export_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)
            writer.append("ID,Type,Amount,Category,Date,Note,ReceiverName,ReceiverId,Remarks,PaymentMethod,ImagePath,CreatedAt\n")
            for (tx in transactions) {
                val noteEscaped = tx.note?.replace("\"", "\"\"") ?: ""
                val receiverNameEsc = tx.receiverName?.replace("\"", "\"\"") ?: ""
                val receiverIdEsc = tx.receiverId?.replace("\"", "\"\"") ?: ""
                val remarksEsc = tx.remarks?.replace("\"", "\"\"") ?: ""
                val paymentMethodEsc = tx.paymentMethod?.replace("\"", "\"\"") ?: ""
                val imagePathEsc = tx.imagePath?.replace("\"", "\"\"") ?: ""
                writer.append("${tx.id},${tx.type},${tx.amount},\"${tx.category}\",${tx.date},\"$noteEscaped\",\"$receiverNameEsc\",\"$receiverIdEsc\",\"$remarksEsc\",\"$paymentMethodEsc\",\"$imagePathEsc\",${tx.createdAt}\n")
            }
            writer.flush()
            writer.close()
            return FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

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
                val noteEscMap = tx.note?.replace("\"", "\\\"") ?: "null"
                val noteVal = if (tx.note == null) "null" else "\"$noteEscMap\""
                val imgVal = if (tx.imagePath == null) "null" else "\"${tx.imagePath.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                
                val recNameVal = if (tx.receiverName == null) "null" else "\"${tx.receiverName.replace("\"", "\\\"")}\""
                val recIdVal = if (tx.receiverId == null) "null" else "\"${tx.receiverId.replace("\"", "\\\"")}\""
                val remVal = if (tx.remarks == null) "null" else "\"${tx.remarks.replace("\"", "\\\"")}\""
                val payVal = if (tx.paymentMethod == null) "null" else "\"${tx.paymentMethod.replace("\"", "\\\"")}\""

                sb.append("    {\n")
                sb.append("      \"id\": ${tx.id},\n")
                sb.append("      \"type\": \"${tx.type}\",\n")
                sb.append("      \"amount\": ${tx.amount},\n")
                sb.append("      \"category\": \"${tx.category}\",\n")
                sb.append("      \"date\": \"${tx.date}\",\n")
                sb.append("      \"note\": $noteVal,\n")
                sb.append("      \"imagePath\": $imgVal,\n")
                sb.append("      \"receiverName\": $recNameVal,\n")
                sb.append("      \"receiverId\": $recIdVal,\n")
                sb.append("      \"remarks\": $remVal,\n")
                sb.append("      \"paymentMethod\": $payVal,\n")
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
                sb.append("      \"monthlyLimit\": ${bg.monthlyLimit},\n")
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
                sb.append("      \"target\": ${sg.target},\n")
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
            e.printStackTrace()
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
}
