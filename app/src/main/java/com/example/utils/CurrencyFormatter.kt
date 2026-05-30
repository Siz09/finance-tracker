package com.example.utils

/**
 * Shared currency formatting for the app.
 * Uses NPR / Rs. format appropriate for Nepal.
 */
object CurrencyFormatter {
    /**
     * Formats a Double as "Rs. 1,500" or "Rs. 1,500.50".
     * Omits trailing ".00" for whole numbers.
     */
    fun format(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "Rs. %,.0f".format(amount)
        } else {
            "Rs. %,.2f".format(amount)
        }
    }
}
