package com.example.utils

import java.util.regex.Pattern

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
    private val amountRegex = Regex("""(?:TOTAL|AMOUNT|NET|DUE|RS|NPR|\$)\s*[:=]?\s*(\d+[.,]\d{2})""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})|(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")
    
    // New fields regex patterns
    private val receiverRegex = Regex("""(?:RECEIVED BY|TO|MERCHANT|PAID TO|TRANSFER TO)\s*[:=]?\s*([A-Z0-9\s.]+)\b""", RegexOption.IGNORE_CASE)
    private val receiverIdRegex = Regex("""(?:ID|A\/C|MOBILE|NUMBER)\s*[:=]?\s*(\d{5,15})""", RegexOption.IGNORE_CASE)
    private val remarksRegex = Regex("""(?:REMARKS|DESCRIPTION|FOR|PURPOSE)\s*[:=]?\s*([A-Z0-9\s.,/]+)\b""", RegexOption.IGNORE_CASE)
    private val paymentMethodRegex = Regex("""(?:PAYMENT|MODE|METHOD|VIA)\s*[:=]?\s*(ESEWA|FONEPAY|BANK|CASH|KHALTI|CONNECTIPS)\b""", RegexOption.IGNORE_CASE)

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().filter { it.isNotBlank() }
        
        // 1. Merchant / Receiver Name
        var merchant = lines.firstOrNull()?.trim()
        val receiverMatch = receiverRegex.find(text)
        val receiverName = receiverMatch?.groupValues?.get(1)?.trim() ?: merchant

        // 2. Amount
        var amount: Double? = null
        val amountMatch = amountRegex.find(text)
        if (amountMatch != null) {
            amount = amountMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
        } else {
            val allAmounts = Regex("""\d+[.,]\d{2}""").findAll(text)
                .map { it.value.replace(",", ".").toDoubleOrNull() }
                .filterNotNull()
                .toList()
            amount = allAmounts.maxOrNull()
        }

        // 3. Date (Enforce YYYY-MM-DD)
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
                "${g4}-${g5.padStart(2, '0')}-${g6.padStart(2, '0')}"
            } else {
                var year = g3
                if (year.length == 2) year = "20$year"
                // Assuming DD-MM-YYYY or MM-DD-YYYY. Default to DD-MM-YYYY
                "${year}-${g2.padStart(2, '0')}-${g1.padStart(2, '0')}"
            }
        }

        // 4. New Fields
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
