package com.example.data.model

import com.example.util.IndianFormatters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.max
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthlyBudget: Double = 65000.0,
    val language: String = "en",
    val seeded: Boolean = false,
    val displayName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "monthlyBudget" to monthlyBudget,
            "language" to language,
            "seeded" to seeded,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!displayName.isNullOrBlank()) {
            map["displayName"] = displayName.take(80)
        }
        return map
    }
}

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "tag",
    val colorIndex: Int = 1,
    val subcategories: List<String> = emptyList(),
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.take(60),
        "iconKey" to iconKey.take(30),
        "colorIndex" to colorIndex.coerceIn(1, 9),
        "subcategories" to subcategories.take(25),
        "sortOrder" to sortOrder.coerceIn(0, 500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.take(60),
        "iconKey" to iconKey.take(30),
        "colorIndex" to colorIndex.coerceIn(1, 9),
        "subcategories" to subcategories.take(25),
        "sortOrder" to sortOrder.coerceIn(0, 500),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ExpenseItem(
    val name: String = "",
    val qty: Double = 1.0,
    val unit: String = "pcs",
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    val hsn: String = "",
    val gstRate: Double = 0.0,
    val parentItemName: String = "",
    val amount: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "qty" to qty,
        "unit" to unit,
        "rate" to rate,
        "discount" to discount,
        "hsn" to hsn,
        "gstRate" to gstRate,
        "parentItemName" to parentItemName,
        "amount" to amount
    )
}

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val categoryId: String = "groc",
    val subcategory: String = "",
    val paymentMode: String = "UPI",
    val projectId: String? = null,
    val vehicleId: String? = null,
    val gstin: String? = null,
    val billNumber: String? = null,
    val taxableValue: Double? = null,
    val cgst: Double? = null,
    val sgst: Double? = null,
    val igst: Double? = null,
    val roundOff: Double? = null,
    val items: List<ExpenseItem>? = null,
    val attachmentNotes: List<String>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to title.ifBlank { "Expense" }.take(140),
            "amount" to amount.coerceAtLeast(0.0),
            "dateEpochMs" to dateEpochMs.coerceAtLeast(1L),
            "categoryId" to categoryId.ifBlank { "groc" }.take(128),
            "subcategory" to subcategory.take(80),
            "paymentMode" to if (paymentMode in listOf("UPI", "Cash", "Card", "Bank")) paymentMode else "UPI",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!projectId.isNullOrBlank()) map["projectId"] = projectId.take(128)
        if (!vehicleId.isNullOrBlank()) map["vehicleId"] = vehicleId.take(128)
        if (!gstin.isNullOrBlank()) map["gstin"] = gstin.take(32)
        if (!billNumber.isNullOrBlank()) map["billNumber"] = billNumber.take(64)
        if (taxableValue != null && taxableValue >= 0.0) map["taxableValue"] = taxableValue
        if (cgst != null && cgst >= 0.0) map["cgst"] = cgst
        if (sgst != null && sgst >= 0.0) map["sgst"] = sgst
        if (igst != null && igst >= 0.0) map["igst"] = igst
        if (roundOff != null) map["roundOff"] = roundOff
        if (!items.isNullOrEmpty()) map["items"] = items.take(50).map { it.toMap() }
        if (!attachmentNotes.isNullOrEmpty()) map["attachmentNotes"] = attachmentNotes.take(10)
        return map
    }
}

enum class ServiceStatus(val severity: Int) {
    OVERDUE(0),
    DUE_SOON(1),
    OK(2)
}

data class ComputedServiceState(
    val status: ServiceStatus,
    val kmLeft: Int?,
    val daysLeft: Int,
    val nextDueDateMs: Long,
    val nextDueKm: Int?,
    val progress: Float,
    val statusLabel: String,
    val nextDueLabel: String
)

