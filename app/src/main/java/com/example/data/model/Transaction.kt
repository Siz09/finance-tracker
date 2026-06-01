package com.example.data.model

import androidx.room.ColumnInfo
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
    // Recurring transaction fields (migration v3 → v4)
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurrence_frequency") val recurrenceFrequency: String? = null, // "daily"|"weekly"|"monthly"
    // Account / wallet FK (migration v3 → v4)
    @ColumnInfo(name = "account_id") val accountId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Phase 4 fields
    @ColumnInfo(name = "mood") val mood: String? = null // e.g. "Necessary", "Happy", "Regret", "Impulse"
)
