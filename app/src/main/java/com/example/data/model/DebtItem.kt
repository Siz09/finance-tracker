package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debt_items")
data class DebtItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val minPayment: Double = 0.0
)
