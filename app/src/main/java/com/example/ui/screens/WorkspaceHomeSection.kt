package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ServiceStatus
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
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import kotlin.math.roundToInt

@Composable
fun ReviewQueueBanner(
    draftCount: Int,
    onOpenReview: () -> Unit
) {
    if (draftCount <= 0) return
    val colorScheme = MaterialTheme.colorScheme
    SelavuCard(
        onClick = onOpenReview,
        modifier = Modifier.testTag("home_review_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "Pending review drafts",
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$draftCount transaction alert${if (draftCount > 1) "s" else ""} ready to review",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Map category, income/expense type or items before saving",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            StatusPill(text = "Review", isAccent = true)
        }
    }
}

@Composable
fun HomeDiscoveryStrip(
    reviewDraftCount: Int,
    workspace: Workspace?,
    onOpenReview: () -> Unit,
    onOpenWorkspace: () -> Unit
) {
    val strings = selavuStrings()
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelavuFilterChip(
            label = if (reviewDraftCount > 0) "${strings.reviewAlerts} ($reviewDraftCount)" else strings.reviewAlerts,
            selected = reviewDraftCount > 0,
            icon = Icons.Filled.NotificationsActive,
            onClick = onOpenReview,
            modifier = Modifier
                .weight(1f)
                .testTag("home_chip_review_alerts")
        )
        SelavuFilterChip(
            label = workspace?.name ?: strings.familyWorkspace,
            selected = workspace != null,
            icon = Icons.Filled.Groups,
            onClick = onOpenWorkspace,
            modifier = Modifier
                .weight(1f)
                .testTag("home_chip_family_workspace")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceHomeContent(
    currentUid: String,
    workspace: Workspace,
    sharedExpenses: List<SharedExpense>,
    filter: WorkspaceExpenseFilter,
    onSelectFilter: (WorkspaceExpenseFilter) -> Unit,
    onOpenWorkspaceSettings: () -> Unit,
    onOpenAllActivity: () -> Unit,
    onOpenSharedDetail: (SharedExpense) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    val filteredShared = remember(sharedExpenses, filter, currentUid) {
        when (filter) {
            WorkspaceExpenseFilter.ALL -> sharedExpenses
            WorkspaceExpenseFilter.OTHERS_ONLY -> sharedExpenses.filter { it.ownerUid != currentUid }
            WorkspaceExpenseFilter.MINE_ONLY -> sharedExpenses.filter { it.ownerUid == currentUid }
        }
    }

    val monthShared = remember(filteredShared, nowMs) {
        filteredShared.filter { IndianFormatters.isSameMonth(it.dateEpochMs, nowMs) }
    }
    val monthSharedSpend = remember(monthShared) { monthShared.filter { it.entryType != "income" } }
    val monthSharedIncome = remember(monthShared) { monthShared.filter { it.entryType == "income" } }
    val monthTotal = remember(monthSharedSpend) { monthSharedSpend.sumOf { it.amount } }
    val monthIncomeTotal = remember(monthSharedIncome) { monthSharedIncome.sumOf { it.amount } }

    val slices = remember(monthSharedSpend) {
        monthSharedSpend.groupBy { it.categoryId }.entries.map { (catId, list) ->
            val first = list.first()
            CategoryShareSlice(
                categoryId = catId,
                name = first.categoryName,
                colorIndex = first.categoryColorIndex,
                iconKey = first.categoryIconKey,
                amount = list.sumOf { it.amount }
            )
        }.sortedByDescending { it.amount }
    }

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
            }
            .clickable(onClick = onOpenWorkspaceSettings)
            .padding(20.dp)
            .testTag("workspace_hero_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workspace.name,
                style = MaterialTheme.typography.labelLarge,
                color = heroFg.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(heroFg.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${workspace.memberIds.size} members · Manage",
                    style = MaterialTheme.typography.labelSmall,
                    color = heroFg
                )
            }
        }
        Text(
            text = IndianFormatters.formatInr(monthTotal),
            style = MaterialTheme.typography.displayLarge,
            color = heroFg,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        if (monthIncomeTotal > 0.0) {
            Text(
                text = "Shared income +${IndianFormatters.formatInr(monthIncomeTotal)} this month",
                style = SelavuTheme.textStyles.numericCaption,
                color = heroFg.copy(alpha = 0.94f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = when (filter) {
                WorkspaceExpenseFilter.ALL -> "${monthShared.size} shared entries this month"
                WorkspaceExpenseFilter.OTHERS_ONLY -> "${monthShared.size} entries by family members (you excluded)"
                WorkspaceExpenseFilter.MINE_ONLY -> "${monthShared.size} of your shared entries this month"
            },
            style = MaterialTheme.typography.bodySmall,
            color = heroFg.copy(alpha = 0.85f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelavuFilterChip(
            label = strings.filterAllShared,
            selected = filter == WorkspaceExpenseFilter.ALL,
            onClick = { onSelectFilter(WorkspaceExpenseFilter.ALL) },
            modifier = Modifier.testTag("ws_filter_all")
        )
        SelavuFilterChip(
            label = strings.filterOthersOnly,
            selected = filter == WorkspaceExpenseFilter.OTHERS_ONLY,
            onClick = { onSelectFilter(WorkspaceExpenseFilter.OTHERS_ONLY) },
            modifier = Modifier.testTag("ws_filter_others")
        )
        SelavuFilterChip(
            label = strings.filterMineShared,
            selected = filter == WorkspaceExpenseFilter.MINE_ONLY,
            onClick = { onSelectFilter(WorkspaceExpenseFilter.MINE_ONLY) },
            modifier = Modifier.testTag("ws_filter_mine")
        )
    }

    if (slices.isNotEmpty()) {
        SectionHeader(title = strings.whereItWent)
        SelavuCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryStackedBar(slices = slices)
                val totalSliceSum = slices.sumOf { it.amount }.coerceAtLeast(1.0)
                val topFour = slices.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topFour.forEach { slice ->
                        val share = ((slice.amount / totalSliceSum) * 100.0).roundToInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(extendedColors.categoryColor(slice.colorIndex))
                            )
                            Text(
                                text = slice.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$share% · ${IndianFormatters.formatInr(slice.amount)}",
                                style = SelavuTheme.textStyles.numericCaption,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    SectionHeader(
        title = "Shared activity",
        actionLabel = strings.seeAll,
        onAction = onOpenAllActivity
    )
    if (filteredShared.isEmpty()) {
        SelavuEmptyState(
            icon = Icons.Filled.Groups,
            title = when (filter) {
                WorkspaceExpenseFilter.OTHERS_ONLY -> "No shared expenses from others yet"
                WorkspaceExpenseFilter.MINE_ONLY -> "You haven't shared any expenses yet"
                WorkspaceExpenseFilter.ALL -> "No shared expenses yet"
            },
            subtitle = "Non-private expenses in visible categories appear here automatically for workspace members.",
            actionLabel = "Workspace settings",
            onAction = onOpenWorkspaceSettings
        )
    } else {
        SelavuListContainer {
            filteredShared.take(8).forEachIndexed { idx, item ->
                if (idx > 0) SelavuDivider()
                SharedExpenseRowItem(
                    expense = item,
                    isMine = item.ownerUid == currentUid,
                    onClick = { onOpenSharedDetail(item) }
                )
            }
        }
    }
}

@Composable
fun SharedExpenseRowItem(
    expense: SharedExpense,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val isIncome = expense.entryType == "income"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryTile(
            iconKey = expense.categoryIconKey,
            colorIndex = expense.categoryColorIndex
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                StatusPill(
                    text = if (isMine) "You" else expense.ownerName,
                    isAccent = !isMine,
                    status = if (isMine) ServiceStatus.OK else null
                )
                if (isIncome) {
                    StatusPill(text = "Income", status = ServiceStatus.OK)
                }
            }
            val subText = if (expense.subcategory.isNotBlank()) " › ${expense.subcategory}" else ""
            Text(
                text = "${expense.categoryName}$subText · ${IndianFormatters.formatShortDate(expense.dateEpochMs)} · ${expense.paymentMode}",
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
