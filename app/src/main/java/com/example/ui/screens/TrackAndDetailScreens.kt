package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceStatus
import com.example.data.model.Vehicle
import com.example.ui.components.CategoryTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuProgressBar
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import kotlin.math.roundToInt

@Composable
fun TrackScreen(
    vehicles: List<Vehicle>,
    projects: List<Project>,
    initialSegment: String = "veh",
    onOpenVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenProject: (String) -> Unit,
    onAddProject: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    val nowMs = remember { System.currentTimeMillis() }

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
                        text = strings.trackTitle,
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
                            .testTag("track_profile_button"),
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
                    options = listOf("veh" to strings.vehicles, "proj" to strings.projects),
                    selectedKey = segment,
                    onSelect = { segment = it }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (segment == "veh") {
                if (vehicles.isEmpty()) {
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "No vehicles tracked yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink
                                )
                                Text(
                                    text = "Add a bike or car to automatically track engine oil, chain lube, brake pads, insurance & service history.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                            }
                        }
                    }
                }
                items(vehicles, key = { it.id }) { vehicle ->
                    val states = vehicle.items.map { it to it.computeState(vehicle.odometer, nowMs) }
                    val overdueCount = states.count { it.second.status == ServiceStatus.OVERDUE }
                    val soonCount = states.count { it.second.status == ServiceStatus.DUE_SOON }
                    val topUrgent = states.sortedWith(
                        compareBy({ it.second.status.severity }, { it.second.daysLeft })
                    ).firstOrNull()

                    SelavuCard(
                        onClick = { onOpenVehicle(vehicle.id) },
                        modifier = Modifier.testTag("vehicle_card_${vehicle.id}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryTile(iconKey = vehicle.type, colorIndex = 1, size = 44.dp, iconSize = 22.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vehicle.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${vehicle.plate} · ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink2
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open",
                                    tint = colors.ink3
                                )
                            }

                            Row(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (overdueCount > 0) {
                                    StatusPill("$overdueCount overdue", status = ServiceStatus.OVERDUE)
                                }
                                if (soonCount > 0) {
                                    StatusPill("$soonCount due soon", status = ServiceStatus.DUE_SOON)
                                }
                                if (overdueCount == 0 && soonCount == 0) {
                                    StatusPill(strings.allUpToDate, status = ServiceStatus.OK)
                                }
                            }

                            if (topUrgent != null) {
                                Text(
                                    text = "${topUrgent.first.name} · ${topUrgent.second.statusLabel}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.ink2,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddVehicle,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.sunk,
                            contentColor = colors.ink
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_vehicle_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addVehicle, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                if (projects.isEmpty()) {
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "No projects yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink
                                )
                                Text(
                                    text = "Create a house construction, renovation, or business setup project to track phases, vendors & payments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                            }
                        }
                    }
                }
                items(projects, key = { it.id }) { project ->
                    val spent = project.phases.sumOf { it.spent }
                    val ratio = if (project.budget > 0) (spent / project.budget).toFloat() else 0f
                    val pct = (ratio * 100f).roundToInt()

                    SelavuCard(
                        onClick = { onOpenProject(project.id) },
                        modifier = Modifier.testTag("project_card_${project.id}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryTile(
                                    iconKey = project.iconKey,
                                    colorIndex = project.colorIndex,
                                    size = 44.dp,
                                    iconSize = 22.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = project.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open",
                                    tint = colors.ink3
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${IndianFormatters.formatLakhCrore(spent)} of ${IndianFormatters.formatLakhCrore(project.budget)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colors.ink
                                )
                                StatusPill("$pct%", isAccent = true)
                            }

                            Spacer(modifier = Modifier.height(9.dp))
                            SelavuProgressBar(progress = ratio, color = colors.accent)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddProject,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.sunk,
                            contentColor = colors.ink
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_project_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.newProject, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle,
    onBack: () -> Unit,
    onOpenOdometerSheet: () -> Unit,
    onOpenServiceItemSheet: (ServiceItem) -> Unit,
    onOpenAddServiceItem: () -> Unit,
    onOpenLogVisit: () -> Unit
) {
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    val sortedItems = remember(vehicle, nowMs) {
        vehicle.items.map { it to it.computeState(vehicle.odometer, nowMs) }
            .sortedWith(compareBy({ it.second.status.severity }, { it.second.daysLeft }))
    }
    val overdueCount = sortedItems.count { it.second.status == ServiceStatus.OVERDUE }
    val soonCount = sortedItems.count { it.second.status == ServiceStatus.DUE_SOON }
    val totalSpend = vehicle.history.sumOf { it.amount }

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
                SelavuTopNavBar(title = vehicle.shortName, onBack = onBack)
            }

            item {
                SelavuCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = strings.odometer,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.ink3
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = IndianFormatters.formatIndianInteger(vehicle.odometer.toLong()),
                                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                                        color = colors.ink
                                    )
                                    Text(
                                        text = " km",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.ink2,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenOdometerSheet,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.sunk,
                                    contentColor = colors.ink
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("update_odometer_button")
                            ) {
                                Text(strings.update, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SelavuDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCell(
                                value = if (vehicle.avgMileage > 0) "${vehicle.avgMileage.toInt()}" else "—",
                                label = "km/l average",
                                modifier = Modifier.weight(1f)
                            )
                            StatCell(
                                value = IndianFormatters.formatInr(totalSpend),
                                label = "service spend",
                                modifier = Modifier.weight(1f)
                            )
                            StatCell(
                                value = "${vehicle.items.size}",
                                label = "items tracked",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (overdueCount > 0 || soonCount > 0) {
                item {
                    val isBad = overdueCount > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isBad) colors.badSoft else colors.warnSoft)
                            .padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isBad) colors.bad else colors.warn,
                            modifier = Modifier.size(18.dp)
                        )
                        val msg = buildString {
                            if (overdueCount > 0) append("$overdueCount overdue")
                            if (overdueCount > 0 && soonCount > 0) append(" and ")
                            if (soonCount > 0) append("$soonCount due soon")
                            append(". Tap an item to mark it done or change its interval.")
                        }
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isBad) colors.bad else colors.warn
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = strings.serviceTracker,
                    actionLabel = strings.addItem,
                    onAction = onOpenAddServiceItem
                )
            }

            items(sortedItems, key = { it.first.id }) { (item, state) ->
                val barColor = when (state.status) {
                    ServiceStatus.OVERDUE -> colors.bad
                    ServiceStatus.DUE_SOON -> colors.warn
                    ServiceStatus.OK -> colors.ok
                }
                SelavuCard(
                    onClick = { onOpenServiceItemSheet(item) },
                    modifier = Modifier.testTag("service_item_${item.id}")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryTile(iconKey = item.iconKey, colorIndex = 1)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val lastKm = if (item.intervalKm > 0) {
                                    " · ${IndianFormatters.formatIndianInteger(item.lastDoneKm.toLong())} km"
                                } else ""
                                Text(
                                    text = "Last ${IndianFormatters.formatShortDate(item.lastDoneEpochMs)}$lastKm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                            }
                            StatusPill(text = state.statusLabel, status = state.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        SelavuProgressBar(progress = state.progress, color = barColor, height = 6.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.nextDueLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink2
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = strings.serviceHistory,
                    actionLabel = strings.logVisit,
                    onAction = onOpenLogVisit
                )
            }

            if (vehicle.history.isEmpty()) {
                item {
                    SelavuCard {
                        Text(
                            text = "No service visits logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink2
                        )
                    }
                }
            } else {
                items(vehicle.history) { visit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(3.dp, colors.accent, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${IndianFormatters.formatDate(visit.dateEpochMs)} · ${IndianFormatters.formatIndianInteger(visit.odometer.toLong())} km",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colors.ink
                                )
                                Text(
                                    text = IndianFormatters.formatInr(visit.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = colors.ink
                                )
                            }
                            Text(
                                text = visit.workshop,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.ink2
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                visit.itemsDone.forEach { doneName ->
                                    StatusPill(text = doneName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(
    project: Project,
    onBack: () -> Unit,
    onOpenAddPayment: () -> Unit
) {
    val colors = SelavuTheme.colors
    var subTab by rememberSaveable { mutableStateOf("budget") }
    val spent = project.phases.sumOf { it.spent }
    val ratio = if (project.budget > 0) (spent / project.budget).toFloat() else 0f
    val pct = (ratio * 100f).roundToInt()
    val owedToVendors = project.vendors.sumOf { (it.quoted - it.paid).coerceAtLeast(0.0) }
    val hotPhase = project.phases.firstOrNull { it.budget > 0 && it.spent / it.budget >= 0.9 && it.spent > 0 }

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
                SelavuTopNavBar(title = project.name, onBack = onBack)
            }

            item {
                SelavuCard {
                    Column {
                        Text(
                            text = "SPENT SO FAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.ink3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = IndianFormatters.formatLakhCrore(spent),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                                color = colors.ink
                            )
                            Text(
                                text = "of ${IndianFormatters.formatLakhCrore(project.budget)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.ink2,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SelavuProgressBar(progress = ratio, color = colors.accent, height = 10.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        SelavuDivider()
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCell("$pct%", "of budget", Modifier.weight(1f))
                            StatCell(
                                IndianFormatters.formatLakhCrore((project.budget - spent).coerceAtLeast(0.0)),
                                "remaining",
                                Modifier.weight(1f)
                            )
                            StatCell(
                                IndianFormatters.formatLakhCrore(owedToVendors),
                                "owed to vendors",
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (hotPhase != null) {
                item {
                    val phasePct = ((hotPhase.spent / hotPhase.budget) * 100.0).roundToInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.warnSoft)
                            .padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.warn, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${hotPhase.name} has used $phasePct% of its budget. Check pending bills before approving more work.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.warn
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
                SelavuSegmentedControl(
                    options = listOf("budget" to "Budget", "vend" to "Vendors", "pay" to "Payments"),
                    selectedKey = subTab,
                    onSelect = { subTab = it }
                )
            }

            when (subTab) {
                "budget" -> {
                    items(project.phases) { phase ->
                        val pRatio = if (phase.budget > 0) (phase.spent / phase.budget).toFloat() else 0f
                        val barColor = when {
                            pRatio >= 1f -> colors.bad
                            pRatio >= 0.9f -> colors.warn
                            else -> colors.ok
                        }
                        SelavuCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = phase.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.ink
                                    )
                                    Text(
                                        text = IndianFormatters.formatLakhCrore(phase.spent),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = colors.ink
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                SelavuProgressBar(progress = pRatio, color = barColor)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Budget ${IndianFormatters.formatLakhCrore(phase.budget)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink2
                                    )
                                    Text(
                                        text = if (phase.spent == 0.0) "Not started" else "${(pRatio * 100).roundToInt()}% used",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.ink2
                                    )
                                }
                            }
                        }
                    }
                }
                "vend" -> {
                    if (project.vendors.isEmpty()) {
                        item {
                            SelavuCard {
                                Text("No vendors added yet.", style = MaterialTheme.typography.bodyMedium, color = colors.ink2)
                            }
                        }
                    } else {
                        items(project.vendors) { vendor ->
                            val vRatio = if (vendor.quoted > 0) (vendor.paid / vendor.quoted).toFloat() else 0f
                            SelavuCard {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = vendor.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.ink
                                        )
                                        Text(
                                            text = "${IndianFormatters.formatLakhCrore((vendor.quoted - vendor.paid).coerceAtLeast(0.0))} due",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = colors.ink
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SelavuProgressBar(progress = vRatio, color = colors.accent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(vendor.role, style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                                        Text(
                                            text = "Paid ${IndianFormatters.formatLakhCrore(vendor.paid)} of ${IndianFormatters.formatLakhCrore(vendor.quoted)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.ink2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        SelavuListContainer {
                            if (project.payments.isEmpty()) {
                                Text(
                                    text = "No payments recorded yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink2,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                project.payments.forEachIndexed { idx, payment ->
                                    if (idx > 0) SelavuDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = payment.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colors.ink
                                            )
                                            Text(
                                                text = "${IndianFormatters.formatShortDate(payment.dateEpochMs)} · ${payment.vendor}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.ink2
                                            )
                                        }
                                        Text(
                                            text = IndianFormatters.formatInr(payment.amount),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
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
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onOpenAddPayment,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_project_payment_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add payment", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = SelavuTheme.colors
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
            color = colors.ink,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink2,
            maxLines = 1
        )
    }
}
