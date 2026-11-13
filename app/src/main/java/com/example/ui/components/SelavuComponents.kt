package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceStatus
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings

fun iconForKey(key: String): ImageVector = when (key) {
    "car" -> Icons.Filled.DirectionsCar
    "bike" -> Icons.Filled.DirectionsBike
    "house" -> Icons.Filled.HomeWork
    "bag" -> Icons.Filled.ShoppingBag
    "bolt" -> Icons.Filled.Bolt
    "tv" -> Icons.Filled.Tv
    "heart" -> Icons.Filled.Favorite
    "building" -> Icons.Filled.Apartment
    "fork" -> Icons.Filled.Restaurant
    "rupee" -> Icons.Filled.CurrencyRupee
    "drop" -> Icons.Filled.WaterDrop
    "wrench" -> Icons.Filled.Build
    "link" -> Icons.Filled.Link
    "gear" -> Icons.Filled.Settings
    "shield" -> Icons.Filled.Shield
    "wifi" -> Icons.Filled.Wifi
    "fuel" -> Icons.Filled.LocalGasStation
    "file" -> Icons.Filled.ReceiptLong
    "bell" -> Icons.Filled.Notifications
    "scan" -> Icons.Filled.DocumentScanner
    "sparkle" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.AccountBalanceWallet
}

@Composable
fun CategoryTile(
    iconKey: String,
    colorIndex: Int,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val colors = SelavuTheme.colors
    val tint = colors.categoryColor(colorIndex)
    val bg = tint.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconForKey(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    status: ServiceStatus? = null,
    isAccent: Boolean = false
) {
    val colors = SelavuTheme.colors
    val (bg, fg) = when {
        status == ServiceStatus.OVERDUE -> colors.badSoft to colors.bad
        status == ServiceStatus.DUE_SOON -> colors.warnSoft to colors.warn
        status == ServiceStatus.OK -> colors.okSoft to colors.ok
        isAccent -> colors.accentSoft to colors.accentInk
        else -> colors.sunk to colors.ink2
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SelavuProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color = SelavuTheme.colors.sunk,
    height: Dp = 7.dp,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(350),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceAtLeast(0.02f))
                .height(height)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
    }
}

@Composable
fun SelavuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = SelavuTheme.colors
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(colors.surface)
        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(14.dp)
    Box(modifier = base) {
        content()
    }
}

