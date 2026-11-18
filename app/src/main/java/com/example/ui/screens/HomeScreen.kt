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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttentionAlert
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseScope
import com.example.data.model.SharedExpense
import com.example.data.model.Workspace
import com.example.data.model.WorkspaceExpenseFilter
import com.example.ui.components.CategoryShareSlice
import com.example.ui.components.CategoryStackedBar
import com.example.ui.components.CategoryTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuEmptyState
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuMainHeader
import com.example.ui.components.SelavuProgressBar
import com.example.ui.components.SelavuSegmentedControl
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
    userDisplayName: String? = null,
    userPhotoUrl: String? = null,
    currentUid: String = "",
    reviewDraftCount: Int = 0,
    workspace: Workspace? = null,
    sharedExpenses: List<SharedExpense> = emptyList(),
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
    onEditBudget: () -> Unit,
    onOpenReview: () -> Unit = {},
    onOpenWorkspace: () -> Unit = {},
    onOpenSharedExpenseDetail: (SharedExpense) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    var scopeName by rememberSaveable { mutableStateOf(ExpenseScope.INDIVIDUAL.name) }
    var wsFilterName by rememberSaveable { mutableStateOf(WorkspaceExpenseFilter.ALL.name) }

    LaunchedEffect(workspace) {
        if (workspace == null) {
            scopeName = ExpenseScope.INDIVIDUAL.name
        }
    }
    val scope = if (workspace != null && scopeName == ExpenseScope.WORKSPACE.name) {
        ExpenseScope.WORKSPACE
    } else {
        ExpenseScope.INDIVIDUAL
    }
    val wsFilter = WorkspaceExpenseFilter.entries.firstOrNull { it.name == wsFilterName }
        ?: WorkspaceExpenseFilter.ALL

    val monthExpenses = remember(expenses, nowMs) {
        expenses.filter { IndianFormatters.isSameMonth(it.dateEpochMs, nowMs) }
    }
    val monthSpend = remember(monthExpenses) { monthExpenses.filter { it.entryType != "income" } }
    val monthIncome = remember(monthExpenses) { monthExpenses.filter { it.entryType == "income" } }
    val monthTotal = remember(monthSpend) { monthSpend.sumOf { it.amount } }
    val monthIncomeTotal = remember(monthIncome) { monthIncome.sumOf { it.amount } }
    val monthNet = remember(monthIncomeTotal, monthTotal) { monthIncomeTotal - monthTotal }
    val percentUsed = remember(monthTotal, monthlyBudget) {
        if (monthlyBudget > 0) ((monthTotal / monthlyBudget) * 100.0).roundToInt().coerceAtMost(999) else 0
    }
    val daysLeft = remember(nowMs) { IndianFormatters.daysLeftInMonth(nowMs) }

    val categorySlices = remember(monthSpend, categories) {
        val grouped = monthSpend.groupBy { it.categoryId }
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
                SelavuMainHeader(
                    title = strings.goodEvening,
                    subtitle = IndianFormatters.fullMonthHeader(nowMs),
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl,
                    onOpenProfile = onOpenCategories,
                    profileTestTag = "home_settings_button"
                )
            }

            if (reviewDraftCount > 0) {
                item {
                    ReviewQueueBanner(
                        draftCount = reviewDraftCount,
                        onOpenReview = onOpenReview
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (workspace != null) {
                item {
                    SelavuSegmentedControl(
                        options = listOf(
                            ExpenseScope.INDIVIDUAL.name to strings.scopeIndividual,
                            ExpenseScope.WORKSPACE.name to workspace.name
                        ),
                        selectedKey = scope.name,
                        onSelect = { scopeName = it },
                        modifier = Modifier.testTag("home_scope_selector")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (scope == ExpenseScope.WORKSPACE && workspace != null) {
                item {
                    WorkspaceHomeContent(
                        currentUid = currentUid,
                        workspace = workspace,
                        sharedExpenses = sharedExpenses,
                        filter = wsFilter,
                        onSelectFilter = { wsFilterName = it.name },
                        onOpenWorkspaceSettings = onOpenWorkspace,
                        onOpenAllActivity = onOpenActivity,
                        onOpenSharedDetail = onOpenSharedExpenseDetail
                    )
                }
            } else {
            // Monthly Spend Hero Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val heroFg = colorScheme.onSecondary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colorScheme.secondary, colorScheme.primary)
                            )
                        )
                        .drawBehind {
                            drawCircle(
                                color = heroFg.copy(alpha = 0.08f),
                                radius = size.height * 0.62f,
                                center = Offset(size.width * 0.92f, size.height * 0.16f)
                            )
                            drawCircle(
                                color = heroFg.copy(alpha = 0.05f),
                                radius = size.height * 0.36f,
                                center = Offset(size.width * 0.12f, size.height * 0.96f)
                            )
                        }
                        .clickable(onClick = onEditBudget)
                        .padding(20.dp)
                        .testTag("home_hero_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.monthSoFar,
                            style = MaterialTheme.typography.labelLarge,
                            color = heroFg.copy(alpha = 0.85f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(heroFg.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$daysLeft ${strings.daysLeft}",
                                style = MaterialTheme.typography.labelSmall,
                                color = heroFg
                            )
                        }
                    }

                    Text(
                        text = IndianFormatters.formatInr(monthTotal),
                        style = MaterialTheme.typography.displayLarge,
                        color = heroFg,
                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(heroFg.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("home_hero_income_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "INCOME THIS MONTH",
                                style = SelavuTheme.textStyles.badgeMicro,
                                color = heroFg.copy(alpha = 0.78f)
                            )
                            Text(
                                text = "+${IndianFormatters.formatInr(monthIncomeTotal)}",
                                style = SelavuTheme.textStyles.numericTitle,
                                color = heroFg
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NET CASHFLOW",
                                style = SelavuTheme.textStyles.badgeMicro,
                                color = heroFg.copy(alpha = 0.78f)
                            )
                            val netPrefix = if (monthNet > 0.0) "+" else if (monthNet < 0.0) "-" else ""
                            Text(
                                text = "$netPrefix${IndianFormatters.formatInr(kotlin.math.abs(monthNet))}",
                                style = SelavuTheme.textStyles.numericTitle,
                                color = heroFg
                            )
                        }
                    }

                    SelavuProgressBar(
                        progress = if (monthlyBudget > 0) (monthTotal / monthlyBudget).toFloat() else 0f,
                        color = heroFg,
                        trackColor = heroFg.copy(alpha = 0.24f),
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
                            color = heroFg.copy(alpha = 0.92f)
                        )
                        Text(
                            text = "$percentUsed${strings.usedPercent}",
                            style = MaterialTheme.typography.labelMedium,
                            color = heroFg
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
                HomeDiscoveryStrip(
                    reviewDraftCount = reviewDraftCount,
                    workspace = workspace,
                    onOpenReview = onOpenReview,
                    onOpenWorkspace = onOpenWorkspace
                )
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
                            color = extendedColors.ok
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
                                    color = colorScheme.onSurface,
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
                                                            .background(extendedColors.categoryColor(slice.colorIndex))
                                                    )
                                                    Text(
                                                        text = slice.name.substringBefore(" & "),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "$share%",
                                                        style = SelavuTheme.textStyles.numericCaption,
                                                        color = colorScheme.onSurface
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
                if (recentExpenses.isEmpty()) {
                    SelavuEmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "No expenses logged yet",
                        subtitle = "Tap Add expense or Scan bill with AI to record your first transaction.",
                        actionLabel = strings.addExpense,
                        onAction = onOpenAddExpense
                    )
                } else {
                    SelavuListContainer {
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
    val colorScheme = MaterialTheme.colorScheme
    val bg = if (isHot) colorScheme.primaryContainer else colorScheme.surface
    val fg = if (isHot) colorScheme.onPrimaryContainer else colorScheme.onSurface
    val borderColor = if (isHot) colorScheme.primary.copy(alpha = 0.35f) else colorScheme.outlineVariant
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(bg)
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
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
                style = MaterialTheme.typography.labelSmall,
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
                    .background(colorScheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = SelavuTheme.textStyles.badgeMicro,
                    color = colorScheme.onPrimary
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
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val isIncome = expense.entryType == "income"
    val fallbackCatName = if (isIncome) "Income" else "Other"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryTile(
            iconKey = category?.iconKey ?: if (isIncome) "rupee" else "tag",
            colorIndex = category?.colorIndex ?: if (isIncome) 4 else 9
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val prefix = if (isIncome) "Income · " else ""
            Text(
                text = "$prefix${category?.name ?: fallbackCatName} · ${IndianFormatters.formatShortDate(expense.dateEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (isIncome) "+${IndianFormatters.formatInr(expense.amount)}" else IndianFormatters.formatInr(expense.amount),
            style = SelavuTheme.textStyles.numericBody,
            color = if (isIncome) extendedColors.ok else colorScheme.onSurface
        )
    }
}
