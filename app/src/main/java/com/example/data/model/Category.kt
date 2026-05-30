package com.example.data.model

data class Category(
    val name: String,
    val icon: String, // Emoji representation
    val type: String // "income" or "expense"
) {
    companion object {
        val EXPENSES = listOf(
            Category("Food & Drinks", "🍔", "expense"),
            Category("Shopping", "🛍️", "expense"),
            Category("Housing & Rent", "🏠", "expense"),
            Category("Transport", "🚗", "expense"),
            Category("Entertainment", "🎬", "expense"),
            Category("Utilities", "⚡", "expense"),
            Category("Health", "🏥", "expense"),
            Category("Education", "📚", "expense"),
            Category("Travel", "✈️", "expense"),
            Category("Other", "🏷️", "expense")
        )

        val INCOMES = listOf(
            Category("Salary", "💰", "income"),
            Category("Freelance", "💻", "income"),
            Category("Gifts", "🎁", "income"),
            Category("Investments", "📈", "income"),
            Category("Other", "💵", "income")
        )

        fun getAll() = EXPENSES + INCOMES

        fun getIcon(name: String, type: String): String {
            return getAll().firstOrNull { 
                it.name.equals(name, ignoreCase = true) && 
                it.type.equals(type, ignoreCase = true) 
            }?.icon ?: "🏷️"
        }
    }
}
