package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseScope
import com.example.data.model.ServiceStatus
import com.example.data.model.SharedBill
import com.example.data.model.SharedCategory
import com.example.data.model.SharedExpense
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.data.model.Workspace
import com.example.data.model.WorkspaceExpenseFilter
import com.example.ui.components.CategoryShareSlice
import com.example.ui.components.CategoryTile
import com.example.ui.components.DonutSpendChart
import com.example.ui.components.MonthlyTrendBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuEmptyState
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuMainHeader
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTextField
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import com.google.firebase.auth.FirebaseUser
import kotlin.math.roundToInt

private data class BillDisplayEntry(
    val bill: Bill,
    val isOwner: Boolean,
    val ownerName: String?
)

@Composable
fun BillsScreen(
    bills: List<Bill>,
    categories: List<Category>,
    currentUid: String = "",
    sharedBills: List<SharedBill> = emptyList(),
    userDisplayName: String? = null,
    userPhotoUrl: String? = null,
    onMarkPaid: (Bill) -> Unit,
    onOpenNewReminder: () -> Unit,
    onOpenBillDetail: (Bill) -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }
    var selectedDayMs by remember { mutableStateOf<Long?>(null) }

    val displayEntries = remember(bills, sharedBills, currentUid) {
        val own = bills.map { BillDisplayEntry(bill = it, isOwner = true, ownerName = null) }
        val others = sharedBills
            .filter { it.ownerUid != currentUid }
            .map { sb -> BillDisplayEntry(bill = sb.toBill(), isOwner = false, ownerName = sb.ownerName) }
        (own + others).sortedBy { it.bill.dueEpochMs }
    }
    val allBills = remember(displayEntries) { displayEntries.map { it.bill } }

    val calendarDays = remember(nowMs) {
        (-3..17).map { offset -> IndianFormatters.startOfDay(IndianFormatters.addDays(nowMs, offset)) }
    }

    val unpaidBills = remember(allBills) { allBills.filter { !it.paid } }
    val paidBills = remember(allBills) { allBills.filter { it.paid }.sortedByDescending { it.paidOnEpochMs ?: it.dueEpochMs } }

    val overdueSum = remember(unpaidBills, nowMs) {
        unpaidBills.filter { IndianFormatters.daysBetween(it.dueEpochMs, nowMs) < 0 }.sumOf { it.amount }
    }
    val next7Sum = remember(unpaidBills, nowMs) {
        unpaidBills.filter { IndianFormatters.daysBetween(it.dueEpochMs, nowMs) in 0..7 }.sumOf { it.amount }
    }
    val paidMonthSum = remember(paidBills, nowMs) {
        paidBills.filter { IndianFormatters.isSameMonth(it.paidOnEpochMs ?: it.dueEpochMs, nowMs) }.sumOf { it.amount }
    }

    val filteredEntries = remember(displayEntries, selectedDayMs) {
        val day = selectedDayMs
        if (day == null) displayEntries else displayEntries.filter { IndianFormatters.sameDay(it.bill.dueEpochMs, day) }
    }

    val overdueList = remember(filteredEntries, nowMs) {
        filteredEntries.filter { !it.bill.paid && IndianFormatters.daysBetween(it.bill.dueEpochMs, nowMs) < 0 }
    }
    val thisWeekList = remember(filteredEntries, nowMs) {
        filteredEntries.filter { !it.bill.paid && IndianFormatters.daysBetween(it.bill.dueEpochMs, nowMs) in 0..7 }
    }
    val laterList = remember(filteredEntries, nowMs) {
        filteredEntries.filter { !it.bill.paid && IndianFormatters.daysBetween(it.bill.dueEpochMs, nowMs) > 7 }
    }
    val paidFiltered = remember(filteredEntries) {
        filteredEntries.filter { it.bill.paid }.sortedByDescending { it.bill.paidOnEpochMs ?: it.bill.dueEpochMs }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelavuMainHeader(
                    title = strings.billsTitle,
                    subtitle = "${unpaidBills.size} upcoming",
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl,
                    onOpenProfile = onOpenProfile,
                    profileTestTag = "bills_profile_button",
                    trailingContent = {
                        Button(
                            onClick = onOpenNewReminder,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("bills_new_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                )
            }

            // Summary strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillSummaryTile(
                        label = strings.overdue,
                        amount = IndianFormatters.formatInr(overdueSum),
                        valueColor = if (overdueSum > 0) colorScheme.error else extendedColors.ok,
                        modifier = Modifier.weight(1f)
                    )
                    BillSummaryTile(
                        label = strings.next7Days,
                        amount = IndianFormatters.formatInr(next7Sum),
                        valueColor = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    BillSummaryTile(
                        label = strings.paidThisMonth,
                        amount = IndianFormatters.formatInr(paidMonthSum),
                        valueColor = extendedColors.ok,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3-week calendar strip
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Upcoming schedule",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurface
                            )
                            if (selectedDayMs != null) {
                                Text(
                                    text = "Show all",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .clickable { selectedDayMs = null }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "Tap a date to filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.outline
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(calendarDays) { dayMs ->
                                val isToday = IndianFormatters.sameDay(dayMs, nowMs)
                                val isSelected = selectedDayMs?.let { IndianFormatters.sameDay(it, dayMs) } ?: false
                                val dayBills = allBills.filter { IndianFormatters.sameDay(it.dueEpochMs, dayMs) }
                                val hasOverdue = dayBills.any { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) < 0 }
                                val hasDue = dayBills.any { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) >= 0 }
                                val hasPaid = dayBills.any { it.paid }

                                val bg = when {
                                    isSelected -> colorScheme.primary
                                    isToday -> colorScheme.primaryContainer
                                    else -> colorScheme.surfaceVariant
                                }
                                val fg = when {
                                    isSelected -> colorScheme.onPrimary
                                    isToday -> colorScheme.onPrimaryContainer
                                    else -> colorScheme.onSurface
                                }
                                Column(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(bg)
                                        .clickable {
                                            selectedDayMs = if (isSelected) null else dayMs
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = IndianFormatters.dayInitial(dayMs),
                                        style = SelavuTheme.textStyles.badgeMicro,
                                        color = fg.copy(alpha = 0.72f)
                                    )
                                    Text(
                                        text = IndianFormatters.dayOfMonth(dayMs).toString(),
                                        style = SelavuTheme.textStyles.numericTitle,
                                        color = fg
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                                        if (hasOverdue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colorScheme.onPrimary else colorScheme.error))
                                        }
                                        if (hasDue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colorScheme.onPrimary else colorScheme.primary))
                                        }
                                        if (hasPaid && !hasOverdue && !hasDue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colorScheme.onPrimary else extendedColors.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredEntries.isEmpty()) {
                item {
                    SelavuEmptyState(
                        icon = Icons.Filled.Notifications,
                        title = if (allBills.isEmpty()) "No bill reminders yet" else "No bills scheduled on this date",
                        subtitle = if (allBills.isEmpty()) {
                            "Add electricity, rent, EMI, or OTT reminders so nothing slips past the due date."
                        } else {
                            "Tap Show all on the schedule strip to view all upcoming reminders."
                        },
                        actionLabel = if (allBills.isEmpty()) strings.newReminder else null,
                        onAction = if (allBills.isEmpty()) onOpenNewReminder else null
                    )
                }
            }

            if (overdueList.isNotEmpty()) {
                item { SectionHeader(title = strings.overdue) }
                items(overdueList, key = { it.bill.id }) { entry ->
                    BillItemCard(
                        bill = entry.bill,
                        categories = categories,
                        nowMs = nowMs,
                        isOwner = entry.isOwner,
                        ownerName = entry.ownerName,
                        onMarkPaid = onMarkPaid,
                        onEditBill = onOpenBillDetail
                    )
                }
            }
            if (thisWeekList.isNotEmpty()) {
                item { SectionHeader(title = strings.thisWeek) }
                items(thisWeekList, key = { it.bill.id }) { entry ->
                    BillItemCard(
                        bill = entry.bill,
                        categories = categories,
                        nowMs = nowMs,
                        isOwner = entry.isOwner,
                        ownerName = entry.ownerName,
                        onMarkPaid = onMarkPaid,
                        onEditBill = onOpenBillDetail
                    )
                }
            }
            if (laterList.isNotEmpty()) {
                item { SectionHeader(title = strings.laterInMonth) }
                items(laterList, key = { it.bill.id }) { entry ->
                    BillItemCard(
                        bill = entry.bill,
                        categories = categories,
                        nowMs = nowMs,
                        isOwner = entry.isOwner,
                        ownerName = entry.ownerName,
                        onMarkPaid = onMarkPaid,
                        onEditBill = onOpenBillDetail
                    )
                }
            }
            if (paidFiltered.isNotEmpty()) {
                item { SectionHeader(title = strings.paid) }
                items(paidFiltered, key = { it.bill.id }) { entry ->
                    BillItemCard(
                        bill = entry.bill,
                        categories = categories,
                        nowMs = nowMs,
                        isOwner = entry.isOwner,
                        ownerName = entry.ownerName,
                        onMarkPaid = onMarkPaid,
                        onEditBill = onOpenBillDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun BillSummaryTile(
    label: String,
    amount: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = amount,
            style = SelavuTheme.textStyles.numericTitle,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun BillItemCard(
    bill: Bill,
    categories: List<Category>,
    nowMs: Long,
    isOwner: Boolean = true,
    ownerName: String? = null,
    onMarkPaid: (Bill) -> Unit,
    onEditBill: (Bill) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = selavuStrings()
    val catColor = categories.firstOrNull { it.id == bill.categoryId }?.colorIndex ?: 4
    val daysDiff = IndianFormatters.daysBetween(bill.dueEpochMs, nowMs)
    val status = when {
        bill.paid -> ServiceStatus.OK
        daysDiff < 0 -> ServiceStatus.OVERDUE
        daysDiff <= 7 -> ServiceStatus.DUE_SOON
        else -> null
    }
    val dueText = when {
        bill.paid -> "Paid ${IndianFormatters.formatShortDate(bill.paidOnEpochMs ?: bill.dueEpochMs)}"
        daysDiff < 0 -> "Overdue ${-daysDiff} days · ${IndianFormatters.formatShortDate(bill.dueEpochMs)}"
        daysDiff == 0 -> "Due today"
        else -> "Due ${IndianFormatters.formatDate(bill.dueEpochMs)}"
    }

    SelavuCard(
        onClick = if (isOwner) ({ onEditBill(bill) }) else null,
        modifier = Modifier.testTag("bill_card_${bill.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryTile(iconKey = bill.iconKey, colorIndex = catColor)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (bill.autopay) {
                        StatusPill(text = strings.autopay, isAccent = true)
                    }
                    if (!isOwner && !ownerName.isNullOrBlank()) {
                        StatusPill(text = "Shared · $ownerName", isAccent = true)
                    } else if (isOwner && bill.hiddenFromWorkspace) {
                        StatusPill(text = "Hidden", status = ServiceStatus.DUE_SOON)
                    }
                }
                Text(
                    text = "$dueText · ${bill.repeat} · ${bill.reminder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == ServiceStatus.OVERDUE) colorScheme.error else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = IndianFormatters.formatInr(bill.amount),
                    style = SelavuTheme.textStyles.numericBody,
                    color = colorScheme.onSurface
                )
                if (!bill.paid && isOwner) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(colorScheme.primaryContainer)
                            .clickable { onMarkPaid(bill) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("mark_paid_${bill.id}")
                    ) {
                        Text(
                            text = strings.markPaid,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                } else if (bill.paid) {
                    StatusPill(text = strings.paid, status = ServiceStatus.OK)
                }
            }
        }
    }
}

@Composable
fun InsightsScreen(
    monthlyBudget: Double,
    categories: List<Category>,
    expenses: List<Expense>,
    vehicles: List<Vehicle>,
    userDisplayName: String? = null,
    userPhotoUrl: String? = null,
    onOpenCategoryDrilldown: (Category) -> Unit,
    onExportGstCsv: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }
    var range by rememberSaveable { mutableStateOf("month") }

    val monthExpenses = remember(expenses, nowMs) {
        expenses.filter { IndianFormatters.isSameMonth(it.dateEpochMs, nowMs) }
    }
    val sixMonthMonths = remember(nowMs) {
        (5 downTo 0).map { offset -> IndianFormatters.addMonths(nowMs, -offset) }
    }
    val sixMonthExpenses = remember(expenses, sixMonthMonths) {
        expenses.filter { exp -> sixMonthMonths.any { m -> IndianFormatters.isSameMonth(exp.dateEpochMs, m) } }
    }

    val slices = remember(range, monthExpenses, sixMonthExpenses, categories) {
        val source = if (range == "month") monthExpenses else sixMonthExpenses
        source.groupBy { it.categoryId }.entries.mapNotNull { (catId, list) ->
            val amt = list.sumOf { it.amount }
            if (amt <= 0.0) null
            else {
                val cat = categories.firstOrNull { it.id == catId }
                CategoryShareSlice(
                    categoryId = catId,
                    name = cat?.name ?: "Other",
                    colorIndex = cat?.colorIndex ?: 9,
                    iconKey = cat?.iconKey ?: "tag",
                    amount = amt
                )
            }
        }.sortedByDescending { it.amount }
    }

    val totalShown = remember(slices) { slices.sumOf { it.amount } }

    val sixMonthSeries = remember(expenses, sixMonthMonths) {
        sixMonthMonths.map { m ->
            val sum = expenses.filter { IndianFormatters.isSameMonth(it.dateEpochMs, m) }.sumOf { it.amount }
            IndianFormatters.monthLabel(m) to sum
        }
    }

    val fuelSpend = remember(monthExpenses) {
        monthExpenses.filter { it.categoryId == "veh" && it.subcategory.equals("Fuel", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val subMonthlyTotal = remember(monthExpenses) {
        monthExpenses.filter { it.categoryId == "subs" }.sumOf { it.amount }
    }
    val gstCreditTotal = remember(expenses) {
        expenses.sumOf { (it.cgst ?: 0.0) + (it.sgst ?: 0.0) + (it.igst ?: 0.0) }
    }
    val gstInvoiceCount = remember(expenses) {
        expenses.count { !it.gstin.isNullOrBlank() || (it.cgst ?: 0.0) > 0.0 || (it.igst ?: 0.0) > 0.0 }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelavuMainHeader(
                    title = strings.insightsTitle,
                    subtitle = "Category breakdown & tax summary",
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl,
                    onOpenProfile = onOpenProfile,
                    profileTestTag = "insights_profile_button"
                )
                SelavuSegmentedControl(
                    options = listOf("month" to strings.thisMonth, "6m" to strings.sixMonths),
                    selectedKey = range,
                    onSelect = { range = it }
                )
            }

            item {
                SelavuCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.byCategory, style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                            Text(strings.tapToSeeEntries, style = MaterialTheme.typography.bodySmall, color = colorScheme.outline)
                        }

                        DonutSpendChart(slices = slices, totalAmount = totalShown)

                        if (slices.isEmpty()) {
                            Text(
                                text = "No expenses logged in this period yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val denom = totalShown.coerceAtLeast(1.0)
                                slices.forEachIndexed { idx, slice ->
                                    if (idx > 0) SelavuDivider()
                                    val pct = ((slice.amount / denom) * 100.0).roundToInt()
                                    val cat = categories.firstOrNull { it.id == slice.categoryId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (cat != null) onOpenCategoryDrilldown(cat)
                                            }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(extendedColors.categoryColor(slice.colorIndex))
                                        )
                                        Text(
                                            text = slice.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = IndianFormatters.formatInr(slice.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = strings.monthlyTrend)
                SelavuCard {
                    MonthlyTrendBarChart(
                        months = sixMonthSeries,
                        budget = monthlyBudget
                    )
                }
            }

            item {
                SectionHeader(title = strings.worthALook)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val primaryBike = vehicles.firstOrNull()
                    InsightCalloutCard(
                        iconKey = "fuel",
                        colorIndex = 1,
                        title = "${primaryBike?.shortName ?: "Vehicle"} fuel this month",
                        body = if (fuelSpend > 0) {
                            "${IndianFormatters.formatInr(fuelSpend)} spent on fuel so far (${primaryBike?.avgMileage?.toInt() ?: 34} km/l avg). Logging odometer on every fill keeps mileage exact."
                        } else {
                            "Log fuel entries with your vehicle selected to track real km/l efficiency."
                        }
                    )
                    InsightCalloutCard(
                        iconKey = "tv",
                        colorIndex = 5,
                        title = "Recurring subscriptions",
                        body = "${IndianFormatters.formatInr(subMonthlyTotal)}/month across OTT & cloud apps (${IndianFormatters.formatInr(subMonthlyTotal * 12)}/year)."
                    )
                    SelavuCard(onClick = onExportGstCsv) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryTile(iconKey = "file", colorIndex = 7)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "GST input credit logged",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "${IndianFormatters.formatInr(gstCreditTotal)} tax across $gstInvoiceCount GST bills. Tap to export CSV for CA.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.Download, contentDescription = "Export CSV", tint = colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCalloutCard(
    iconKey: String,
    colorIndex: Int,
    title: String,
    body: String
) {
    val colorScheme = MaterialTheme.colorScheme
    SelavuCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryTile(iconKey = iconKey, colorIndex = colorIndex)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActivityScreen(
    categories: List<Category>,
    expenses: List<Expense>,
    currentUid: String = "",
    workspace: Workspace? = null,
    sharedExpenses: List<SharedExpense> = emptyList(),
    onBack: () -> Unit,
    onOpenExpenseDetail: (Expense) -> Unit,
    onOpenSharedExpenseDetail: (SharedExpense) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = selavuStrings()
    var query by rememberSaveable { mutableStateOf("") }
    var filterKey by rememberSaveable { mutableStateOf("all") }
    var scopeName by rememberSaveable { mutableStateOf(ExpenseScope.INDIVIDUAL.name) }
    var wsFilterName by rememberSaveable { mutableStateOf(WorkspaceExpenseFilter.ALL.name) }

    LaunchedEffect(workspace) {
        if (workspace == null) scopeName = ExpenseScope.INDIVIDUAL.name
    }
    val scope = if (workspace != null && scopeName == ExpenseScope.WORKSPACE.name) {
        ExpenseScope.WORKSPACE
    } else {
        ExpenseScope.INDIVIDUAL
    }
    val wsFilter = WorkspaceExpenseFilter.entries.firstOrNull { it.name == wsFilterName }
        ?: WorkspaceExpenseFilter.ALL

    val filtered = remember(expenses, query, filterKey) {
        val q = query.trim().lowercase()
        expenses.filter { exp ->
            val matchesFilter = when (filterKey) {
                "all" -> true
                "gst" -> !exp.gstin.isNullOrBlank() || (exp.cgst ?: 0.0) > 0.0 || (exp.igst ?: 0.0) > 0.0
                "items" -> !exp.items.isNullOrEmpty()
                else -> exp.categoryId == filterKey
            }
            val matchesQuery = q.isEmpty() ||
                exp.title.lowercase().contains(q) ||
                exp.subcategory.lowercase().contains(q) ||
                exp.gstin.orEmpty().lowercase().contains(q) ||
                exp.billNumber.orEmpty().lowercase().contains(q) ||
                (exp.items?.any {
                    it.name.lowercase().contains(q) || it.hsn.lowercase().contains(q)
                } == true)
            matchesFilter && matchesQuery
        }.sortedByDescending { it.dateEpochMs }
    }

    val filteredShared = remember(sharedExpenses, wsFilter, currentUid, query) {
        val q = query.trim().lowercase()
        sharedExpenses.filter { shared ->
            val matchesMember = when (wsFilter) {
                WorkspaceExpenseFilter.ALL -> true
                WorkspaceExpenseFilter.OTHERS_ONLY -> shared.ownerUid != currentUid
                WorkspaceExpenseFilter.MINE_ONLY -> shared.ownerUid == currentUid
            }
            val matchesQuery = q.isEmpty() ||
                shared.title.lowercase().contains(q) ||
                shared.ownerName.lowercase().contains(q) ||
                shared.categoryName.lowercase().contains(q) ||
                shared.subcategory.lowercase().contains(q)
            matchesMember && matchesQuery
        }.sortedByDescending { it.dateEpochMs }
    }

    val totalFiltered = remember(scope, filtered, filteredShared) {
        if (scope == ExpenseScope.WORKSPACE) filteredShared.sumOf { it.amount } else filtered.sumOf { it.amount }
    }
    val countFiltered = if (scope == ExpenseScope.WORKSPACE) filteredShared.size else filtered.size

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelavuTopNavBar(title = strings.allActivity, onBack = onBack)
                if (workspace != null) {
                    SelavuSegmentedControl(
                        options = listOf(
                            ExpenseScope.INDIVIDUAL.name to strings.scopeIndividual,
                            ExpenseScope.WORKSPACE.name to workspace.name
                        ),
                        selectedKey = scope.name,
                        onSelect = { scopeName = it },
                        modifier = Modifier.testTag("activity_scope_selector")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                SelavuTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = strings.searchEntries,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colorScheme.onSurfaceVariant) },
                    containerColor = colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scope == ExpenseScope.WORKSPACE) {
                        item {
                            SelavuFilterChip(
                                label = strings.filterAllShared,
                                selected = wsFilter == WorkspaceExpenseFilter.ALL,
                                onClick = { wsFilterName = WorkspaceExpenseFilter.ALL.name },
                                modifier = Modifier.testTag("activity_ws_all")
                            )
                        }
                        item {
                            SelavuFilterChip(
                                label = strings.filterOthersOnly,
                                selected = wsFilter == WorkspaceExpenseFilter.OTHERS_ONLY,
                                onClick = { wsFilterName = WorkspaceExpenseFilter.OTHERS_ONLY.name },
                                modifier = Modifier.testTag("activity_ws_others")
                            )
                        }
                        item {
                            SelavuFilterChip(
                                label = strings.filterMineShared,
                                selected = wsFilter == WorkspaceExpenseFilter.MINE_ONLY,
                                onClick = { wsFilterName = WorkspaceExpenseFilter.MINE_ONLY.name },
                                modifier = Modifier.testTag("activity_ws_mine")
                            )
                        }
                    } else {
                        item {
                            SelavuFilterChip(
                                label = "All (${expenses.size})",
                                selected = filterKey == "all",
                                onClick = { filterKey = "all" }
                            )
                        }
                        item {
                            SelavuFilterChip(
                                label = "GST bills",
                                selected = filterKey == "gst",
                                onClick = { filterKey = "gst" }
                            )
                        }
                        item {
                            SelavuFilterChip(
                                label = "Itemized",
                                selected = filterKey == "items",
                                onClick = { filterKey = "items" }
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            SelavuFilterChip(
                                label = cat.name.substringBefore(" & "),
                                selected = filterKey == cat.id,
                                onClick = { filterKey = cat.id }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$countFiltered entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total ${IndianFormatters.formatInr(totalFiltered)}",
                        style = SelavuTheme.textStyles.numericCaption,
                        color = colorScheme.onSurface
                    )
                }
            }

            item {
                if (scope == ExpenseScope.WORKSPACE) {
                    if (filteredShared.isEmpty()) {
                        SelavuEmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = "No matching shared expenses",
                            subtitle = "Adjust the member filter or search text."
                        )
                    } else {
                        SelavuListContainer {
                            filteredShared.forEachIndexed { idx, item ->
                                if (idx > 0) SelavuDivider()
                                SharedExpenseRowItem(
                                    expense = item,
                                    isMine = item.ownerUid == currentUid,
                                    onClick = { onOpenSharedExpenseDetail(item) }
                                )
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    SelavuEmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = "No matching expenses",
                        subtitle = if (query.isBlank() && filterKey == "all") {
                            "Entries you add or scan will appear here for search and GST export."
                        } else {
                            "Try clearing your search or switching the category filter."
                        }
                    )
                } else {
                    SelavuListContainer {
                        filtered.forEachIndexed { idx, expense ->
                            if (idx > 0) SelavuDivider()
                            ExpenseRowItem(
                                expense = expense,
                                category = categories.firstOrNull { it.id == expense.categoryId },
                                onClick = { onOpenExpenseDetail(expense) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesAndSettingsScreen(
    profile: UserProfile,
    user: FirebaseUser,
    categories: List<Category>,
    sharedCategories: List<SharedCategory> = emptyList(),
    expenses: List<Expense>,
    themeMode: String,
    reviewDraftCount: Int = 0,
    smsAutoReadEnabled: Boolean = false,
    workspace: Workspace? = null,
    onBack: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeThemeMode: (String) -> Unit,
    onEditBudget: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onSignOut: () -> Unit,
    onShowToast: (String) -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onRestoreCategories: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onOpenWorkspace: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val effectiveName = profile.displayName?.takeIf { it.isNotBlank() }
        ?: user.displayName?.takeIf { it.isNotBlank() }
        ?: "RecapSpends User"

    var showEditNameDialog by remember { mutableStateOf(false) }
    var draftDisplayName by rememberSaveable(effectiveName) { mutableStateOf(effectiveName) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    var languageExpanded by rememberSaveable { mutableStateOf(false) }
    var budgetExpanded by rememberSaveable { mutableStateOf(false) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Edit username / display name",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                SelavuTextField(
                    value = draftDisplayName,
                    onValueChange = { draftDisplayName = it },
                    label = "Display name",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
            },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    onClick = {
                        if (draftDisplayName.trim().isNotEmpty()) {
                            onUpdateDisplayName(draftDisplayName.trim())
                            showEditNameDialog = false
                        } else {
                            onShowToast("Display name cannot be empty")
                        }
                    },
                    modifier = Modifier.testTag("save_display_name_button")
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Delete all data & start fresh?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "This permanently deletes all your expenses, vehicles, service records, bill reminders, projects, and categories from your account. Your login stays active so you can start completely fresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    onClick = {
                        showClearDataDialog = false
                        onClearAllData()
                    },
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) {
                    Text("Delete all data", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Delete account permanently?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "This permanently erases all your RecapSpends data in Firestore and deletes your login account. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete account", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    val gstCsvContent = remember(expenses, categories) {
        buildString {
            appendLine("Date,Merchant,Category,Subcategory,PaymentMode,BillNumber,GSTIN,TaxableValue,CGST,SGST,IGST,RoundOff,GrandTotal,Items")
            expenses.sortedByDescending { it.dateEpochMs }.forEach { e ->
                val catName = categories.firstOrNull { it.id == e.categoryId }?.name ?: e.categoryId
                val itemsSummary = e.items?.joinToString("; ") { "${it.name} x${it.qty} (HSN ${it.hsn} @${it.gstRate}%)" }.orEmpty()
                fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""
                appendLine(
                    listOf(
                        esc(IndianFormatters.formatDate(e.dateEpochMs)),
                        esc(e.title),
                        esc(catName),
                        esc(e.subcategory),
                        esc(e.paymentMode),
                        esc(e.billNumber.orEmpty()),
                        esc(e.gstin.orEmpty()),
                        e.taxableValue?.toString().orEmpty(),
                        e.cgst?.toString().orEmpty(),
                        e.sgst?.toString().orEmpty(),
                        e.igst?.toString().orEmpty(),
                        e.roundOff?.toString().orEmpty(),
                        e.amount.toString(),
                        esc(itemsSummary)
                    ).joinToString(",")
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelavuTopNavBar(title = strings.categoriesTitle, onBack = onBack)
            }

            // Top Profile Details (outside sections)
            item {
                SelavuCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (user.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = effectiveName.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = effectiveName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = {
                                        draftDisplayName = effectiveName
                                        showEditNameDialog = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("edit_display_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit display name",
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = user.email ?: "No email linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // System Theme Selector (outside sections)
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface
                        )
                        SelavuSegmentedControl(
                            options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                            selectedKey = themeMode,
                            onSelect = onChangeThemeMode,
                            modifier = Modifier.testTag("theme_selector")
                        )
                    }
                }
            }

            // Section 1: Language
            item {
                val langLabel = if (profile.language == "ta") "தமிழ் (Tamil)" else "English"
                ExpandableSettingsSection(
                    icon = Icons.Filled.Language,
                    title = strings.languageLabel,
                    summary = langLabel,
                    expanded = languageExpanded,
                    onToggle = { languageExpanded = !languageExpanded },
                    headerTestTag = "settings_section_language"
                ) {
                    SelavuSegmentedControl(
                        options = listOf("en" to "English", "ta" to "தமிழ் (Tamil)"),
                        selectedKey = profile.language,
                        onSelect = onChangeLanguage,
                        modifier = Modifier.testTag("language_selector")
                    )
                }
            }

            // Section 2: Monthly Budget
            item {
                ExpandableSettingsSection(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = strings.budgetLabel,
                    summary = IndianFormatters.formatInr(profile.monthlyBudget),
                    expanded = budgetExpanded,
                    onToggle = { budgetExpanded = !budgetExpanded },
                    headerTestTag = "settings_section_budget"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(colorScheme.surfaceVariant)
                            .clickable(onClick = onEditBudget)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("edit_budget_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Current monthly target",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to adjust your monthly spending limit",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = IndianFormatters.formatInr(profile.monthlyBudget),
                                style = SelavuTheme.textStyles.numericTitle,
                                color = colorScheme.onPrimaryContainer
                            )
                            Icon(Icons.Filled.Edit, contentDescription = "Edit budget", tint = colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Section 3: Categories
            item {
                val othersSharedCats = remember(sharedCategories, user.uid) {
                    sharedCategories.filter { it.ownerUid != user.uid }
                }
                val catSummary = if (othersSharedCats.isNotEmpty()) {
                    "${categories.size} yours · ${othersSharedCats.size} shared"
                } else {
                    "${categories.size} configured"
                }
                ExpandableSettingsSection(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    summary = catSummary,
                    expanded = categoriesExpanded,
                    onToggle = { categoriesExpanded = !categoriesExpanded },
                    headerTestTag = "settings_section_categories"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onAddCategory,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_category_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.newCategory, style = MaterialTheme.typography.labelLarge)
                        }

                        if (categories.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "No categories yet. Add your own or restore the 9 starter Indian categories.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onRestoreCategories,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("restore_categories_button")
                                ) {
                                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore starter categories")
                                }
                            }
                        } else {
                            SelavuListContainer {
                                categories.forEachIndexed { idx, cat ->
                                    if (idx > 0) SelavuDivider()
                                    val count = expenses.count { it.categoryId == cat.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEditCategory(cat) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("category_row_${cat.id}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CategoryTile(iconKey = cat.iconKey, colorIndex = cat.colorIndex)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            val hiddenSuffix = if (cat.hiddenFromWorkspace) " · Hidden from workspace" else ""
                                            Text(
                                                text = "${cat.subcategories.size} sub-categories · $count entries$hiddenSuffix",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (cat.hiddenFromWorkspace) {
                                            StatusPill(text = "Hidden", status = ServiceStatus.DUE_SOON)
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Edit category",
                                            tint = colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        if (othersSharedCats.isNotEmpty()) {
                            Text(
                                text = "Shared by workspace members (${othersSharedCats.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            SelavuListContainer {
                                othersSharedCats.forEachIndexed { idx, sc ->
                                    if (idx > 0) SelavuDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("shared_category_row_${sc.id}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CategoryTile(iconKey = sc.iconKey, colorIndex = sc.colorIndex)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${sc.subcategories.size} sub-categories · ${sc.subcategories.take(3).joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        StatusPill(text = "Shared · ${sc.ownerName}", isAccent = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Advanced settings (SMS alerts, Workspace, CA export, App updates, Privacy & Legal, Danger Zone)
            item {
                ExpandableSettingsSection(
                    icon = Icons.Filled.Tune,
                    title = "Advanced settings",
                    summary = "SMS alerts, Workspace, Export, Updates & Data",
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded },
                    headerTestTag = "settings_section_advanced"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Review alerts & Family Workspace
                        SelavuListContainer {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenReview)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("settings_open_review_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.reviewAlerts,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (smsAutoReadEnabled) {
                                            "SMS auto-detect ON (today onwards · OTPs ignored) · $reviewDraftCount pending"
                                        } else {
                                            "Opt-in SMS reader (internal testing) · $reviewDraftCount pending"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                if (reviewDraftCount > 0) {
                                    StatusPill("$reviewDraftCount", isAccent = true)
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open review alerts",
                                    tint = colorScheme.outline
                                )
                            }
                            SelavuDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenWorkspace)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("settings_open_workspace_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.familyWorkspace,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (workspace != null) {
                                            "${workspace.name} · ${workspace.memberIds.size} members"
                                        } else {
                                            "Create or join a shared workspace with family"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open family workspace",
                                    tint = colorScheme.outline
                                )
                            }
                        }

                        // CA GST Export
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "RecapSpends Expense & GST Report")
                                        putExtra(Intent.EXTRA_TEXT, gstCsvContent)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export CSV for CA"))
                                },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primaryContainer,
                                    contentColor = colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_gst_csv_button")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.caExportNotice, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(gstCsvContent))
                                    onShowToast("GST CSV copied to clipboard (${expenses.size} entries)")
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy CSV", modifier = Modifier.size(18.dp))
                            }
                        }

                        SelavuDivider()

                        // App updates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "RecapSpends v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Build ${BuildConfig.VERSION_CODE} · Play Flexible updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onCheckForUpdates,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("settings_check_updates_button")
                            ) {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check updates", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        SelavuDivider()

                        // Privacy & Legal
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = extendedColors.ok,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Private to your account: only you can view, edit, or delete your expenses, vehicles, bills, and projects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        val termsUrl = stringResource(R.string.terms_url)
                        val policiesUrl = stringResource(R.string.policies_url)
                        val privacyUrl = stringResource(R.string.privacy_url)
                        fun openLegalUrl(url: String) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                onShowToast("Open $url")
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openLegalUrl(termsUrl) },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("settings_terms_link")
                            ) {
                                Text("Terms", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            OutlinedButton(
                                onClick = { openLegalUrl(policiesUrl) },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("settings_policies_link")
                            ) {
                                Text("Policies", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            OutlinedButton(
                                onClick = { openLegalUrl(privacyUrl) },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("settings_privacy_link")
                            ) {
                                Text("Privacy Notice", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }

                        SelavuDivider()

                        // Danger Zone
                        Text(
                            text = "Danger zone",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete all data (Start fresh)", color = colorScheme.error)
                        }

                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.error,
                                contentColor = colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_account_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete account permanently", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Bottom Sign Out Button (outside sections)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun ExpandableSettingsSection(
    icon: ImageVector,
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    headerTestTag: String,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    SelavuCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onToggle)
                    .padding(vertical = 2.dp)
                    .testTag(headerTestTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = colorScheme.outline
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelavuDivider()
                    content()
                }
            }
        }
    }
}
