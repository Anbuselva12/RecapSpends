package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ServiceStatus
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.ui.components.CategoryShareSlice
import com.example.ui.components.CategoryTile
import com.example.ui.components.DonutSpendChart
import com.example.ui.components.MonthlyTrendBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import com.google.firebase.auth.FirebaseUser
import kotlin.math.roundToInt

@Composable
fun BillsScreen(
    bills: List<Bill>,
    categories: List<Category>,
    onMarkPaid: (Bill) -> Unit,
    onOpenNewReminder: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }
    var selectedDayMs by remember { mutableStateOf<Long?>(null) }

    val calendarDays = remember(nowMs) {
        (-3..17).map { offset -> IndianFormatters.startOfDay(IndianFormatters.addDays(nowMs, offset)) }
    }

    val unpaidBills = remember(bills) { bills.filter { !it.paid } }
    val paidBills = remember(bills) { bills.filter { it.paid }.sortedByDescending { it.paidOnEpochMs ?: it.dueEpochMs } }

    val overdueSum = remember(unpaidBills, nowMs) {
        unpaidBills.filter { IndianFormatters.daysBetween(it.dueEpochMs, nowMs) < 0 }.sumOf { it.amount }
    }
    val next7Sum = remember(unpaidBills, nowMs) {
        unpaidBills.filter { IndianFormatters.daysBetween(it.dueEpochMs, nowMs) in 0..7 }.sumOf { it.amount }
    }
    val paidMonthSum = remember(paidBills, nowMs) {
        paidBills.filter { IndianFormatters.isSameMonth(it.paidOnEpochMs ?: it.dueEpochMs, nowMs) }.sumOf { it.amount }
    }

    val filteredBills = remember(bills, selectedDayMs) {
        val day = selectedDayMs
        if (day == null) bills else bills.filter { IndianFormatters.sameDay(it.dueEpochMs, day) }
    }

    val overdueList = remember(filteredBills, nowMs) {
        filteredBills.filter { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) < 0 }
    }
    val thisWeekList = remember(filteredBills, nowMs) {
        filteredBills.filter { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) in 0..7 }
    }
    val laterList = remember(filteredBills, nowMs) {
        filteredBills.filter { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) > 7 }
    }
    val paidFiltered = remember(filteredBills) {
        filteredBills.filter { it.paid }.sortedByDescending { it.paidOnEpochMs ?: it.dueEpochMs }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.billsTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenNewReminder,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("bills_new_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                        }
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenProfile)
                                .testTag("bills_profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile and settings",
                                tint = colors.ink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
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
                        valueColor = if (overdueSum > 0) colors.bad else colors.ok,
                        modifier = Modifier.weight(1f)
                    )
                    BillSummaryTile(
                        label = strings.next7Days,
                        amount = IndianFormatters.formatInr(next7Sum),
                        valueColor = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    BillSummaryTile(
                        label = strings.paidThisMonth,
                        amount = IndianFormatters.formatInr(paidMonthSum),
                        valueColor = colors.ok,
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
                                color = colors.ink
                            )
                            if (selectedDayMs != null) {
                                Text(
                                    text = "Show all",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accentInk,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedDayMs = null }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "Tap a date to filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink3
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(calendarDays) { dayMs ->
                                val isToday = IndianFormatters.sameDay(dayMs, nowMs)
                                val isSelected = selectedDayMs?.let { IndianFormatters.sameDay(it, dayMs) } ?: false
                                val dayBills = bills.filter { IndianFormatters.sameDay(it.dueEpochMs, dayMs) }
                                val hasOverdue = dayBills.any { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) < 0 }
                                val hasDue = dayBills.any { !it.paid && IndianFormatters.daysBetween(it.dueEpochMs, nowMs) >= 0 }
                                val hasPaid = dayBills.any { it.paid }

                                val bg = when {
                                    isSelected -> colors.accent
                                    isToday -> colors.accentSoft
                                    else -> colors.sunk
                                }
                                val fg = when {
                                    isSelected -> colors.onAccent
                                    isToday -> colors.accentInk
                                    else -> colors.ink
                                }
                                Column(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = fg.copy(alpha = 0.72f)
                                    )
                                    Text(
                                        text = IndianFormatters.dayOfMonth(dayMs).toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = fg
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                                        if (hasOverdue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colors.onAccent else colors.bad))
                                        }
                                        if (hasDue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colors.onAccent else colors.accent))
                                        }
                                        if (hasPaid && !hasOverdue && !hasDue) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) colors.onAccent else colors.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredBills.isEmpty()) {
                item {
                    SelavuCard {
                        Text(
                            text = if (bills.isEmpty()) {
                                "No bill reminders yet. Tap + New to add electricity, rent, EMI, or OTT reminders."
                            } else {
                                "No bills scheduled for this date."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink2
                        )
                    }
                }
            }

            if (overdueList.isNotEmpty()) {
                item { SectionHeader(title = strings.overdue) }
                items(overdueList, key = { it.id }) { bill ->
                    BillItemCard(bill, categories, nowMs, onMarkPaid)
                }
            }
            if (thisWeekList.isNotEmpty()) {
                item { SectionHeader(title = strings.thisWeek) }
                items(thisWeekList, key = { it.id }) { bill ->
                    BillItemCard(bill, categories, nowMs, onMarkPaid)
                }
            }
            if (laterList.isNotEmpty()) {
                item { SectionHeader(title = strings.laterInMonth) }
                items(laterList, key = { it.id }) { bill ->
                    BillItemCard(bill, categories, nowMs, onMarkPaid)
                }
            }
            if (paidFiltered.isNotEmpty()) {
                item { SectionHeader(title = strings.paid) }
                items(paidFiltered, key = { it.id }) { bill ->
                    BillItemCard(bill, categories, nowMs, onMarkPaid)
                }
            }
        }
    }
}

