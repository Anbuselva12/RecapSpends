package com.example.util

import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.SharedExpense
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CsvExporter {

    private val qtyFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

    fun escape(value: String): String {
        val clean = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    fun exportExpensesCsv(expenses: List<Expense>, categories: List<Category>): String {
        val catById = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine(
            "Date,Type,Merchant,Category,Subcategory,PaymentMode,BillNumber,GSTIN,TaxableValue,CGST,SGST,IGST,RoundOff,Amount,Items"
        )
        expenses.forEach { e ->
            val catName = catById[e.categoryId]?.name
                ?: if (e.categoryId == "inc" || e.entryType == "income") "Income" else "Other"
            val entryLabel = if (e.entryType == "income") "Income" else "Expense"
            val itemText = e.items.orEmpty().joinToString(" | ") { item ->
                val qty = qtyFormat.format(item.qty)
                val unit = item.unit.ifBlank { "pcs" }
                "${item.name} ($qty $unit @ ${String.format(Locale.US, "%.2f", item.rate)})"
            }
            sb.appendLine(
                listOf(
                    escape(IndianFormatters.formatDate(e.dateEpochMs)),
                    escape(entryLabel),
                    escape(e.title),
                    escape(catName),
                    escape(e.subcategory),
                    escape(e.paymentMode),
                    escape(e.billNumber.orEmpty()),
                    escape(e.gstin.orEmpty()),
                    String.format(Locale.US, "%.2f", e.taxableValue ?: e.amount),
                    String.format(Locale.US, "%.2f", e.cgst ?: 0.0),
                    String.format(Locale.US, "%.2f", e.sgst ?: 0.0),
                    String.format(Locale.US, "%.2f", e.igst ?: 0.0),
                    String.format(Locale.US, "%.2f", e.roundOff ?: 0.0),
                    String.format(Locale.US, "%.2f", e.amount),
                    escape(itemText)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun exportSharedExpensesCsv(sharedExpenses: List<SharedExpense>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Member,Merchant,Category,Subcategory,PaymentMode,Amount")
        sharedExpenses.forEach { e ->
            val entryLabel = if (e.entryType == "income") "Income" else "Expense"
            sb.appendLine(
                listOf(
                    escape(IndianFormatters.formatDate(e.dateEpochMs)),
                    escape(entryLabel),
                    escape(e.ownerName),
                    escape(e.title),
                    escape(e.categoryName),
                    escape(e.subcategory),
                    escape(e.paymentMode),
                    String.format(Locale.US, "%.2f", e.amount)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}
