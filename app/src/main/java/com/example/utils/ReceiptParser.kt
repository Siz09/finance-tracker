package com.example.utils

data class ParsedReceipt(
    val amount: Double?,
    val date: String?,
    val merchant: String?,
    val receiverName: String? = null,
    val receiverId: String? = null,
    val remarks: String? = null,
    val paymentMethod: String? = null
)

object ReceiptParser {
    /**
     * Amount regex — matches:
     *  - Keywords (TOTAL, AMOUNT, etc.) followed by whole or decimal number
     *  - NPR/RS prefix with whole or decimal: "NPR 1500", "Rs. 500", "Rs 250.50"
     *  - Amounts with commas as thousands separator: "1,500" "1,500.00"
     */
    private val amountKeywordRegex = Regex(
        """(?:TOTAL|AMOUNT|NET|DUE|PAID|RS\.?|NPR|NRS)\s*[:=]?\s*([\d,]+(?:[.,]\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val dateRegex = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})|(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")

    private val receiverRegex = Regex(
        """(?:RECEIVED BY|TO|MERCHANT|PAID TO|TRANSFER TO)\s*[:=]?\s*([A-Z0-9\s.]+)\b""",
        RegexOption.IGNORE_CASE
    )
    private val receiverIdRegex = Regex("""(?:ID|A\/C|MOBILE|NUMBER)\s*[:=]?\s*(\d{5,15})""", RegexOption.IGNORE_CASE)
    private val remarksRegex = Regex("""(?:REMARKS|DESCRIPTION|FOR|PURPOSE)\s*[:=]?\s*([A-Z0-9\s.,/]+)\b""", RegexOption.IGNORE_CASE)
    private val paymentMethodRegex = Regex(
        """(?:PAYMENT|MODE|METHOD|VIA)\s*[:=]?\s*(ESEWA|FONEPAY|BANK|CASH|KHALTI|CONNECTIPS)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().filter { it.isNotBlank() }

        // 1. Merchant / Receiver Name
        val merchant = lines.firstOrNull()?.trim()
        val receiverMatch = receiverRegex.find(text)
        val receiverName = receiverMatch?.groupValues?.get(1)?.trim() ?: merchant

        // 2. Amount — try keyword-anchored match first
        var amount: Double? = null
        val keywordMatch = amountKeywordRegex.find(text)
        if (keywordMatch != null) {
            // Remove thousands commas then parse
            amount = keywordMatch.groupValues[1].replace(",", "").replace(",", ".").toDoubleOrNull()
        }

        // Fallback: find all plausible numeric amounts (avoid phone numbers by capping digits)
        if (amount == null) {
            val candidates = Regex("""(?<!\d)(\d{1,6}(?:[.,]\d{2})?)(?!\d)""")
                .findAll(text)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .filter { it > 0 }
                .toList()
            // Pick the largest value that's reasonably an amount (not a year/date component)
            amount = candidates.filter { it < 10_000_000 }.maxOrNull()
        }

        // 3. Date → enforce YYYY-MM-DD
        val dateMatch = dateRegex.find(text)
        var formattedDate: String? = null
        if (dateMatch != null) {
            val g1 = dateMatch.groupValues[1]
            val g2 = dateMatch.groupValues[2]
            val g3 = dateMatch.groupValues[3]
            val g4 = dateMatch.groupValues[4]
            val g5 = dateMatch.groupValues[5]
            val g6 = dateMatch.groupValues[6]

            formattedDate = if (g4.isNotEmpty()) {
                // Already YYYY-MM-DD or YYYY/MM/DD
                "${g4}-${g5.padStart(2, '0')}-${g6.padStart(2, '0')}"
            } else {
                var year = g3
                if (year.length == 2) year = "20$year"
                // Treat as DD-MM-YYYY (most common on Nepali receipts)
                "${year}-${g2.padStart(2, '0')}-${g1.padStart(2, '0')}"
            }
        }

        // 4. Extended fields
        val receiverId = receiverIdRegex.find(text)?.groupValues?.get(1)?.trim()
        val remarks = remarksRegex.find(text)?.groupValues?.get(1)?.trim()
        val paymentMethod = paymentMethodRegex.find(text)?.groupValues?.get(1)?.trim()

        return ParsedReceipt(
            amount = amount,
            date = formattedDate,
            merchant = receiverName,
            receiverName = receiverName,
            receiverId = receiverId,
            remarks = remarks,
            paymentMethod = paymentMethod
        )
    }
}
