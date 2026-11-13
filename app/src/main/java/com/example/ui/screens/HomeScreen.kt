package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttentionAlert
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.ui.components.CategoryShareSlice
import com.example.ui.components.CategoryStackedBar
import com.example.ui.components.CategoryTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuProgressBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    monthlyBudget: Double,
    categories: List<Category>,
    expenses: List<Expense>,
    alerts: List<AttentionAlert>,
    onOpenCategories: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenLogService: () -> Unit,
    onOpenNewReminder: () -> Unit,
    onOpenVehicle: (String) -> Unit,
    onOpenBills: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenExpenseDetail: (Expense) -> Unit,
    onEditBudget: () -> Unit
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    val monthExpenses = remember(expenses, nowMs) {
        expenses.filter { IndianFormatters.isSameMonth(it.dateEpochMs, nowMs) }
    }
    val monthTotal = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val percentUsed = remember(monthTotal, monthlyBudget) {
        if (monthlyBudget > 0) ((monthTotal / monthlyBudget) * 100.0).roundToInt().coerceAtMost(999) else 0
    }
    val daysLeft = remember(nowMs) { IndianFormatters.daysLeftInMonth(nowMs) }

    val categorySlices = remember(monthExpenses, categories) {
        val grouped = monthExpenses.groupBy { it.categoryId }
        grouped.entries.map { (catId, list) ->
            val cat = categories.firstOrNull { it.id == catId }
            CategoryShareSlice(
                categoryId = catId,
                name = cat?.name ?: "Other",
                colorIndex = cat?.colorIndex ?: 9,
                iconKey = cat?.iconKey ?: "tag",
                amount = list.sumOf { it.amount }
            )
        }.sortedByDescending { it.amount }
    }

    val recentExpenses = remember(expenses) {
        expenses.sortedByDescending { it.dateEpochMs }.take(6)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = IndianFormatters.fullMonthHeader(nowMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink3
                        )
                        Text(
                            text = strings.goodEvening,
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.ink
                        )
                    }
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenCategories)
                            .testTag("home_settings_button"),
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

            // Monthly Spend Hero Card
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.hero)
                        .clickable(onClick = onEditBudget)
                        .padding(18.dp)
                        .testTag("home_hero_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.monthSoFar,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.heroFg.copy(alpha = 0.82f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$daysLeft ${strings.daysLeft}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.heroFg
                            )
                        }
                    }

                    Text(
                        text = IndianFormatters.formatInr(monthTotal),
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.heroFg,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                    )

                    SelavuProgressBar(
                        progress = if (monthlyBudget > 0) (monthTotal / monthlyBudget).toFloat() else 0f,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        height = 8.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val remaining = (monthlyBudget - monthTotal).coerceAtLeast(0.0)
                        Text(
                            text = "${IndianFormatters.formatInr(remaining)} ${strings.leftOf} ${IndianFormatters.formatInr(monthlyBudget)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.heroFg.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "$percentUsed${strings.usedPercent}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.heroFg.copy(alpha = 0.95f)
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        label = strings.scanBill,
                        icon = Icons.Filled.DocumentScanner,
                        isHot = true,
                        badge = "AI",
                        onClick = onOpenScan,
                        modifier = Modifier.weight(1f).testTag("qa_scan_bill")
                    )
                    QuickActionTile(
                        label = strings.addExpense,
                        icon = Icons.Filled.Add,
                        onClick = onOpenAddExpense,
                        modifier = Modifier.weight(1f).testTag("qa_add_expense")
                    )
                    QuickActionTile(
                        label = strings.logService,
                        icon = Icons.Filled.Build,
                        onClick = onOpenLogService,
                        modifier = Modifier.weight(1f).testTag("qa_log_service")
                    )
                    QuickActionTile(
                        label = strings.newReminder,
                        icon = Icons.Filled.Notifications,
                        onClick = onOpenNewReminder,
                        modifier = Modifier.weight(1f).testTag("qa_new_reminder")
                    )
                }
            }

            // Needs attention
            item {
                SectionHeader(
                    title = strings.needsAttention,
                    actionLabel = strings.allBills,
                    onAction = onOpenBills
                )
                if (alerts.isEmpty()) {
                    SelavuCard {
                        Text(
                            text = strings.allUpToDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ok
                        )
                    }
                } else {
                    SelavuListContainer {
                        val topAlerts = alerts.take(4)
                        topAlerts.forEachIndexed { index, alert ->
                            if (index > 0) SelavuDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (alert.isBill) onOpenBills()
                                        else alert.targetVehicleId?.let(onOpenVehicle)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryTile(iconKey = alert.iconKey, colorIndex = alert.colorIndex)
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(text = alert.statusLabel, status = alert.severity)
                            }
                        }
                    }
                }
            }

            // Where it went
            if (categorySlices.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = strings.whereItWent,
                        actionLabel = strings.insights,
                        onAction = onOpenInsights
                    )
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CategoryStackedBar(slices = categorySlices)
                            val totalSliceSum = categorySlices.sumOf { it.amount }.coerceAtLeast(1.0)
                            val topFour = categorySlices.take(4)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (rowStart in topFour.indices step 2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        for (col in 0..1) {
                                            val slice = topFour.getOrNull(rowStart + col)
                                            if (slice != null) {
                                                val share = ((slice.amount / totalSliceSum) * 100.0).roundToInt()
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(9.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(colors.categoryColor(slice.colorIndex))
                                                    )
                                                    Text(
                                                        text = slice.name.substringBefore(" & "),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.ink2,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "$share%",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = colors.ink
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent expenses
            item {
                SectionHeader(
                    title = strings.recent,
                    actionLabel = strings.seeAll,
                    onAction = onOpenActivity
                )
                SelavuListContainer {
                    if (recentExpenses.isEmpty()) {
                        Text(
                            text = "No expenses logged yet. Tap Add or Scan bill to start fresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink2,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        recentExpenses.forEachIndexed { idx, expense ->
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

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHot: Boolean = false,
    badge: String? = null
) {
    val colors = SelavuTheme.colors
    val bg = if (isHot) colors.accentSoft else colors.surface
    val fg = if (isHot) colors.accentInk else colors.ink
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .then(
                    if (!isHot) Modifier.border(1.dp, colors.line, RoundedCornerShape(16.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = (-6).dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.accent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = colors.onAccent
                )
            }
        }
    }
}

@Composable
fun ExpenseRowItem(
    expense: Expense,
    category: Category?,
    onClick: () -> Unit
) {
    val colors = SelavuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryTile(
            iconKey = category?.iconKey ?: "tag",
            colorIndex = category?.colorIndex ?: 9
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${category?.name ?: "Other"} · ${IndianFormatters.formatShortDate(expense.dateEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = IndianFormatters.formatInr(expense.amount),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum"
            ),
            color = colors.ink
        )
    }
}
