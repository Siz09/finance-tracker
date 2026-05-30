package com.example.utils

import java.text.SimpleDateFormat
import java.util.Locale

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

    // ─── AMOUNT PATTERNS ────────────────────────────────────────────────────────

    /** Keyword-anchored: "TOTAL: 1,500" / "Rs. 500" / "NPR 1500.50" */
    private val amountKeywordRegex = Regex(
        """(?:TOTAL|AMOUNT|NET\s*AMOUNT|DUE|PAID|GRAND\s*TOTAL|SUB\s*TOTAL|RS\.?|NPR|NRS)\s*[:=]?\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    /** Thousands-comma format: "1,500" or "1,500.00" */
    private val thousandsCommaRegex = Regex("""(?<!\d)(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?)(?!\d)""")

    /** Plain decimal/integer without comma: "500" / "1500.50" */
    private val plainNumberRegex = Regex("""(?<!\d)(\d+(?:\.\d{1,2})?)(?!\d)""")

    // ─── DATE PATTERNS (ordered: most-specific → least-specific) ────────────────

    /** ISO with optional time: 2026-05-30 or 2026-05-30 12:00:23 (eSewa, Khalti) */
    private val isoDateRegex = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?""")

    /** DD/MM/YYYY or DD-MM-YYYY with optional time (Fonepay) */
    private val dmyDateRegex = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?""")

    /** DD Month YYYY or DDth Month, YYYY — e.g. "30 May 2026", "30th May, 2026" */
    private val textMonthDayFirstRegex = Regex(
        """(\d{1,2})(?:st|nd|rd|th)?\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?),?\s+(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    /** Month DD, YYYY — e.g. "May 30, 2026" */
    private val textMonthMonthFirstRegex = Regex(
        """(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+(\d{1,2}),?\s+(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // ─── OTHER FIELD PATTERNS ────────────────────────────────────────────────────

    private val receiverRegex = Regex(
        """(?:RECEIVED\s*BY|TO|MERCHANT|PAID\s*TO|TRANSFER\s*TO|SENDER|RECIPIENT)\s*[:=]?\s*([A-Z][A-Z0-9\s.&'-]{1,40})""",
        RegexOption.IGNORE_CASE
    )

    private val receiverIdRegex = Regex(
        """(?:ID|A\/C|ACCOUNT|MOBILE|PHONE|REF(?:ERENCE)?(?:\s*NO)?)\s*[:=.]?\s*(\d{7,15})""",
        RegexOption.IGNORE_CASE
    )

    private val remarksRegex = Regex(
        """(?:REMARKS?|DESCRIPTION|FOR|PURPOSE|NARRATION)\s*[:=]?\s*([^\n]{2,60})""",
        RegexOption.IGNORE_CASE
    )

    private val paymentMethodRegex = Regex(
        """(?:PAYMENT|MODE|METHOD|VIA|THROUGH|CHANNEL)\s*[:=]?\s*(E-?SEWA|FONEPAY|BANK\s*TRANSFER|CASH|KHALTI|CONNECT\s*IPS|DEBIT\s*CARD|CREDIT\s*CARD|MOBILE\s*BANKING)""",
        RegexOption.IGNORE_CASE
    )

    /** Detect eSewa / Khalti / Fonepay in header/footer */
    private val appNameRegex = Regex(
        """(E-?SEWA|KHALTI|FONEPAY|CONNECT\s*IPS|IME\s*PAY|PRABHU\s*PAY)""",
        RegexOption.IGNORE_CASE
    )

    // ─── PUBLIC API ──────────────────────────────────────────────────────────────

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Merchant / app name from first line or known digital wallet name
        val appName = appNameRegex.find(text)?.groupValues?.get(1)?.trim()
        val merchant = appName ?: lines.firstOrNull()?.take(60)

        // 2. Receiver name
        val receiverName = receiverRegex.find(text)?.groupValues?.get(1)?.trim()?.trimEnd(',', '.')

        // 3. Amount — keyword-anchored first
        var amount: Double? = null

        val keywordMatch = amountKeywordRegex.find(text)
        if (keywordMatch != null) {
            amount = keywordMatch.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // Fallback 1 — thousands-comma format (e.g. "1,500" or "1,500.00")
        if (amount == null) {
            val candidates = thousandsCommaRegex.findAll(text)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .filter { it in 10.0..9_999_999.0 }
                .toList()
            // Prefer the last occurrence (total is usually at the bottom)
            amount = candidates.lastOrNull()
        }

        // Fallback 2 — plain integers/decimals, scan bottom-up through lines
        if (amount == null) {
            val candidates = mutableListOf<Double>()
            for (line in lines.asReversed()) {
                // Skip lines that are obviously dates, phone numbers, or IDs
                if (line.matches(Regex(""".*\d{4}[/-]\d{1,2}[/-]\d{1,2}.*"""))) continue
                if (line.matches(Regex(""".*\d{1,2}[/-]\d{1,2}[/-]\d{4}.*"""))) continue
                plainNumberRegex.findAll(line).forEach { m ->
                    val v = m.groupValues[1].toDoubleOrNull() ?: return@forEach
                    // Plausible amount: at least Rs. 10, not obviously a year/ID/phone
                    if (v in 10.0..999_999.0 && v.toLong().toString().length <= 7) {
                        candidates.add(v)
                    }
                }
                if (candidates.isNotEmpty()) break // stop after finding candidates in first non-empty line from bottom
            }
            // If still nothing, scan whole doc and pick max plausible value
            if (candidates.isEmpty()) {
                plainNumberRegex.findAll(text).forEach { m ->
                    val v = m.groupValues[1].toDoubleOrNull() ?: return@forEach
                    if (v in 10.0..999_999.0 && v.toLong().toString().length <= 7) candidates.add(v)
                }
            }
            amount = candidates.maxOrNull()
        }

        // 4. Date — try most-specific formats first
        val formattedDate = parseDate(text)

        // 5. Extended fields
        val receiverId = receiverIdRegex.find(text)?.groupValues?.get(1)?.trim()
        val remarks = remarksRegex.find(text)?.groupValues?.get(1)?.trim()?.take(100)

        // Payment method: try explicit label, or infer from app name in header
        val paymentMethod = paymentMethodRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: appName?.let { capitalizeFirst(it.replace(Regex("""\s+"""), " ")) }

        return ParsedReceipt(
            amount = amount,
            date = formattedDate,
            merchant = receiverName ?: merchant,
            receiverName = receiverName,
            receiverId = receiverId,
            remarks = remarks,
            paymentMethod = paymentMethod
        )
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    private fun parseDate(text: String): String? {
        // 1. ISO date (already YYYY-MM-DD), with optional time
        isoDateRegex.find(text)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            val d = m.groupValues[3].padStart(2, '0')
            if (y.toIntOrNull() in 2000..2099) return "$y-$mo-$d"
        }

        // 2. DD/MM/YYYY or DD-MM-YYYY (Fonepay, printed receipts)
        dmyDateRegex.find(text)?.let { m ->
            val day = m.groupValues[1].padStart(2, '0')
            val month = m.groupValues[2].padStart(2, '0')
            val year = m.groupValues[3]
            if (year.toIntOrNull() in 2000..2099) return "$year-$month-$day"
        }

        // 3. "30 May 2026" or "30th May, 2026"
        textMonthDayFirstRegex.find(text)?.let { m ->
            val day = m.groupValues[1].padStart(2, '0')
            val monthStr = m.groupValues[2]
            val year = m.groupValues[3]
            val month = parseMonthName(monthStr) ?: return@let
            return "$year-$month-$day"
        }

        // 4. "May 30, 2026"
        textMonthMonthFirstRegex.find(text)?.let { m ->
            val monthStr = m.groupValues[1]
            val day = m.groupValues[2].padStart(2, '0')
            val year = m.groupValues[3]
            val month = parseMonthName(monthStr) ?: return@let
            return "$year-$month-$day"
        }

        return null
    }

    private fun parseMonthName(name: String): String? {
        val months = mapOf(
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
            "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
        )
        return months[name.lowercase().take(3)]
    }

    private fun capitalizeFirst(s: String): String =
        s.lowercase().replaceFirstChar { it.uppercase() }
}
