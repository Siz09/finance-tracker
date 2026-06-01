package com.example.utils

/**
 * Rule-based NLP parser for Voice-to-Transaction.
 * No internet or API key needed — 100% offline regex matching.
 *
 * Supported phrases:
 *   "spent 500 on food"
 *   "paid 1200 for transport"
 *   "added 55000 salary income"
 *   "received 3000 freelance"
 *   "bought groceries for 800"
 */
object VoiceParser {

    data class ParsedTransaction(
        val amount: Double?,
        val type: String,       // "expense" | "income"
        val category: String,
        val note: String?
    )

    // Keywords that indicate income
    private val incomeKeywords = listOf(
        "salary", "income", "received", "receive", "earned", "earn",
        "bonus", "freelance", "dividend", "refund", "allowance", "added", "add"
    )

    // Expense trigger words
    private val expenseKeywords = listOf(
        "spent", "spend", "paid", "pay", "bought", "buy",
        "purchased", "purchase", "cost", "charged"
    )

    // Category keyword map — order matters (first match wins)
    private val categoryMap = linkedMapOf(
        "Food & Drinks" to listOf("food", "lunch", "dinner", "breakfast", "coffee", "tea",
            "snack", "restaurant", "eat", "drink", "momo", "pizza", "burger", "dal"),
        "Groceries" to listOf("grocery", "groceries", "vegetable", "fruit", "supermarket", "bazar", "market"),
        "Transport" to listOf("transport", "bus", "taxi", "uber", "cab", "fuel",
            "petrol", "diesel", "fare", "auto", "bike", "ride"),
        "Shopping" to listOf("shopping", "clothes", "shirt", "shoes", "dress",
            "amazon", "daraz", "mall", "store"),
        "Health" to listOf("medicine", "medical", "doctor", "hospital", "pharmacy",
            "health", "clinic", "prescription"),
        "Entertainment" to listOf("movie", "cinema", "concert", "game", "netflix",
            "subscription", "entertainment", "fun"),
        "Education" to listOf("school", "college", "university", "course", "tuition",
            "book", "education", "study", "fee"),
        "Bills & Utilities" to listOf("electricity", "water", "internet", "wifi",
            "bill", "utility", "phone", "recharge", "top-up"),
        "Salary" to listOf("salary", "wage", "stipend"),
        "Freelance" to listOf("freelance", "project", "client", "gig"),
        "Investment" to listOf("investment", "invest", "stock", "share", "mutual fund", "crypto"),
        "Bonus" to listOf("bonus", "incentive", "award", "prize"),
        "Other" to listOf()     // Catch-all
    )

    fun parse(speech: String): ParsedTransaction {
        val lower = speech.lowercase().trim()

        // 1. Extract the amount — look for a number in the string
        val amountRegex = Regex("""(\d+(?:[.,]\d+)?)""")
        val amountStr = amountRegex.find(lower)?.value?.replace(",", "")
        val amount = amountStr?.toDoubleOrNull()

        // 2. Determine transaction type
        val isIncome = incomeKeywords.any { lower.contains(it) }
        val isExpense = expenseKeywords.any { lower.contains(it) }
        val type = when {
            isIncome && !isExpense -> "income"
            isExpense -> "expense"
            else -> "expense" // default
        }

        // 3. Detect category
        val category = detectCategory(lower, type)

        // 4. Build a clean note from the spoken text (strip the amount)
        val note = amountStr?.let { lower.replace(it, "").trim()
            .replace(Regex("\\s+"), " ")
            .replaceFirstChar { c -> c.uppercase() }
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            category = category,
            note = if (note.isNullOrBlank()) null else note
        )
    }

    private fun detectCategory(lower: String, type: String): String {
        for ((category, keywords) in categoryMap) {
            if (keywords.any { lower.contains(it) }) {
                return category
            }
        }
        // Fallback defaults by type
        return if (type == "income") "Salary" else "Food & Drinks"
    }
}