@IgnoreExtraProperties
data class ServiceItem(
    val id: String = "",
    val name: String = "",
    val iconKey: String = "wrench",
    val intervalKm: Int = 0,
    val intervalMonths: Int = 12,
    val lastDoneEpochMs: Long = System.currentTimeMillis(),
    val lastDoneKm: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "iconKey" to iconKey,
        "intervalKm" to intervalKm,
        "intervalMonths" to intervalMonths,
        "lastDoneEpochMs" to lastDoneEpochMs,
        "lastDoneKm" to lastDoneKm
    )

    fun computeState(currentOdometer: Int, todayMs: Long): ComputedServiceState {
        val nextDueDateMs = IndianFormatters.addMonths(lastDoneEpochMs, intervalMonths.coerceAtLeast(1))
        val daysLeft = IndianFormatters.daysBetween(nextDueDateMs, todayMs)
        val kmLeft = if (intervalKm > 0) (lastDoneKm + intervalKm) - currentOdometer else null
        val nextDueKm = if (intervalKm > 0) lastDoneKm + intervalKm else null

        val status = when {
            (kmLeft != null && kmLeft < 0) || daysLeft < 0 -> ServiceStatus.OVERDUE
            (kmLeft != null && kmLeft < intervalKm * 0.15) || daysLeft < 30 -> ServiceStatus.DUE_SOON
            else -> ServiceStatus.OK
        }

        val kmUsage = if (intervalKm > 0) {
            (currentOdometer - lastDoneKm).toFloat() / intervalKm.toFloat()
        } else 0f
        val totalPeriodMs = (nextDueDateMs - lastDoneEpochMs).coerceAtLeast(86_400_000L)
        val timeUsage = (todayMs - lastDoneEpochMs).toFloat() / totalPeriodMs.toFloat()
        val progress = max(0f, max(kmUsage, timeUsage)).coerceIn(0f, 1f)

        val statusLabel = when (status) {
            ServiceStatus.OVERDUE -> {
                if (kmLeft != null && kmLeft < 0) {
                    "Overdue by ${IndianFormatters.formatIndianInteger((-kmLeft).toLong())} km"
                } else {
                    "Overdue ${-daysLeft} days"
                }
            }
            ServiceStatus.DUE_SOON -> {
                if (kmLeft != null && kmLeft < intervalKm * 0.15) {
                    "${IndianFormatters.formatIndianInteger(kmLeft.toLong())} km left"
                } else if (daysLeft <= 0) {
                    "Due today"
                } else {
                    "$daysLeft days left"
                }
            }
            ServiceStatus.OK -> {
                if (kmLeft != null && daysLeft >= 60) {
                    "${IndianFormatters.formatIndianInteger(kmLeft.toLong())} km left"
                } else if (daysLeft <= 0) {
                    "Due today"
                } else {
                    "$daysLeft days left"
                }
            }
        }

        val nextKmPart = if (nextDueKm != null) {
            "${IndianFormatters.formatIndianInteger(nextDueKm.toLong())} km or "
        } else ""
        val nextDueLabel = "Next: $nextKmPart${IndianFormatters.formatDate(nextDueDateMs)}"

        return ComputedServiceState(
            status = status,
            kmLeft = kmLeft,
            daysLeft = daysLeft,
            nextDueDateMs = nextDueDateMs,
            nextDueKm = nextDueKm,
            progress = progress,
            statusLabel = statusLabel,
            nextDueLabel = nextDueLabel
        )
    }
}

@IgnoreExtraProperties
data class ServiceVisit(
    val dateEpochMs: Long = System.currentTimeMillis(),
    val odometer: Int = 0,
    val workshop: String = "",
    val amount: Double = 0.0,
    val itemsDone: List<String> = emptyList(),
    val expenseId: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "dateEpochMs" to dateEpochMs,
        "odometer" to odometer,
        "workshop" to workshop,
        "amount" to amount,
        "itemsDone" to itemsDone,
        "expenseId" to expenseId
    )
}

@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shortName: String = "",
    val type: String = "bike",
    val plate: String = "",
    val odometer: Int = 0,
    val avgMileage: Double = 0.0,
    val items: List<ServiceItem> = emptyList(),
    val history: List<ServiceVisit> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.take(80),
        "shortName" to shortName.take(40),
        "type" to if (type == "car") "car" else "bike",
        "plate" to plate.ifBlank { "Add plate" }.take(30),
        "odometer" to odometer.coerceIn(0, 2_000_000),
        "avgMileage" to avgMileage.coerceIn(0.0, 200.0),
        "items" to items.take(40).map { it.toMap() },
        "history" to history.take(100).map { it.toMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.take(80),
        "shortName" to shortName.take(40),
        "type" to if (type == "car") "car" else "bike",
        "plate" to plate.ifBlank { "Add plate" }.take(30),
        "odometer" to odometer.coerceIn(0, 2_000_000),
        "avgMileage" to avgMileage.coerceIn(0.0, 200.0),
        "items" to items.take(40).map { it.toMap() },
        "history" to history.take(100).map { it.toMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Bill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val dueEpochMs: Long = System.currentTimeMillis(),
    val repeat: String = "Monthly",
    val reminder: String = "3 days before",
    val categoryId: String = "util",
    val subcategory: String = "",
    val iconKey: String = "bell",
    val autopay: Boolean = false,
    val paid: Boolean = false,
    val paidOnEpochMs: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.take(100),
            "amount" to amount.coerceAtLeast(1.0),
            "dueEpochMs" to dueEpochMs.coerceAtLeast(1L),
            "repeat" to if (repeat in listOf("Once", "Monthly", "Bi-monthly", "Half-yearly", "Yearly")) repeat else "Monthly",
            "reminder" to if (reminder in listOf("On the day", "3 days before", "7 days before")) reminder else "3 days before",
            "categoryId" to categoryId.ifBlank { "util" }.take(128),
            "subcategory" to subcategory.take(80),
            "iconKey" to iconKey.ifBlank { "bell" }.take(30),
            "autopay" to autopay,
            "paid" to paid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (paidOnEpochMs != null && paidOnEpochMs > 0L) {
            map["paidOnEpochMs"] = paidOnEpochMs
        }
        return map
    }
}