@Composable
private fun BillSummaryTile(
    label: String,
    amount: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum"
            ),
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
    onMarkPaid: (Bill) -> Unit
) {
    val colors = SelavuTheme.colors
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

    SelavuCard {
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
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (bill.autopay) {
                        StatusPill(text = strings.autopay, isAccent = true)
                    }
                }
                Text(
                    text = "$dueText · ${bill.repeat} · ${bill.reminder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == ServiceStatus.OVERDUE) colors.bad else colors.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = IndianFormatters.formatInr(bill.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = colors.ink
                )
                if (!bill.paid) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accentSoft)
                            .clickable { onMarkPaid(bill) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("mark_paid_${bill.id}")
                    ) {
                        Text(
                            text = strings.markPaid,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.accentInk
                        )
                    }
                } else {
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
    onOpenCategoryDrilldown: (Category) -> Unit,
    onExportGstCsv: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val colors = SelavuTheme.colors
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.insightsTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.ink
                    )
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenProfile)
                            .testTag("insights_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile and settings",
                            tint = colors.ink,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
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
                            Text(strings.byCategory, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(strings.tapToSeeEntries, style = MaterialTheme.typography.bodySmall, color = colors.ink3)
                        }

                        DonutSpendChart(slices = slices, totalAmount = totalShown)

                        if (slices.isEmpty()) {
                            Text(
                                text = "No expenses logged in this period yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink2
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
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(colors.categoryColor(slice.colorIndex))
                                        )
                                        Text(
                                            text = slice.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.ink,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.ink2
                                        )
                                        Text(
                                            text = IndianFormatters.formatInr(slice.amount),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = colors.ink
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
                                    color = colors.ink
                                )
                                Text(
                                    text = "${IndianFormatters.formatInr(gstCreditTotal)} tax across $gstInvoiceCount GST bills. Tap to export CSV for CA.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                            }
                            Icon(Icons.Filled.Download, contentDescription = "Export CSV", tint = colors.accentInk)
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
    val colors = SelavuTheme.colors
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
                    color = colors.ink
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink2
                )
            }
        }
    }
}

