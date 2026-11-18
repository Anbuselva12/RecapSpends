package com.example.util

import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.SharedExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun escape_quotesCommasQuotesAndNormalizesNewlines() {
        assertEquals("Coffee", CsvExporter.escape("Coffee"))
        assertEquals("\"Fresh, Mart\"", CsvExporter.escape("Fresh, Mart"))
        assertEquals("\"Sri \"\"Balaji\"\" Traders\"", CsvExporter.escape("Sri \"Balaji\" Traders"))
        assertEquals("\"Line1 Line2, ok\"", CsvExporter.escape("Line1\nLine2, ok"))
    }

    @Test
    fun exportExpensesCsv_writesOnlyProvidedFilteredRowsWithTypeAndGstColumns() {
        val categories = listOf(
            Category(id = "groc", name = "Groceries"),
            Category(id = "inc", name = "Income")
        )
        val filteredRows = listOf(
            Expense(
                id = "e1",
                title = "Fresh, Mart",
                amount = 1050.0,
                dateEpochMs = 1_700_000_000_000L,
                categoryId = "groc",
                subcategory = "Groceries",
                paymentMode = "UPI",
                entryType = "expense",
                billNumber = "INV-101",
                gstin = "33AABCU9603R1ZM",
                taxableValue = 1000.0,
                cgst = 25.0,
                sgst = 25.0,
                items = listOf(ExpenseItem(name = "Rice", qty = 10.0, unit = "kg", rate = 100.0))
            ),
            Expense(
                id = "e2",
                title = "Acme Consulting",
                amount = 45000.0,
                dateEpochMs = 1_700_086_400_000L,
                categoryId = "inc",
                subcategory = "Business / Freelance",
                paymentMode = "Bank",
                entryType = "income"
            )
        )

        val csv = CsvExporter.exportExpensesCsv(filteredRows, categories)
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Date,Type,Merchant,Category,Subcategory,PaymentMode"))
        assertTrue(lines[1].contains(",Expense,\"Fresh, Mart\",Groceries,Groceries,UPI,INV-101,33AABCU9603R1ZM,1000.00,25.00,25.00,0.00,0.00,1050.00,"))
        assertTrue(lines[1].contains("Rice (10 kg @ 100.00)"))
        assertTrue(lines[2].contains(",Income,Acme Consulting,Income,Business / Freelance,Bank"))
        assertFalse(csv.contains("Excluded Merchant"))
    }

    @Test
    fun exportSharedExpensesCsv_includesMemberAndEntryType() {
        val rows = listOf(
            SharedExpense(
                id = "s1",
                ownerUid = "u1",
                ownerName = "Karthik, R",
                title = "Dividend",
                amount = 3200.0,
                dateEpochMs = 1_700_000_000_000L,
                categoryId = "inc",
                categoryName = "Income",
                subcategory = "Interest & dividend",
                paymentMode = "Bank",
                entryType = "income"
            )
        )

        val csv = CsvExporter.exportSharedExpensesCsv(rows)
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertEquals("Date,Type,Member,Merchant,Category,Subcategory,PaymentMode,Amount", lines[0])
        assertTrue(lines[1].contains(",Income,\"Karthik, R\",Dividend,Income,Interest & dividend,Bank,3200.00"))
    }
}