@Composable
fun SelavuListContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = SelavuTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
fun SelavuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SelavuTheme.colors.line)
    )
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = SelavuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.ink
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.accentInk,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SelavuTopNavBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = SelavuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                .clickable(onClick = onBack)
                .testTag("nav_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.ink,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun SelavuSegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.sunk)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (key, label) ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(1.dp, RoundedCornerShape(10.dp))
                                .background(colors.surface)
                        } else Modifier
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 9.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = if (selected) colors.ink else colors.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SelavuFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    val bg = if (selected) colors.accentSoft else colors.surface
    val border = if (selected) colors.accent else colors.line
    val fg = if (selected) colors.accentInk else colors.ink
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun SelavuBottomBar(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    Surface(
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            SelavuDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BottomTabItem(
                    label = strings.tabHome,
                    selected = currentTab == "home",
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onSelectTab("home") },
                    modifier = Modifier.weight(1f).testTag("tab_home")
                )
                BottomTabItem(
                    label = strings.tabTrack,
                    selected = currentTab == "track",
                    selectedIcon = Icons.Outlined.GridView,
                    unselectedIcon = Icons.Outlined.GridView,
                    onClick = { onSelectTab("track") },
                    modifier = Modifier.weight(1f).testTag("tab_track")
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-12).dp)
                            .size(56.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .border(4.dp, colors.surface, CircleShape)
                            .clickable(onClick = onFabClick)
                            .testTag("fab_add"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = colors.onAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                BottomTabItem(
                    label = strings.tabBills,
                    selected = currentTab == "bills",
                    selectedIcon = Icons.Filled.Notifications,
                    unselectedIcon = Icons.Outlined.Notifications,
                    onClick = { onSelectTab("bills") },
                    modifier = Modifier.weight(1f).testTag("tab_bills")
                )
                BottomTabItem(
                    label = strings.tabInsights,
                    selected = currentTab == "insights",
                    selectedIcon = Icons.Filled.PieChart,
                    unselectedIcon = Icons.Outlined.PieChart,
                    onClick = { onSelectTab("insights") },
                    modifier = Modifier.weight(1f).testTag("tab_insights")
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    val tint = if (selected) colors.accentInk else colors.ink3
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class CategoryShareSlice(
    val categoryId: String,
    val name: String,
    val colorIndex: Int,
    val iconKey: String,
    val amount: Double
)

@Composable
fun CategoryStackedBar(slices: List<CategoryShareSlice>) {
    val colors = SelavuTheme.colors
    val total = slices.sumOf { it.amount }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(99.dp))
            .semantics { contentDescription = "Spending split by category" },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        slices.forEach { slice ->
            val weight = (slice.amount / total).toFloat().coerceAtLeast(0.02f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(12.dp)
                    .background(colors.categoryColor(slice.colorIndex))
            )
        }
    }
}

@Composable
fun DonutSpendChart(
    slices: List<CategoryShareSlice>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    val sliceColors = slices.map { colors.categoryColor(it.colorIndex) }
    val safeTotal = totalAmount.coerceAtLeast(1.0)
    Box(
        modifier = modifier
            .size(200.dp)
            .semantics { contentDescription = "Donut chart of spending by category" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokePx = 24.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            var startAngle = -90f
            val gapDeg = if (slices.size > 1) 2.5f else 0f
            slices.forEachIndexed { idx, slice ->
                val sweep = ((slice.amount / safeTotal) * 360.0).toFloat()
                val visSweep = (sweep - gapDeg).coerceAtLeast(1f)
                drawArc(
                    color = sliceColors[idx],
                    startAngle = startAngle,
                    sweepAngle = visSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = IndianFormatters.formatLakhCrore(totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink
            )
            Text(
                text = "total spend",
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink2
            )
        }
    }
}

@Composable
fun MonthlyTrendBarChart(
    months: List<Pair<String, Double>>,
    budget: Double,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    val maxVal = maxOf(budget * 1.12, months.maxOfOrNull { it.second } ?: 70000.0, 10000.0)
    val gridColor = colors.line
    val accentColor = colors.accent
    val mutedBar = colors.bar
    val warnColor = colors.warn
    val labelArgb = colors.ink2.toArgb()
    val boldArgb = colors.ink.toArgb()
    val warnArgb = colors.warn.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = "Monthly spending bar chart for last six months" }
    ) {
        val leftPad = 36.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 20.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 10.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }

        val steps = 3
        for (i in 0..steps) {
            val value = (maxVal / steps) * i
            val y = topPad + chartH - ((value / maxVal).toFloat() * chartH)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val tickText = if (value < 1000) "0" else "${(value / 1000).toInt()}k"
            drawContext.canvas.nativeCanvas.drawText(
                tickText,
                leftPad - 6.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        val stepW = chartW / months.size.coerceAtLeast(1)
        val barW = (stepW * 0.56f).coerceAtMost(32.dp.toPx())

        val monthPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val boldPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = boldArgb
            textSize = 11.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        months.forEachIndexed { index, (label, amount) ->
            val isLast = index == months.lastIndex
            val barH = ((amount / maxVal).toFloat() * chartH).coerceAtLeast(2.dp.toPx())
            val x = leftPad + index * stepW + (stepW - barW) / 2f
            val y = topPad + chartH - barH

            drawRoundRect(
                color = if (isLast) accentColor else mutedBar,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barW / 2f,
                size.height - 6.dp.toPx(),
                if (isLast) boldPaint else monthPaint
            )
            if (isLast) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${(amount / 1000).toInt()}k",
                    x + barW / 2f,
                    y - 6.dp.toPx(),
                    boldPaint
                )
            }
        }

        // Dashed budget line
        val budgetY = topPad + chartH - ((budget / maxVal).toFloat() * chartH)
        drawLine(
            color = warnColor,
            start = Offset(leftPad, budgetY),
            end = Offset(size.width - rightPad, budgetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
        val budgetPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = warnArgb
            textSize = 10.5.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            "Budget ${(budget / 1000).toInt()}k",
            size.width - rightPad,
            budgetY - 5.dp.toPx(),
            budgetPaint
        )
    }
}