@Composable
fun ActivityScreen(
    categories: List<Category>,
    expenses: List<Expense>,
    onBack: () -> Unit,
    onOpenExpenseDetail: (Expense) -> Unit
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    var query by rememberSaveable { mutableStateOf("") }
    var filterKey by rememberSaveable { mutableStateOf("all") }

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

    val totalFiltered = remember(filtered) { filtered.sumOf { it.amount } }

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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchEntries) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filtered.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.ink2
                    )
                    Text(
                        text = "Total ${IndianFormatters.formatInr(totalFiltered)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.ink
                    )
                }
            }

            item {
                SelavuListContainer {
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No matching expenses found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink2,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
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
    expenses: List<Expense>,
    themeMode: String,
    onBack: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeThemeMode: (String) -> Unit,
    onEditBudget: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onSignOut: () -> Unit,
    onShowToast: (String) -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onSendPasswordReset: (String) -> Unit = {},
    onRestoreCategories: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val effectiveName = profile.displayName?.takeIf { it.isNotBlank() }
        ?: user.displayName?.takeIf { it.isNotBlank() }
        ?: "RecapSpends User"

    val providerLabels = remember(user) {
        val ids = user.providerData.map { it.providerId }.toSet()
        buildList {
            if ("google.com" in ids) add("Google")
            if ("password" in ids) add("Email & Password")
        }.ifEmpty { listOf("Authenticated") }.joinToString(" · ")
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var draftDisplayName by rememberSaveable(effectiveName) { mutableStateOf(effectiveName) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit username / display name") },
            text = {
                OutlinedTextField(
                    value = draftDisplayName,
                    onValueChange = { draftDisplayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
            },
            confirmButton = {
                Button(
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Delete all data & start fresh?") },
            text = {
                Text(
                    "This permanently deletes all your expenses, vehicles, service records, bill reminders, projects, and categories from your account. Your login stays active so you can start completely fresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink2
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.bad,
                        contentColor = colors.onAccent
                    ),
                    onClick = {
                        showClearDataDialog = false
                        onClearAllData()
                    },
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) {
                    Text("Delete all data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete account permanently?") },
            text = {
                Text(
                    "This permanently erases all your RecapSpends data in Firestore and deletes your login account. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink2
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.bad,
                        contentColor = colors.onAccent
                    ),
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelavuTopNavBar(title = strings.categoriesTitle, onBack = onBack)
            }

            // User Profile & Authentication Card
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        .size(52.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = effectiveName.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = colors.accentInk
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
                                        color = colors.ink,
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
                                            .size(32.dp)
                                            .testTag("edit_display_name_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit display name",
                                            tint = colors.accentInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = user.email ?: "No email linked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Signed in via $providerLabels",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accentInk
                                )
                            }
                        }

                        SelavuDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Account ID (Private)", style = MaterialTheme.typography.labelSmall, color = colors.ink3)
                                Text(
                                    text = "${user.uid.take(10)}…${user.uid.takeLast(4)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(user.uid))
                                    onShowToast("Account ID copied")
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("copy_uid_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy ID", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val userEmail = user.email
                            if (!userEmail.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onSendPasswordReset(userEmail) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("profile_reset_password_button")
                                ) {
                                    Icon(Icons.Filled.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset password", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Button(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.sunk,
                                    contentColor = colors.ink
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.signOut, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Preferences & CA Export
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge, color = colors.ink)
                            SelavuSegmentedControl(
                                options = listOf("en" to "English", "ta" to "தமிழ் (Tamil)"),
                                selectedKey = profile.language,
                                onSelect = onChangeLanguage,
                                modifier = Modifier.testTag("language_selector")
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                            SelavuSegmentedControl(
                                options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                                selectedKey = themeMode,
                                onSelect = onChangeThemeMode,
                                modifier = Modifier.testTag("theme_selector")
                            )
                        }

                        SelavuDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onEditBudget)
                                .padding(vertical = 4.dp)
                                .testTag("edit_budget_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.budgetLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                                Text("Tap to adjust monthly target", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = IndianFormatters.formatInr(profile.monthlyBudget),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colors.accentInk
                                )
                                Icon(Icons.Filled.Edit, contentDescription = "Edit budget", tint = colors.ink3, modifier = Modifier.size(16.dp))
                            }
                        }

                        SelavuDivider()

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
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accentSoft,
                                    contentColor = colors.accentInk
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy CSV", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Categories
            item {
                SectionHeader(
                    title = "Categories (${categories.size})",
                    actionLabel = "+ ${strings.newCategory}",
                    onAction = onAddCategory
                )
                SelavuListContainer {
                    if (categories.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No categories yet. Add your own or restore the 9 starter Indian categories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink2
                            )
                            OutlinedButton(
                                onClick = onRestoreCategories,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("restore_categories_button")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore starter categories")
                            }
                        }
                    } else {
                        categories.forEachIndexed { idx, cat ->
                            if (idx > 0) SelavuDivider()
                            val count = expenses.count { it.categoryId == cat.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditCategory(cat) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("category_row_${cat.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryTile(iconKey = cat.iconKey, colorIndex = cat.colorIndex)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.ink
                                    )
                                    Text(
                                        text = "${cat.subcategories.size} sub-categories · $count entries",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Edit category",
                                    tint = colors.ink3
                                )
                            }
                        }
                    }
                }
            }

            // Privacy, Fresh Start & Delete Account
            item {
                SectionHeader(title = "Privacy & data")
                SelavuCard(modifier = Modifier.testTag("privacy_data_card")) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = colors.ok,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Private to your account: only you can view, edit, or delete your expenses, vehicles, bills, and projects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.ink2
                            )
                        }

                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = colors.bad, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete all data (Start fresh)", color = colors.bad)
                        }

                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.bad,
                                contentColor = colors.onAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("delete_account_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete account permanently")
                        }
                    }
                }
            }
        }
    }
}
