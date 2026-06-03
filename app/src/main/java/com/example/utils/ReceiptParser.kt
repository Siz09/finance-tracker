package com.example.utils

data class ParsedReceipt(
    val amount: Double?,
    val date: String?,
    val merchant: String?,
    val receiverName: String? = null,
    val receiverId: String? = null,
    val remarks: String? = null,
    val paymentMethod: String? = null,
    val transactionCode: String? = null,
    val processedBy: String? = null,
    val purpose: String? = null,
    val initiatorName: String? = null,
    val suggestedCategory: String? = null
)

object ReceiptParser {

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val amount          = extractAmount(lines, text)
        val date            = extractDate(lines, text)
        val merchant        = extractMerchant(lines)
        val receiverName    = extractAfterLabel(lines, "Receiver Name")
        val receiverId      = extractReceiverId(lines)
        val remarks         = extractAfterLabel(lines, "Remarks")
        val paymentMethod   = extractAfterLabel(lines, "Payment Method")
        val transactionCode = extractAfterLabel(lines, "Transaction Code")
        val processedBy     = extractAfterLabel(lines, "Processed By")
        val purpose         = extractAfterLabel(lines, "Purpose")
        val initiatorName   = extractAfterLabel(lines, "Initiator Name")

        val suggestedCategory = suggestCategory(text)

