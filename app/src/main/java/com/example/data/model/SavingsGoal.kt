package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "General Goal",
    val target: Double,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val month: String = "", // Legacy field, kept for schema compatibility
    val autoCreditEnabled: Boolean = false
)
