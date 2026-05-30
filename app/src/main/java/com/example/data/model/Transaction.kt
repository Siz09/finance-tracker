package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "income" or "expense"
    val amount: Double,
    val category: String,
    val date: String, // "YYYY-MM-DD"
    val note: String?,
    val imagePath: String?,
    val receiverName: String? = null,
    val receiverId: String? = null,
    val remarks: String? = null,
    val paymentMethod: String? = null,
    val transactionCode: String? = null,  // e.g. "16D37HB"
    val processedBy: String? = null,       // e.g. phone number "9844296224"
    val purpose: String? = null,           // e.g. "Personal Use"
    val initiatorName: String? = null,     // e.g. "Sijan Maharjan"
    val createdAt: Long = System.currentTimeMillis()
)