        return ParsedReceipt(
            amount          = amount,
            date            = date,
            merchant        = merchant,
            receiverName    = receiverName,
            receiverId      = receiverId,
            remarks         = remarks,
            paymentMethod   = paymentMethod,
            transactionCode = transactionCode,
            processedBy     = processedBy,
            purpose         = purpose,
            initiatorName   = initiatorName,
            suggestedCategory = suggestedCategory
        )
    }

    // ── Amount ────────────────────────────────────────────────────────────────

    private fun extractAmount(lines: List<String>, text: String): Double? {
        // 1. Look for "Amount (NPR):" label — value is on the next non-empty line
        val amountLabelIdx = lines.indexOfFirst {
            it.contains("Amount", ignoreCase = true) &&
            it.contains("NPR", ignoreCase = true)
        }
        if (amountLabelIdx >= 0) {
            for (i in (amountLabelIdx + 1)..minOf(amountLabelIdx + 3, lines.lastIndex)) {
                val v = parseNumber(lines[i])
                if (v != null && isValidAmount(v)) return v
            }
        }

        // 2. Look for "Total", "Amount", "RS", "NPR" keyword on same line as number
        val keywordRegex = Regex(
            """(?:TOTAL|AMOUNT|NET|DUE|PAID|RS\.?|NPR|NRS)\s*[:=]?\s*([\d,]+(?:[.,]\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        val keywordMatches = keywordRegex.findAll(text).toList()
        if (keywordMatches.isNotEmpty()) {
            // Prioritize matches that explicitly mention "total"
            val totalMatch = keywordMatches.firstOrNull { it.value.contains("total", ignoreCase = true) }
            if (totalMatch != null) {
                parseNumber(totalMatch.groupValues[1])?.let { if (isValidAmount(it)) return it }
            }
            // Fallback: take the largest valid keyword match
            val largestKeywordAmount = keywordMatches
                .mapNotNull { parseNumber(it.groupValues[1]) }
                .filter { isValidAmount(it) }
                .maxOrNull()
            if (largestKeywordAmount != null) return largestKeywordAmount
        }

        // 3. Largest standalone number in plausible range (10–999999),
        //    ignoring phone numbers (10 digits), dates, reference codes, and UUIDs (with hyphens)
        val numberRegex = Regex("""(?<![-A-Z0-9])(\d+(?:,\d{3})*(?:\.\d{1,2})?)(?![-A-Z0-9])""", RegexOption.IGNORE_CASE)
        return numberRegex.findAll(text)
            .mapNotNull { parseNumber(it.groupValues[1]) }
            .filter { isValidAmount(it) }
            .maxOrNull()
    }

    private fun isValidAmount(value: Double?): Boolean {
        if (value == null) return false
        // Filter out 10-digit mobile numbers starting with 9
        if (value >= 9000000000.0 && value <= 9999999999.0) return false
        // Filter out numbers that look like years (4-digit numbers 1900–2099)
        // Only reject in fallback path; context-specific extraction (label lookup) bypasses this.
        if (value == value.toLong().toDouble() && value >= 1900.0 && value <= 2099.0) return false
        return value in 10.0..999999.0
    }

    private fun parseNumber(raw: String): Double? {
        val regex = Regex("""([\d,]+(?:\.\d+)?)""")
        val match = regex.find(raw) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    // ── Date ──────────────────────────────────────────────────────────────────

    private fun extractDate(lines: List<String>, text: String): String? {
        // Format 1: 2025-11-17 (ISO — also handles eSewa "2025-11-17 10:52 PM")
        Regex("""(\d{4})-(\d{2})-(\d{2})""").find(text)?.let { m ->
            return "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}"
        }

        // Format 2: DD/MM/YYYY or DD/MM/YYYY HH:MM:SS (Fonepay)
        Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""").find(text)?.let { m ->
            val y  = m.groupValues[3]
            val mo = m.groupValues[2].padStart(2, '0')
            val d  = m.groupValues[1].padStart(2, '0')
            return "$y-$mo-$d"
        }

        // Format 3: DD/MM/YY
        Regex("""(\d{1,2})/(\d{1,2})/(\d{2})""").find(text)?.let { m ->
            val y  = "20${m.groupValues[3]}"
            val mo = m.groupValues[2].padStart(2, '0')
            val d  = m.groupValues[1].padStart(2, '0')
            return "$y-$mo-$d"
        }

        // Format 4: "30 May 2026" or "30th May, 2026"
        val months = mapOf(
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
            "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
        )
        Regex("""(\d{1,2})(?:st|nd|rd|th)?\s+([A-Za-z]{3,9}),?\s+(\d{4})""").find(text)?.let { m ->
            val mo = months[m.groupValues[2].lowercase().take(3)]
            if (mo != null) return "${m.groupValues[3]}-$mo-${m.groupValues[1].padStart(2, '0')}"
        }

        // Format 5: "May 30, 2026"
        Regex("""([A-Za-z]{3,9})\s+(\d{1,2}),?\s+(\d{4})""").find(text)?.let { m ->
            val mo = months[m.groupValues[1].lowercase().take(3)]
            if (mo != null) return "${m.groupValues[3]}-$mo-${m.groupValues[2].padStart(2, '0')}"
        }

        return null
    }

    // ── Label-based extraction ─────────────────────────────────────────────────
    // Handles: "Label:\nValue" OR "Label: Value" on same line
    // Also handles multi-line values (e.g. receiver name split across 2 lines)

    private fun findSplitIndex(line: String, labelLineIndex: Int, labelLineLength: Int): Int {
        if (labelLineIndex <= 0) return 0
        if (labelLineIndex >= labelLineLength) return line.length
        val targetIdx = line.length * (labelLineIndex.toDouble() / labelLineLength)
        val spaceIndices = line.indices.filter { line[it] == ' ' }
        return spaceIndices.minByOrNull { Math.abs(it - targetIdx) } ?: line.length
    }

    private fun extractAfterLabel(lines: List<String>, label: String): String? {
        val labelIdx = lines.indexOfFirst { it.contains(label, ignoreCase = true) }
        if (labelIdx < 0) return null

        val labelLine = lines[labelIdx]
        val labelStart = labelLine.indexOf(label, ignoreCase = true)

        // Check if there are other labels on the same line (multi-column)
        val colonCount = labelLine.count { it == ':' }
        if (colonCount > 1) {
            val colonIdx = labelLine.indexOf(':', labelStart)
            if (colonIdx >= 0) {
                val nextColonIdx = labelLine.indexOf(':', colonIdx + 1)
                val nextLine = lines.getOrNull(labelIdx + 1)
                if (nextLine != null && !isLabel(nextLine)) {
                    val startIdx = findSplitIndex(nextLine, labelStart, labelLine.length)
                    if (nextColonIdx >= 0) {
                        var nextLabelStart = colonIdx + 1
                        while (nextLabelStart < labelLine.length && labelLine[nextLabelStart].isWhitespace()) {
                            nextLabelStart++
                        }
                        val endIdx = findSplitIndex(nextLine, nextLabelStart, labelLine.length)
                        if (startIdx < endIdx) {
                            return nextLine.substring(startIdx, endIdx).trim()
                        }
                    } else {
                        return nextLine.substring(startIdx).trim()
                    }
                }
            }
        }

        // Standard single-column label extraction
        val colonIdx = labelLine.indexOf(':')
        if (colonIdx >= 0) {
            val inline = labelLine.substring(colonIdx + 1).trim()
            if (inline.isNotEmpty()) {
                val nextLine = lines.getOrNull(labelIdx + 1)
                if (nextLine != null && !nextLine.contains(':') && !isLabel(nextLine) && !isNumericOrAmount(nextLine)) {
                    return "$inline $nextLine".trim()
                }
                return inline
            }
        }

        // Value on next line(s)
        val sb = StringBuilder()
        for (i in (labelIdx + 1)..minOf(labelIdx + 3, lines.lastIndex)) {
            val l = lines[i]
            if (isLabel(l) || isNumericOrAmount(l)) break
            sb.append(if (sb.isEmpty()) l else " $l")
            val next = lines.getOrNull(i + 1)
            if (next == null || isLabel(next) || next.contains(':') || isNumericOrAmount(next)) break
        }
        return sb.toString().trim().ifEmpty { null }
    }

    private fun isNumericOrAmount(line: String): Boolean {
        val clean = line.replace(",", "").trim()
        // Matches decimal numbers like 1750.00, 864.93, 10.50
        return clean.matches(Regex("""^\d+\.\d{2}$"""))
    }

    // A line is a "label" if it ends with a colon or matches known eSewa section headers
    private fun isLabel(line: String): Boolean {
        if (line.endsWith(':')) return true
        val known = listOf(
            "Amount", "Processed By", "Purpose", "Payment Method",
            "Remarks", "Transaction Code", "Receiver", "Initiator",
            "Send Money", "Complete", "Success", "Failed"
        )
        return known.any { line.contains(it, ignoreCase = true) }
    }

    // ── Receiver eSewa ID ─────────────────────────────────────────────────────
    // OCR reads "Receiver Esewa ld:" and the value may be fragmented across lines
    // e.g. "amitjay230 @gmail" + ".Com" → "amitjay230@gmail.com"

    private fun extractReceiverId(lines: List<String>): String? {
        val idx = lines.indexOfFirst {
            it.contains("Esewa", ignoreCase = true) &&
            (it.contains("Id", ignoreCase = true) || it.contains("ld", ignoreCase = true))
        }
        if (idx < 0) return null

        val sb = StringBuilder()
        for (i in (idx + 1)..minOf(idx + 3, lines.lastIndex)) {
            val l = lines[i]
            if (isLabel(l)) break
            sb.append(l.trim())
        }

        return sb.toString()
            .replace(Regex("""\s*@\s*"""), "@")
            .replace(Regex("""\s*\.\s*"""), ".")
            .lowercase()
            .trim()
            .ifEmpty { null }
    }

    // ── Merchant ──────────────────────────────────────────────────────────────

    private fun cleanMerchantValue(raw: String): String {
        val words = raw.split(Regex("""\s+"""))
        val cleanWords = mutableListOf<String>()
        for (w in words) {
            // Stop at the first word that contains digits and is length >= 4, or starts with a digit
            if (w.any { it.isDigit() } && (w.length >= 4 || (w.firstOrNull()?.isDigit() == true))) {
                break
            }
            cleanWords.add(w)
        }
        return cleanWords.joinToString(" ").trim()
    }

    private fun extractMerchant(lines: List<String>): String? {
        // 1. Look for explicit label "Merchant Name" or "Merchant"
        val labels = listOf("Merchant Name", "Merchant")
        for (label in labels) {
            val idx = lines.indexOfFirst { it.contains(label, ignoreCase = true) }
            if (idx >= 0) {
                val line = lines[idx]
                val colonIdx = line.indexOf(':')
                if (colonIdx >= 0) {
                    val inline = line.substring(colonIdx + 1).trim()
                    if (inline.isNotEmpty() && !inline.contains("Code", ignoreCase = true) && !inline.contains("Unique", ignoreCase = true)) {
                        val cleaned = cleanMerchantValue(inline)
                        if (cleaned.isNotEmpty()) return cleaned
                    }
                }
                // Check next line
                val nextIdx = idx + 1
                if (nextIdx <= lines.lastIndex) {
                    val nextLine = lines[nextIdx]
                    if (!isLabel(nextLine)) {
                        val cleaned = cleanMerchantValue(nextLine)
                        if (cleaned.isNotEmpty()) return cleaned
                    }
                }
            }
        }

        // 2. Fallback to existing logic: first 5 lines
        val skip = setOf(
            "send money", "receive money", "payment", "complete", "success", "failed",
            "personal use", "business", "transfer", "official use", "family", "friend"
        )
        val fallback = lines.take(5).firstOrNull { line ->
            line.isNotBlank() &&
            line.length > 2 &&
            !skip.any { line.lowercase().contains(it) } &&
            !line.contains(':') &&
            !line.matches(Regex("""\d+.*"""))
        }
        return fallback?.let { cleanMerchantValue(it) }
    }

    private fun suggestCategory(text: String): String {
        val lower = text.lowercase()

        // 0. Personal transfers / Send Money — check FIRST before any keyword matching
        //    to avoid misclassifying eSewa "Send Money" receipts as Utilities
        val transferKeywords = listOf("send money", "receive money", "personal transfer", "personal use", "money transfer")
        if (transferKeywords.any { lower.contains(it) }) return "Transfer"

        // 1. Investments / IPO
        val investmentKeywords = listOf("ipo", "share", "stock", "nepse", "tms", "mutual fund", "investment", "crypto", "bitcoin")
        if (investmentKeywords.any { lower.contains(it) }) return "Investments"

        // 2. Food & Drinks
        val foodKeywords = listOf("food", "drink", "cafe", "restaurant", "coke", "momo", "burger", "tea", "lunch", "dinner", "breakfast", "bakery", "sweets", "khaja", "caterers", "canteen")
        if (foodKeywords.any { lower.contains(it) }) return "Food & Drinks"

        // 3. Utilities / Bill Payments
        // Note: "visa" and "visacode" removed — they overlap with personal transfers
        val utilityKeywords = listOf("utility", "electricity", "water", "nea", "internet", "wlink", "vianet", "worldlink", "dishhome", "topup", "recharge", "ntc", "ncell", "card payment", "tax", "bill", "commission")
        if (utilityKeywords.any { lower.contains(it) }) return "Utilities"

        // 4. Transport / Ride Sharing
        val transportKeywords = listOf("transport", "bus", "taxi", "ride", "pathao", "indriver", "fuel", "petrol", "bike", "car", "airline", "flight", "ticket", "travel")
        if (transportKeywords.any { lower.contains(it) }) return "Transport"

        // 5. Shopping
        val shoppingKeywords = listOf("shopping", "dress", "shirt", "pant", "shoes", "daraz", "amazon", "cloth", "bhat-bhateni", "bhatbhateni", "mall", "gift", "fancy", "store", "mart", "grocery", "groceries")
        if (shoppingKeywords.any { lower.contains(it) }) return "Shopping"

        // 6. Entertainment
        val entertainmentKeywords = listOf("movie", "cinema", "netflix", "game", "pub", "bar", "club", "concert", "qfx", "theater")
        if (entertainmentKeywords.any { lower.contains(it) }) return "Entertainment"

        // 7. Housing & Rent
        val housingKeywords = listOf("rent", "room", "flat", "housing", "landlord", "furniture", "cement", "hardware")
        if (housingKeywords.any { lower.contains(it) }) return "Housing & Rent"

        // 8. Health
        val healthKeywords = listOf("health", "hospital", "medicine", "clinic", "doctor", "pharmacy", "medical", "lab", "dental")
        if (healthKeywords.any { lower.contains(it) }) return "Health"

        // 9. Education
        val educationKeywords = listOf("education", "school", "college", "tuition", "fee", "book", "stationery", "exam", "academy")
        if (educationKeywords.any { lower.contains(it) }) return "Education"

        // 10. Travel
        val travelKeywords = listOf("tour", "hotel", "resort", "trip", "trek", "vacation")
        if (travelKeywords.any { lower.contains(it) }) return "Travel"

        return "Other"
    }
}
