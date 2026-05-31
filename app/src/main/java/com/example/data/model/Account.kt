package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a payment wallet/account (Cash, eSewa, Khalti, Bank, etc.)
 * Transactions can optionally reference an account via accountId.
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,       // e.g. "eSewa", "Cash", "NIC Asia"
    val type: String,       // "digital" | "bank" | "cash"
    val emoji: String = "💳",
    val createdAt: Long = System.currentTimeMillis()
)