@IgnoreExtraProperties
data class ProjectPhase(
    val name: String = "",
    val budget: Double = 0.0,
    val spent: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "budget" to budget,
        "spent" to spent
    )
}

@IgnoreExtraProperties
data class ProjectVendor(
    val name: String = "",
    val role: String = "",
    val quoted: Double = 0.0,
    val paid: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "role" to role,
        "quoted" to quoted,
        "paid" to paid
    )
}

@IgnoreExtraProperties
data class ProjectPayment(
    val dateEpochMs: Long = System.currentTimeMillis(),
    val title: String = "",
    val amount: Double = 0.0,
    val vendor: String = "",
    val phaseName: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "dateEpochMs" to dateEpochMs,
        "title" to title,
        "amount" to amount,
        "vendor" to vendor,
        "phaseName" to phaseName
    )
}

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val subtitle: String = "",
    val iconKey: String = "house",
    val colorIndex: Int = 2,
    val budget: Double = 0.0,
    val phases: List<ProjectPhase> = emptyList(),
    val vendors: List<ProjectVendor> = emptyList(),
    val payments: List<ProjectPayment> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.take(100),
        "subtitle" to subtitle.take(140),
        "iconKey" to iconKey.ifBlank { "house" }.take(30),
        "colorIndex" to colorIndex.coerceIn(1, 9),
        "budget" to budget.coerceAtLeast(1.0),
        "phases" to phases.take(25).map { it.toMap() },
        "vendors" to vendors.take(50).map { it.toMap() },
        "payments" to payments.take(200).map { it.toMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "subtitle" to subtitle.take(140),
        "iconKey" to iconKey.ifBlank { "house" }.take(30),
        "colorIndex" to colorIndex.coerceIn(1, 9),
        "budget" to budget.coerceAtLeast(1.0),
        "phases" to phases.take(25).map { it.toMap() },
        "vendors" to vendors.take(50).map { it.toMap() },
        "payments" to payments.take(200).map { it.toMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

// --- AI Scan Draft Models ---

data class ScannedSubItem(
    val name: String,
    val qty: Double,
    val rate: Double
) {
    val lineAmount: Double get() = qty * rate
}

data class ScannedBillItem(
    val id: Int,
    val name: String,
    val qty: Double,
    val unit: String,
    val rate: Double,
    val discount: Double = 0.0,
    val hsn: String = "",
    val gstRate: Double = 18.0,
    val mappedServiceItemId: String? = null,
    val lowConfidenceNote: String? = null,
    val subItems: List<ScannedSubItem> = emptyList()
) {
    val taxableAmount: Double
        get() = if (subItems.isNotEmpty()) {
            subItems.sumOf { it.lineAmount } - discount
        } else {
            (qty * rate) - discount
        }.coerceAtLeast(0.0)
}

data class ScannedBillDraft(
    val merchant: String,
    val gstin: String,
    val billNumber: String,
    val dateEpochMs: Long,
    val confidencePercent: Int,
    val suggestedCategoryId: String,
    val suggestedSubcategory: String,
    val selectedVehicleId: String?,
    val isInterStateIgst: Boolean = false,
    val keepItemLevel: Boolean = true,
    val items: List<ScannedBillItem>
) {
    val taxableTotal: Double get() = items.sumOf { it.taxableAmount }
    val gstTotal: Double get() = items.sumOf { it.taxableAmount * (it.gstRate / 100.0) }
    val cgst: Double get() = if (isInterStateIgst) 0.0 else gstTotal / 2.0
    val sgst: Double get() = if (isInterStateIgst) 0.0 else gstTotal / 2.0
    val igst: Double get() = if (isInterStateIgst) gstTotal else 0.0
    val exactTotal: Double get() = taxableTotal + gstTotal
    val grandTotal: Double get() = exactTotal.roundToInt().toDouble()
    val roundOff: Double get() = grandTotal - exactTotal
}

data class AttentionAlert(
    val id: String,
    val title: String,
    val statusLabel: String,
    val severity: ServiceStatus,
    val iconKey: String,
    val colorIndex: Int,
    val daysWeight: Int,
    val targetVehicleId: String? = null,
    val isBill: Boolean = false
)
