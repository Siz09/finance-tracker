package com.example

import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.utils.ExportHelper
import org.junit.Assert.*
import org.junit.Test

class ExportHelperTest {

    @Test
    fun `formatBytes - formats different sizes correctly`() {
        assertEquals("512 B", ExportHelper.formatBytes(512L))
        assertEquals("1.0 KB", ExportHelper.formatBytes(1024L))
        assertEquals("1.5 KB", ExportHelper.formatBytes(1536L))
        assertEquals("2.0 MB", ExportHelper.formatBytes(2097152L))
        assertEquals("3.5 GB", ExportHelper.formatBytes(3758096384L))
    }

    @Test
    fun `parseImportedJSON - extracts valid structured backup content successfully`() {
        val json = """
        {
          "transactions": [
            {
              "id": 1,
              "type": "expense",
              "amount": 250.50,
              "category": "Food & Drinks",
              "date": "2026-05-30",
              "note": "A \"premium\" lunch momo",
              "imagePath": "null",
              "receiverName": "Bhatbhateni",
              "receiverId": "null",
              "remarks": "momo",
              "paymentMethod": "Fonepay",
              "transactionCode": "FP123",
              "processedBy": "9841000000",
              "purpose": "Food",
              "initiatorName": "Ram",
              "createdAt": 1717142400000
            }
          ],
          "budgets": [
            {
              "id": 1,
              "category": "Food & Drinks",
              "monthlyLimit": 15000.00,
              "month": "2026-05"
            }
          ],
          "savingsGoals": [
            {
              "id": 1,
              "target": 50000.00,
              "month": "2026-05"
            }
          ]
        }
        """.trimIndent()

        val result = ExportHelper.parseImportedJSON(json)

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.budgets.size)
        assertEquals(1, result.savingsGoals.size)

        val tx = result.transactions[0]
        assertEquals(1, tx.id)
        assertEquals("expense", tx.type)
        assertEquals(250.50, tx.amount, 0.001)
        assertEquals("Food & Drinks", tx.category)
        assertEquals("2026-05-30", tx.date)
        assertEquals("A \\\"premium\\\" lunch momo", tx.note) // parsed string escaped format
        assertEquals("Bhatbhateni", tx.receiverName)
        assertEquals("Fonepay", tx.paymentMethod)
        assertEquals("FP123", tx.transactionCode)
        assertEquals(1717142400000L, tx.createdAt)

        val bg = result.budgets[0]
        assertEquals(1, bg.id)
        assertEquals("Food & Drinks", bg.category)
        assertEquals(15000.0, bg.monthlyLimit, 0.001)
        assertEquals("2026-05", bg.month)

        val sg = result.savingsGoals[0]
        assertEquals(1, sg.id)
        assertEquals(50000.0, sg.target, 0.001)
        assertEquals("2026-05", sg.month)
    }

    @Test
    fun `parseImportedJSON - invalid transaction amounts are skipped with error`() {
        val json = """
        {
          "transactions": [
            {
              "id": 1,
              "type": "expense",
              "amount": -50.00,
              "category": "Other",
              "date": "2026-05-30",
              "note": null,
              "imagePath": null,
              "receiverName": null,
              "receiverId": null,
              "remarks": null,
              "paymentMethod": null,
              "transactionCode": null,
              "processedBy": null,
              "purpose": null,
              "initiatorName": null,
              "createdAt": 1717142400000
            }
          ]
        }
        """.trimIndent()

        val result = ExportHelper.parseImportedJSON(json)
        assertEquals(0, result.transactions.size)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("amount must be > 0"))
    }

    @Test
    fun `parseImportedJSON - invalid transaction types are skipped with error`() {
        val json = """
        {
          "transactions": [
            {
              "id": 1,
              "type": "unknown_type",
              "amount": 50.00,
              "category": "Other",
              "date": "2026-05-30",
              "note": null,
              "imagePath": null,
              "receiverName": null,
              "receiverId": null,
              "remarks": null,
              "paymentMethod": null,
              "transactionCode": null,
              "processedBy": null,
              "purpose": null,
              "initiatorName": null,
              "createdAt": 1717142400000
            }
          ]
        }
        """.trimIndent()

        val result = ExportHelper.parseImportedJSON(json)
        assertEquals(0, result.transactions.size)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("unknown type 'unknown_type'"))
    }

    @Test
    fun `parseImportedJSON - missing transactions array reports error`() {
        val json = """
        {
          "budgets": []
        }
        """.trimIndent()

        val result = ExportHelper.parseImportedJSON(json)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("No 'transactions' array found"))
    }
}
