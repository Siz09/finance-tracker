package com.example

import com.example.utils.ReceiptParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ReceiptParser covering each supported receipt format.
 * Raw OCR text strings are representative of what the ML Kit recogniser produces.
 *
 * Run with:  ./gradlew test  (or the green triangle in the IDE gutter)
 */
class ReceiptParserTest {

    // ── eSewa Send Money receipt ──────────────────────────────────────────────

    @Test
    fun `parse eSewa send money receipt - amount extracted correctly`() {
        val ocrText = """
            eSewa
            Send Money
            Complete
            Amount (NPR):
            1,750.00
            Receiver Name:
            Amit Jayaswal
            Receiver Esewa ld:
            amitjay230 @gmail
            .Com
            Transaction Code:
            16D37HB
            Processed By:
            9844296224
            Purpose:
            Personal Use
            2025-11-17 10:52 PM
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        assertEquals(1750.0, result.amount)
        assertEquals("2025-11-17", result.date)
        assertEquals("Amit Jayaswal", result.receiverName)
        assertEquals("amitjay230@gmail.com", result.receiverId)
        assertEquals("16D37HB", result.transactionCode)
        assertEquals("9844296224", result.processedBy)
        assertEquals("Personal Use", result.purpose)
        assertEquals("Transfer", result.suggestedCategory)
    }

    @Test
    fun `parse eSewa - category is Transfer not Utilities`() {
        val ocrText = """
            eSewa
            Send Money
            Complete
            Amount (NPR):
            500.00
            Receiver Name:
            Ram Bahadur
            Purpose:
            Personal Use
            2025-12-01 03:00 PM
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        // "send money" must win over any utility keyword match
        assertEquals("Transfer", result.suggestedCategory)
        assertNotEquals("Utilities", result.suggestedCategory)
    }

    // ── Fonepay receipt ───────────────────────────────────────────────────────

    @Test
    fun `parse Fonepay receipt - DD slash MM slash YYYY date format`() {
        val ocrText = """
            Fonepay
            Payment Successful
            Amount: Rs. 864.93
            Transaction Code: FP20251118
            Payment Method: Fonepay QR
            25/11/2025 14:30:00
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        assertEquals(864.93, result.amount!!, 0.01)
        assertEquals("2025-11-25", result.date)
        assertEquals("Fonepay QR", result.paymentMethod)
    }

    // ── Paper/POS receipt ─────────────────────────────────────────────────────

    @Test
    fun `parse paper receipt - largest plausible amount selected`() {
        val ocrText = """
            BAKERY & CAFE
            Table 4
            Croissant x2      Rs. 240
            Coffee x1         Rs. 180
            Total             Rs. 420
            30 May 2026
            Thank you for visiting!
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        // Should pick 420 as the dominant "Total" amount
        assertEquals(420.0, result.amount!!, 0.01)
        assertEquals("2026-05-30", result.date)
        assertEquals("Food & Drinks", result.suggestedCategory)
    }

    // ── Khalti receipt ────────────────────────────────────────────────────────

    @Test
    fun `parse Khalti receipt - amount via keyword fallback`() {
        val ocrText = """
            Khalti Digital Wallet
            Payment Successful
            NPR 1,200
            Merchant: Daraz Nepal
            Order: #DZ987654
            Date: 2025/11/20
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        assertEquals(1200.0, result.amount!!, 0.01)
        // DD/MM/YYYY or YYYY/MM/DD — ensure it parses without crashing
        assertNotNull(result.date)
        assertEquals("Shopping", result.suggestedCategory)
    }

    // ── Amount validation edge-cases ──────────────────────────────────────────

    @Test
    fun `phone numbers are not extracted as amounts`() {
        val ocrText = """
            Contact: 9841234567
            Amount (NPR):
            500.00
            Date: 2025-11-01
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        // 9841234567 is 10 digits starting with 9 — must be rejected
        assertEquals(500.0, result.amount)
        assertNotEquals(9841234567.0, result.amount)
    }

    @Test
    fun `year numbers are not extracted as amounts`() {
        val ocrText = """
            Invoice 2025
            Amount: Rs. 350
            2025-06-15
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        assertEquals(350.0, result.amount)
        assertNotEquals(2025.0, result.amount)
    }

    // ── Date format coverage ──────────────────────────────────────────────────

    @Test
    fun `ISO date format YYYY-MM-DD extracted correctly`() {
        val result = ReceiptParser.parse("Amount: Rs. 100\n2026-01-15")
        assertEquals("2026-01-15", result.date)
    }

    @Test
    fun `DD slash MM slash YYYY date format extracted correctly`() {
        val result = ReceiptParser.parse("Amount: Rs. 100\n15/01/2026")
        assertEquals("2026-01-15", result.date)
    }

    @Test
    fun `D Month YYYY text date extracted correctly`() {
        val result = ReceiptParser.parse("Amount: Rs. 100\n5 January 2026")
        assertEquals("2026-01-05", result.date)
    }

    @Test
    fun `Month D comma YYYY text date extracted correctly`() {
        val result = ReceiptParser.parse("Amount: Rs. 100\nJanuary 5, 2026")
        assertEquals("2026-01-05", result.date)
    }

    // ── Category suggestion ───────────────────────────────────────────────────

    @Test
    fun `restaurant receipt categorised as food and drinks`() {
        val result = ReceiptParser.parse("Momo restaurant\nAmount Rs. 150\n2026-05-01")
        assertEquals("Food & Drinks", result.suggestedCategory)
    }

    @Test
    fun `internet bill categorised as utilities`() {
        val result = ReceiptParser.parse("WorldLink internet bill\nAmount Rs. 1200\n2026-05-01")
        assertEquals("Utilities", result.suggestedCategory)
    }

    @Test
    fun `Pathao ride categorised as transport`() {
        val result = ReceiptParser.parse("Pathao ride\nAmount Rs. 250\n2026-05-01")
        assertEquals("Transport", result.suggestedCategory)
    }

    @Test
    fun `Daraz purchase categorised as shopping`() {
        val result = ReceiptParser.parse("Daraz delivery\nAmount Rs. 850\n2026-05-01")
        assertEquals("Shopping", result.suggestedCategory)
    }

    @Test
    fun `unknown receipt falls back to Other`() {
        val result = ReceiptParser.parse("Random unrecognised text\nAmount Rs. 50\n2026-05-01")
        assertEquals("Other", result.suggestedCategory)
    }

    @Test
    fun `parse Fonepay payment receipt - amount with columns and request id extracted correctly`() {
        val ocrText = """
            Fonepay Payment
            2026-06-03 05:19 PM
            Complete
            Amount (NPR): Transaction Code:
            273.42 1J38S7B
            Processed By:
            9844296224
            Merchant Code: Transaction Currency:
            2222400020599080 NPR
            Initiator: Description:
            9844296224 9844296224
            Merchant Name: Unique Request Id:
            NEW RABINA COSMETIC PASAL 72793349881126686b
            3792-0072-489a-90ba
            -416e1beb4237
            Purpose Of Payment: Payment Method:
            Lifestyle & Entertainment eSewa Wallet
            Request Unique Id:
            416066384214
        """.trimIndent()

        val result = ReceiptParser.parse(ocrText)

        assertEquals(273.42, result.amount!!, 0.001)
        assertEquals("2026-06-03", result.date)
        assertEquals("NEW RABINA COSMETIC PASAL", result.merchant)
        assertEquals("eSewa Wallet", result.paymentMethod)
        assertEquals("1J38S7B", result.transactionCode)
        assertEquals("9844296224", result.processedBy)
        assertEquals("Lifestyle & Entertainment", result.purpose)
    }
}
