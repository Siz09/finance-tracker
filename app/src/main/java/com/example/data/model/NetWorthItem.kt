package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single asset or liability entry in the Net Worth Tracker.
 * type = "asset" or "liability"
 */
@Entity(tableName = "net_worth_items")
data class NetWorthItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: Double,
    val type: String,          // "asset" | "liability"
    val category: String,      // e.g. "Cash", "Property", "Loan", "Credit Card"
    val createdAt: Long = System.currentTimeMillis()
)
