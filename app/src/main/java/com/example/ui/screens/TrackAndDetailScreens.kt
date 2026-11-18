package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ProjectProduct
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceStatus
import com.example.data.model.SharedExpense
import com.example.data.model.SharedProject
import com.example.data.model.SharedProjectContribution
import com.example.data.model.SharedVehicle
import com.example.data.model.Vehicle
import com.example.ui.components.CategoryTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuEmptyState
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuListContainer
import com.example.ui.components.SelavuMainHeader
import com.example.ui.components.SelavuProgressBar
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTextField
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackScreen(
    vehicles: List<Vehicle>,
    projects: List<Project>,
    currentUid: String = "",
    inWorkspace: Boolean = false,
    sharedVehicles: List<SharedVehicle> = emptyList(),
    sharedProjects: List<SharedProject> = emptyList(),
    sharedProjectContributions: List<SharedProjectContribution> = emptyList(),
    retainedVehicles: List<SharedVehicle> = emptyList(),
    retainedProjects: List<SharedProject> = emptyList(),
    initialSegment: String = "veh",
    userDisplayName: String? = null,
    userPhotoUrl: String? = null,
    onOpenVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenProject: (String) -> Unit,
    onAddProject: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = selavuStrings()
    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    val nowMs = remember { System.currentTimeMillis() }

    val activeForeignVehicles = remember(sharedVehicles, currentUid) {
        sharedVehicles.filter { it.ownerUid != currentUid }
    }
    val activeForeignProjects = remember(sharedProjects, currentUid) {
        sharedProjects.filter { it.ownerUid != currentUid }
    }
    val contribSumByProjectId = remember(sharedProjectContributions) {
        sharedProjectContributions.groupBy { it.sharedProjectId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
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
                    title = strings.trackTitle,
                    subtitle = "Maintenance memory & project budgets",
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl,
                    onOpenProfile = onOpenProfile,
                    profileTestTag = "track_profile_button"
                )
                SelavuSegmentedControl(
                    options = listOf("veh" to strings.vehicles, "proj" to strings.projects),
                    selectedKey = segment,
                    onSelect = { segment = it }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (segment == "veh") {
                val hasAnyVehicles = vehicles.isNotEmpty() || activeForeignVehicles.isNotEmpty() || retainedVehicles.isNotEmpty()
                if (!hasAnyVehicles) {
                    item {
                        SelavuEmptyState(
                            icon = Icons.Filled.DirectionsBike,
                            title = "No vehicles tracked yet",
                            subtitle = "Add a bike or car to automatically track engine oil, chain lube, brake pads, insurance & service history.",
                            actionLabel = strings.addVehicle,
                            onAction = onAddVehicle
                        )
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
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${vehicle.plate} · ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open",
                                    tint = colorScheme.outline
                                )
                            }

                            FlowRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                if (inWorkspace && vehicle.hiddenFromWorkspace) {
                                    StatusPill("Hidden", status = ServiceStatus.DUE_SOON)
                                }
                            }

                            if (topUrgent != null) {
                                Text(
                                    text = "${topUrgent.first.name} · ${topUrgent.second.statusLabel}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }

                if (activeForeignVehicles.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Shared in Family Workspace")
                    }
                    items(activeForeignVehicles, key = { "shared_veh_${it.id}" }) { sv ->
                        val states = sv.items.map { it to it.computeState(sv.odometer, nowMs) }
                        val overdueCount = states.count { it.second.status == ServiceStatus.OVERDUE }
                        val soonCount = states.count { it.second.status == ServiceStatus.DUE_SOON }
                        val topUrgent = states.sortedWith(
                            compareBy({ it.second.status.severity }, { it.second.daysLeft })
                        ).firstOrNull()

                        SelavuCard(
                            onClick = { onOpenVehicle(sv.id) },
                            modifier = Modifier.testTag("shared_vehicle_card_${sv.id}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CategoryTile(iconKey = sv.type, colorIndex = 1, size = 44.dp, iconSize = 22.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sv.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sv.plate} · ${IndianFormatters.formatIndianInteger(sv.odometer.toLong())} km",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = colorScheme.outline
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier.padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill("Shared · ${sv.ownerName}", isAccent = true)
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
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (retainedVehicles.isNotEmpty()) {
                    item {
                        SectionHeader(title = "From past workspaces (Read-only)")
                    }
                    items(retainedVehicles, key = { "ret_veh_${it.workspaceId}_${it.id}" }) { sv ->
                        SelavuCard(
                            onClick = { onOpenVehicle(sv.id) },
                            modifier = Modifier.testTag("retained_vehicle_card_${sv.id}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CategoryTile(iconKey = sv.type, colorIndex = 1, size = 44.dp, iconSize = 22.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sv.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sv.plate} · Shared by ${sv.ownerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = colorScheme.outline
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill("Past workspace · Read-only", status = ServiceStatus.DUE_SOON)
                                    StatusPill("Your expenses only")
                                }
                            }
                        }
                    }
                }

                if (hasAnyVehicles) {
                    item {
                        Button(
                            onClick = onAddVehicle,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.surfaceVariant,
                                contentColor = colorScheme.onSurface
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
                }
            } else {
                val hasAnyProjects = projects.isNotEmpty() || activeForeignProjects.isNotEmpty() || retainedProjects.isNotEmpty()
                if (!hasAnyProjects) {
                    item {
                        SelavuEmptyState(
                            icon = Icons.Filled.HomeWork,
                            title = "No projects yet",
                            subtitle = "Create a house construction, renovation, or business setup project to track phases, vendors & payments.",
                            actionLabel = strings.newProject,
                            onAction = onAddProject
                        )
                    }
                }
                items(projects, key = { it.id }) { project ->
                    val sharedKey = if (currentUid.isNotBlank()) "${currentUid}_${project.id}" else project.id
                    val extraContribs = contribSumByProjectId[sharedKey] ?: 0.0
                    val spent = project.phases.sumOf { it.spent } + extraContribs
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
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = project.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open",
                                    tint = colorScheme.outline
                                )
                            }

                            if (inWorkspace && project.hiddenFromWorkspace) {
                                Row(modifier = Modifier.padding(top = 10.dp)) {
                                    StatusPill("Hidden from workspace", status = ServiceStatus.DUE_SOON)
                                }
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
                                    style = SelavuTheme.textStyles.numericTitle,
                                    color = colorScheme.onSurface
                                )
                                StatusPill("$pct%", isAccent = true)
                            }

                            Spacer(modifier = Modifier.height(9.dp))
                            SelavuProgressBar(progress = ratio, color = colorScheme.primary)
                        }
                    }
                }

                if (activeForeignProjects.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Shared in Family Workspace")
                    }
                    items(activeForeignProjects, key = { "shared_proj_${it.id}" }) { sp ->
                        val extraContribs = contribSumByProjectId[sp.id] ?: 0.0
                        val spent = sp.phases.sumOf { it.spent } + extraContribs
                        val ratio = if (sp.budget > 0) (spent / sp.budget).toFloat() else 0f
                        val pct = (ratio * 100f).roundToInt()

                        SelavuCard(
                            onClick = { onOpenProject(sp.id) },
                            modifier = Modifier.testTag("shared_project_card_${sp.id}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CategoryTile(
                                        iconKey = sp.iconKey,
                                        colorIndex = sp.colorIndex,
                                        size = 44.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sp.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = sp.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = colorScheme.outline
                                    )
                                }

                                Row(modifier = Modifier.padding(top = 10.dp)) {
                                    StatusPill("Shared · ${sp.ownerName}", isAccent = true)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${IndianFormatters.formatLakhCrore(spent)} of ${IndianFormatters.formatLakhCrore(sp.budget)}",
                                        style = SelavuTheme.textStyles.numericTitle,
                                        color = colorScheme.onSurface
                                    )
                                    StatusPill("$pct%", isAccent = true)
                                }

                                Spacer(modifier = Modifier.height(9.dp))
                                SelavuProgressBar(progress = ratio, color = colorScheme.primary)
                            }
                        }
                    }
                }

                if (retainedProjects.isNotEmpty()) {
                    item {
                        SectionHeader(title = "From past workspaces (Read-only)")
                    }
                    items(retainedProjects, key = { "ret_proj_${it.workspaceId}_${it.id}" }) { sp ->
                        SelavuCard(
                            onClick = { onOpenProject(sp.id) },
                            modifier = Modifier.testTag("retained_project_card_${sp.id}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CategoryTile(
                                        iconKey = sp.iconKey,
                                        colorIndex = sp.colorIndex,
                                        size = 44.dp,
                                        iconSize = 22.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sp.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Shared by ${sp.ownerName} · Your contributions only",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = colorScheme.outline
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill("Past workspace · Read-only", status = ServiceStatus.DUE_SOON)
                                    StatusPill("Delete only")
                                }
                            }
                        }
                    }
                }

                if (hasAnyProjects) {
                    item {
                        Button(
                            onClick = onAddProject,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.surfaceVariant,
                                contentColor = colorScheme.onSurface
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
}

private data class VehicleExpenseRow(
    val id: String,
    val title: String,
    val subcategory: String,
    val dateEpochMs: Long,
    val paymentMode: String,
    val amount: Double,
    val odometerReading: Int?,
    val fuelPricePerLitre: Double?,
    val fuelLitres: Double?,
    val ownerBadge: String?,
    val personalExpense: Expense?,
    val sharedExpense: SharedExpense?
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle,
    expenses: List<Expense> = emptyList(),
    currentUid: String = "",
    inWorkspace: Boolean = false,
    isOwner: Boolean = true,
    ownerName: String? = null,
    isRetainedReadOnly: Boolean = false,
    sharedVehicleExpenses: List<SharedExpense> = emptyList(),
    onBack: () -> Unit,
    onOpenOdometerSheet: () -> Unit,
    onOpenServiceItemSheet: (ServiceItem) -> Unit,
    onOpenAddServiceItem: () -> Unit,
    onOpenAddExpense: (sub: String?) -> Unit,
    onOpenExpenseDetail: (Expense) -> Unit = {},
    onOpenSharedExpenseDetail: (SharedExpense) -> Unit = {},
    onToggleHiddenFromWorkspace: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    var subTab by rememberSaveable(vehicle.id, isRetainedReadOnly) {
        mutableStateOf(if (isRetainedReadOnly) "expenses" else "service")
    }
    LaunchedEffect(isRetainedReadOnly) {
        if (isRetainedReadOnly && subTab == "service") {
            subTab = "expenses"
        }
    }

    val sortedItems = remember(vehicle, nowMs, isRetainedReadOnly) {
        if (isRetainedReadOnly) emptyList()
        else vehicle.items.map { it to it.computeState(vehicle.odometer, nowMs) }
            .sortedWith(compareBy({ it.second.status.severity }, { it.second.daysLeft }))
    }
    val overdueCount = sortedItems.count { it.second.status == ServiceStatus.OVERDUE }
    val soonCount = sortedItems.count { it.second.status == ServiceStatus.DUE_SOON }

    val vehicleExpenses = remember(expenses, sharedVehicleExpenses, vehicle.id, currentUid, isOwner, isRetainedReadOnly) {
        val ownMatching = expenses.filter {
            it.vehicleId == vehicle.id || (isOwner && currentUid.isNotBlank() && it.vehicleId == "${currentUid}_${vehicle.id}")
        }
        val ownIds = ownMatching.map { it.id }.toSet()
        val rows = mutableListOf<VehicleExpenseRow>()
        ownMatching.forEach { e ->
            rows += VehicleExpenseRow(
                id = "p_${e.id}",
                title = e.title,
                subcategory = e.subcategory,
                dateEpochMs = e.dateEpochMs,
                paymentMode = e.paymentMode,
                amount = e.amount,
                odometerReading = e.odometerReading,
                fuelPricePerLitre = e.fuelPricePerLitre,
                fuelLitres = e.fuelLitres,
                ownerBadge = if (isOwner) null else "You",
                personalExpense = e,
                sharedExpense = null
            )
        }
        val allowedShared = if (isRetainedReadOnly) {
            sharedVehicleExpenses.filter { it.ownerUid == currentUid && it.sourceExpenseId !in ownIds }
        } else {
            sharedVehicleExpenses.filter {
                if (it.ownerUid == currentUid) it.sourceExpenseId !in ownIds else true
            }
        }
        allowedShared.forEach { se ->
            rows += VehicleExpenseRow(
                id = "s_${se.id}",
                title = se.title,
                subcategory = se.subcategory,
                dateEpochMs = se.dateEpochMs,
                paymentMode = se.paymentMode,
                amount = se.amount,
                odometerReading = se.odometerReading,
                fuelPricePerLitre = se.fuelPricePerLitre,
                fuelLitres = se.fuelLitres,
                ownerBadge = if (se.ownerUid == currentUid) "You" else se.ownerName,
                personalExpense = null,
                sharedExpense = se
            )
        }
        rows.sortedByDescending { it.dateEpochMs }
    }
    val linkedExpIds = remember(vehicleExpenses) {
        vehicleExpenses.mapNotNull { it.personalExpense?.id ?: it.sharedExpense?.sourceExpenseId }.toSet()
    }
    val unlinkedVisits = remember(vehicle.history, linkedExpIds, isRetainedReadOnly) {
        if (isRetainedReadOnly) emptyList()
        else vehicle.history.filter { it.expenseId.isBlank() || it.expenseId !in linkedExpIds }
    }

    val fuelExpenses = remember(vehicleExpenses) {
        vehicleExpenses.filter { it.subcategory.equals("Fuel", ignoreCase = true) }
    }
    val serviceExpenses = remember(vehicleExpenses) {
        vehicleExpenses.filter { it.subcategory.equals("Service", ignoreCase = true) }
    }
    val otherExpenses = remember(vehicleExpenses) {
        vehicleExpenses.filter {
            !it.subcategory.equals("Fuel", ignoreCase = true) &&
                !it.subcategory.equals("Service", ignoreCase = true)
        }
    }

    val fuelTotal = fuelExpenses.sumOf { it.amount }
    val serviceTotal = serviceExpenses.sumOf { it.amount } + unlinkedVisits.sumOf { it.amount }
    val otherTotal = otherExpenses.sumOf { it.amount }
    val totalSpend = fuelTotal + serviceTotal + otherTotal

    val totalLitres = fuelExpenses.sumOf { it.fuelLitres ?: 0.0 }
    val avgFuelRate = remember(fuelExpenses) {
        val withLitres = fuelExpenses.filter { (it.fuelLitres ?: 0.0) > 0.0 }
        val litresSum = withLitres.sumOf { it.fuelLitres ?: 0.0 }
        if (litresSum > 0.0) {
            withLitres.sumOf { it.amount } / litresSum
        } else {
            val rates = fuelExpenses.mapNotNull { it.fuelPricePerLitre?.takeIf { r -> r > 0.0 } }
            if (rates.isNotEmpty()) rates.average() else 0.0
        }
    }

    val computedMileage = remember(fuelExpenses, vehicle.avgMileage) {
        val odoFuelEntries = fuelExpenses
            .filter { it.odometerReading != null && (it.fuelLitres ?: 0.0) > 0.0 }
            .sortedBy { it.odometerReading }
        if (odoFuelEntries.size >= 2) {
            val distKm = (odoFuelEntries.last().odometerReading!! - odoFuelEntries.first().odometerReading!!).toDouble()
            val litresBetween = odoFuelEntries.drop(1).sumOf { it.fuelLitres ?: 0.0 }
            if (distKm > 0.0 && litresBetween > 0.0) distKm / litresBetween else vehicle.avgMileage
        } else {
            vehicle.avgMileage
        }
    }

    val odoRangeKm = remember(vehicleExpenses, unlinkedVisits, vehicle.odometer, isRetainedReadOnly) {
        val readings = buildList {
            addAll(vehicleExpenses.mapNotNull { it.odometerReading })
            addAll(unlinkedVisits.map { it.odometer })
            if (!isRetainedReadOnly) add(vehicle.odometer)
        }.filter { it > 0 }
        if (readings.size >= 2) (readings.max() - readings.min()).coerceAtLeast(0) else 0
    }
    val costPerKm = if (odoRangeKm > 0 && totalSpend > 0.0) totalSpend / odoRangeKm else 0.0

    val monthlyVehicleSpend = remember(vehicleExpenses, unlinkedVisits, nowMs) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val buckets = (5 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val label = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) ?: ""
            Triple(year, month, label)
        }
        buckets.map { (y, m, label) ->
            var fuel = 0.0
            var serviceAndOther = 0.0
            vehicleExpenses.forEach { e ->
                val c = Calendar.getInstance().apply { timeInMillis = e.dateEpochMs }
                if (c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m) {
                    if (e.subcategory.equals("Fuel", ignoreCase = true)) fuel += e.amount
                    else serviceAndOther += e.amount
                }
            }
            unlinkedVisits.forEach { v ->
                val c = Calendar.getInstance().apply { timeInMillis = v.dateEpochMs }
                if (c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m) {
                    serviceAndOther += v.amount
                }
            }
            VehicleMonthSpend(label = label, fuel = fuel, serviceAndOther = serviceAndOther)
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
                SelavuTopNavBar(title = vehicle.shortName, onBack = onBack)
            }

            if (!isOwner) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isRetainedReadOnly) extendedColors.warnSoft else colorScheme.primaryContainer.copy(alpha = 0.45f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = if (isRetainedReadOnly) "Past workspace · Read-only" else "Shared · ${ownerName ?: "Member"}",
                            status = if (isRetainedReadOnly) ServiceStatus.DUE_SOON else ServiceStatus.OK,
                            isAccent = !isRetainedReadOnly
                        )
                        Text(
                            text = if (isRetainedReadOnly) {
                                "Only your own contributions are shown. Existing entries can be deleted only."
                            } else {
                                "Shared vehicle from Family Workspace. You can view reminders & log expenses."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
                                    color = colorScheme.outline
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = IndianFormatters.formatIndianInteger(vehicle.odometer.toLong()),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = " km",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                            }
                            if (isOwner) {
                                Button(
                                    onClick = onOpenOdometerSheet,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.surfaceVariant,
                                        contentColor = colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("update_odometer_button")
                                ) {
                                    Text(strings.update, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        SelavuDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCell(
                                value = if (computedMileage > 0) "${"%.1f".format(computedMileage)}" else "—",
                                label = "km/l average",
                                modifier = Modifier.weight(1f)
                            )
                            StatCell(
                                value = IndianFormatters.formatInr(totalSpend),
                                label = if (isRetainedReadOnly) "your spend" else "total spend",
                                modifier = Modifier.weight(1f)
                            )
                            StatCell(
                                value = "${vehicleExpenses.size}",
                                label = "expenses",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isRetainedReadOnly) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenAddExpense(null) },
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("vehicle_add_expense_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add expense", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { onOpenAddExpense("Fuel") },
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("vehicle_log_fuel_button")
                                ) {
                                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fuel", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { onOpenAddExpense("Service") },
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("vehicle_log_service_button")
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Service", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (isOwner && (inWorkspace || vehicle.hiddenFromWorkspace)) {
                item {
                    SelavuCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Hide from Family Workspace",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (vehicle.hiddenFromWorkspace) {
                                        "Private — members cannot see service reminders or log expenses here"
                                    } else {
                                        "Shared with Family Workspace members"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = vehicle.hiddenFromWorkspace,
                                onCheckedChange = { onToggleHiddenFromWorkspace() },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("vehicle_hidden_workspace_switch")
                            )
                        }
                    }
                }
            }

            if (!isRetainedReadOnly && (overdueCount > 0 || soonCount > 0)) {
                item {
                    val isBad = overdueCount > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isBad) colorScheme.errorContainer else extendedColors.warnSoft)
                            .padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isBad) colorScheme.error else extendedColors.warn,
                            modifier = Modifier.size(18.dp)
                        )
                        val msg = buildString {
                            if (overdueCount > 0) append("$overdueCount overdue")
                            if (overdueCount > 0 && soonCount > 0) append(" and ")
                            if (soonCount > 0) append("$soonCount due soon")
                            if (isOwner) {
                                append(". Tap an item to mark it done or change its interval.")
                            } else {
                                append(" on this shared vehicle.")
                            }
                        }
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isBad) colorScheme.error else extendedColors.warn
                        )
                    }
                }
            }

            item {
                val tabOptions = if (isRetainedReadOnly) {
                    listOf(
                        "expenses" to "My Expenses (${vehicleExpenses.size})",
                        "overview" to "Overview"
                    )
                } else {
                    listOf(
                        "service" to "Service",
                        "expenses" to "Expenses (${vehicleExpenses.size})",
                        "overview" to "Overview"
                    )
                }
                SelavuSegmentedControl(
                    options = tabOptions,
                    selectedKey = subTab,
                    onSelect = { subTab = it },
                    modifier = Modifier.testTag("vehicle_detail_tabs")
                )
            }

            when (subTab) {
                "service" -> {
                    item {
                        SectionHeader(
                            title = strings.serviceTracker,
                            actionLabel = if (isOwner) strings.addItem else null,
                            onAction = onOpenAddServiceItem
                        )
                    }

                    items(sortedItems, key = { it.first.id }) { (item, state) ->
                        val barColor = when (state.status) {
                            ServiceStatus.OVERDUE -> colorScheme.error
                            ServiceStatus.DUE_SOON -> extendedColors.warn
                            ServiceStatus.OK -> extendedColors.ok
                        }
                        SelavuCard(
                            onClick = { if (isOwner) onOpenServiceItemSheet(item) },
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
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val lastKm = if (item.intervalKm > 0) {
                                            " · ${IndianFormatters.formatIndianInteger(item.lastDoneKm.toLong())} km"
                                        } else ""
                                        Text(
                                            text = "Last ${IndianFormatters.formatShortDate(item.lastDoneEpochMs)}$lastKm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
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
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = strings.serviceHistory,
                            actionLabel = if (!isRetainedReadOnly) strings.logVisit else null,
                            onAction = { onOpenAddExpense("Service") }
                        )
                    }

                    if (vehicle.history.isEmpty()) {
                        item {
                            SelavuCard {
                                Text(
                                    text = "No service visits logged yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
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
                                        .background(colorScheme.surface)
                                        .border(3.dp, colorScheme.primary, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${IndianFormatters.formatDate(visit.dateEpochMs)} · ${IndianFormatters.formatIndianInteger(visit.odometer.toLong())} km",
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = IndianFormatters.formatInr(visit.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = visit.workshop,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
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

                "expenses" -> {
                    item {
                        SectionHeader(
                            title = if (isRetainedReadOnly) "Your tagged expenses (${vehicleExpenses.size})"
                            else "Tagged vehicle expenses (${vehicleExpenses.size})",
                            actionLabel = if (isRetainedReadOnly) null else "+ Add expense",
                            onAction = { onOpenAddExpense(null) }
                        )
                    }
                    if (vehicleExpenses.isEmpty()) {
                        item {
                            SelavuEmptyState(
                                icon = Icons.Filled.ReceiptLong,
                                title = "No expenses tagged to ${vehicle.shortName}",
                                subtitle = if (isRetainedReadOnly) {
                                    "You have deleted or have no remaining expenses on this past-workspace vehicle."
                                } else {
                                    "Log fuel fill-ups, services, insurance, or accessories and select ${vehicle.shortName} to track all costs here."
                                },
                                actionLabel = if (isRetainedReadOnly) null else "Add expense",
                                onAction = if (isRetainedReadOnly) null else { -> onOpenAddExpense(null) }
                            )
                        }
                    } else {
                        item {
                            SelavuListContainer {
                                vehicleExpenses.forEachIndexed { idx, row ->
                                    if (idx > 0) SelavuDivider()
                                    val isFuel = row.subcategory.equals("Fuel", ignoreCase = true)
                                    val isService = row.subcategory.equals("Service", ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (row.personalExpense != null) {
                                                    onOpenExpenseDetail(row.personalExpense)
                                                } else if (row.sharedExpense != null) {
                                                    onOpenSharedExpenseDetail(row.sharedExpense)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("vehicle_expense_row_${row.id}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CategoryTile(
                                            iconKey = when {
                                                isFuel -> "fuel"
                                                isService -> "wrench"
                                                else -> vehicle.type
                                            },
                                            colorIndex = when {
                                                isFuel -> 4
                                                isService -> 1
                                                else -> 6
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = row.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (!row.ownerBadge.isNullOrBlank()) {
                                                    StatusPill(text = row.ownerBadge, isAccent = row.ownerBadge != "You")
                                                }
                                            }
                                            Text(
                                                text = "${row.subcategory.ifBlank { "Vehicle" }} · ${IndianFormatters.formatShortDate(row.dateEpochMs)} · ${row.paymentMode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val metaParts = buildList {
                                                row.fuelLitres?.let {
                                                    add("${if (it % 1.0 == 0.0) it.toInt() else "%.2f".format(it)} L")
                                                }
                                                row.fuelPricePerLitre?.let {
                                                    add("${IndianFormatters.formatInr(it, 2)}/L")
                                                }
                                                row.odometerReading?.let {
                                                    add("${IndianFormatters.formatIndianInteger(it.toLong())} km")
                                                }
                                            }
                                            if (metaParts.isNotEmpty()) {
                                                Text(
                                                    text = metaParts.joinToString(" · "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            text = IndianFormatters.formatInr(row.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Vehicle Overview Tab: KPIs + Breakdown + Monthly Spend Chart
                    item {
                        SectionHeader(title = "Vehicle cost KPIs")
                        SelavuCard(modifier = Modifier.testTag("vehicle_overview_kpi_card")) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCell(
                                        value = IndianFormatters.formatInr(totalSpend),
                                        label = "Total vehicle spend",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCell(
                                        value = IndianFormatters.formatInr(fuelTotal),
                                        label = "Fuel (${fuelExpenses.size})",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCell(
                                        value = IndianFormatters.formatInr(serviceTotal),
                                        label = "Service & repairs",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                SelavuDivider()
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCell(
                                        value = if (totalLitres > 0.0) "${"%.1f".format(totalLitres)} L" else "—",
                                        label = "Fuel bought",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCell(
                                        value = if (avgFuelRate > 0.0) "${IndianFormatters.formatInr(avgFuelRate, 2)}/L" else "—",
                                        label = "Avg fuel rate",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCell(
                                        value = if (costPerKm > 0.0) "${IndianFormatters.formatInr(costPerKm, 2)}/km"
                                        else IndianFormatters.formatInr(otherTotal),
                                        label = if (costPerKm > 0.0) "Running cost" else "Insurance & other",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Expense split")
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (totalSpend <= 0.0) {
                                    Text(
                                        text = "Add fuel, service, or vehicle expenses tagged to ${vehicle.shortName} to see your cost breakdown.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val fuelColor = extendedColors.categoryColor(4)
                                    val serviceColor = colorScheme.primary
                                    val otherColor = extendedColors.categoryColor(6)
                                    val splitEntries = listOf(
                                        Triple("Fuel", fuelTotal, fuelColor),
                                        Triple("Service", serviceTotal, serviceColor),
                                        Triple("Other", otherTotal, otherColor)
                                    ).filter { it.second > 0.0 }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .semantics { contentDescription = "Vehicle spend split bar" },
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        splitEntries.forEach { (_, amt, color) ->
                                            val weight = (amt / totalSpend).toFloat().coerceAtLeast(0.03f)
                                            Box(
                                                modifier = Modifier
                                                    .weight(weight)
                                                    .height(12.dp)
                                                    .background(color)
                                            )
                                        }
                                    }

                                    splitEntries.forEach { (label, amt, color) ->
                                        val share = ((amt / totalSpend) * 100.0).roundToInt()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$share%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = IndianFormatters.formatInr(amt),
                                                style = SelavuTheme.textStyles.numericBody,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "6-month vehicle spend")
                        SelavuCard(modifier = Modifier.testTag("vehicle_overview_chart_card")) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                VehicleSpendMonthlyChart(months = monthlyVehicleSpend)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(extendedColors.categoryColor(4))
                                        )
                                        Text("Fuel", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(colorScheme.primary)
                                        )
                                        Text("Service & other", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class VehicleMonthSpend(
    val label: String,
    val fuel: Double,
    val serviceAndOther: Double
) {
    val total: Double get() = fuel + serviceAndOther
}

@Composable
private fun VehicleSpendMonthlyChart(
    months: List<VehicleMonthSpend>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val context = LocalContext.current
    val manropeTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.manrope)
        } catch (_: Exception) {
            null
        }
    }
    val maxVal = maxOf(months.maxOfOrNull { it.total } ?: 1000.0, 1000.0)
    val gridColor = colorScheme.outlineVariant
    val fuelColor = extendedColors.categoryColor(4)
    val serviceColor = colorScheme.primary
    val emptyBarColor = colorScheme.surfaceContainerHighest
    val labelArgb = colorScheme.onSurfaceVariant.toArgb()
    val boldArgb = colorScheme.onSurface.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .semantics { contentDescription = "Monthly vehicle fuel and service spend bar chart" }
    ) {
        val leftPad = 38.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 18.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = manropeTypeface
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
            val tickText = when {
                value <= 0.0 -> "0"
                value < 1000.0 -> "${value.toInt()}"
                else -> "${(value / 1000.0).toInt()}k"
            }
            drawContext.canvas.nativeCanvas.drawText(
                tickText,
                leftPad - 6.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        val stepW = chartW / months.size.coerceAtLeast(1)
        val barW = (stepW * 0.52f).coerceAtMost(30.dp.toPx())

        val monthPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelArgb
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = manropeTypeface
        }
        val valuePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = boldArgb
            textSize = 10.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = manropeTypeface
        }

        months.forEachIndexed { idx, item ->
            val x = leftPad + idx * stepW + (stepW - barW) / 2f
            val totalH = if (item.total > 0.0) {
                ((item.total / maxVal).toFloat() * chartH).coerceAtLeast(4.dp.toPx())
            } else 2.dp.toPx()
            val topY = topPad + chartH - totalH

            if (item.total <= 0.0) {
                drawRoundRect(
                    color = emptyBarColor,
                    topLeft = Offset(x, topY),
                    size = Size(barW, totalH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            } else {
                val fuelH = ((item.fuel / item.total).toFloat() * totalH).coerceIn(0f, totalH)
                val svcH = totalH - fuelH
                if (svcH > 0f) {
                    drawRoundRect(
                        color = serviceColor,
                        topLeft = Offset(x, topY),
                        size = Size(barW, svcH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                if (fuelH > 0f) {
                    drawRoundRect(
                        color = fuelColor,
                        topLeft = Offset(x, topY + svcH),
                        size = Size(barW, fuelH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                val topLabel = if (item.total >= 1000.0) "${"%.1f".format(item.total / 1000.0)}k" else "${item.total.toInt()}"
                drawContext.canvas.nativeCanvas.drawText(
                    topLabel,
                    x + barW / 2f,
                    topY - 5.dp.toPx(),
                    valuePaint
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                x + barW / 2f,
                size.height - 5.dp.toPx(),
                monthPaint
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    project: Project,
    currentUid: String = "",
    inWorkspace: Boolean = false,
    isOwner: Boolean = true,
    ownerName: String? = null,
    isRetainedReadOnly: Boolean = false,
    memberContributions: List<SharedProjectContribution> = emptyList(),
    projectPersonalExpenses: List<Expense> = emptyList(),
    projectSharedExpenses: List<SharedExpense> = emptyList(),
    onBack: () -> Unit,
    onOpenAddPayment: () -> Unit,
    onUpdateProjectProducts: (List<ProjectProduct>) -> Unit = {},
    onToggleHiddenFromWorkspace: () -> Unit = {},
    onDeleteContribution: (SharedProjectContribution) -> Unit = {},
    onOpenExpenseDetail: (Expense) -> Unit = {},
    onOpenSharedExpenseDetail: (SharedExpense) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    var subTab by rememberSaveable(project.id, isRetainedReadOnly) {
        mutableStateOf(if (isRetainedReadOnly) "pay" else "budget")
    }
    LaunchedEffect(isRetainedReadOnly) {
        if (isRetainedReadOnly && subTab != "pay") {
            subTab = "pay"
        }
    }

    val visibleContributions = remember(memberContributions, currentUid, isOwner, isRetainedReadOnly) {
        when {
            isRetainedReadOnly -> memberContributions.filter { it.ownerUid == currentUid }
            isOwner -> memberContributions.filter { it.ownerUid != currentUid }
            else -> memberContributions
        }
    }
    val linkedExpenseIdsFromContribs = remember(visibleContributions, project.payments) {
        visibleContributions.mapNotNull { it.linkedExpenseId }.toSet()
    }
    val ownRetainedExpenses = remember(projectPersonalExpenses, projectSharedExpenses, currentUid, isRetainedReadOnly, linkedExpenseIdsFromContribs) {
        if (!isRetainedReadOnly) emptyList()
        else {
            projectPersonalExpenses.filter { it.id !in linkedExpenseIdsFromContribs }
        }
    }
    val ownRetainedSharedOnly = remember(projectSharedExpenses, ownRetainedExpenses, currentUid, isRetainedReadOnly, linkedExpenseIdsFromContribs) {
        if (!isRetainedReadOnly) emptyList()
        else {
            val personalIds = ownRetainedExpenses.map { it.id }.toSet()
            projectSharedExpenses.filter {
                it.ownerUid == currentUid &&
                    it.sourceExpenseId !in personalIds &&
                    it.sourceExpenseId !in linkedExpenseIdsFromContribs
            }
        }
    }
    val extraForeignExpenses = remember(projectSharedExpenses, isRetainedReadOnly, linkedExpenseIdsFromContribs, currentUid, isOwner) {
        if (isRetainedReadOnly) emptyList()
        else projectSharedExpenses.filter {
            it.sourceExpenseId !in linkedExpenseIdsFromContribs &&
                (!isOwner || it.ownerUid != currentUid)
        }
    }

    val contribSum = visibleContributions.sumOf { it.amount }
    val retainedExpSum = ownRetainedExpenses.sumOf { it.amount } + ownRetainedSharedOnly.sumOf { it.amount }
    val spent = if (isRetainedReadOnly) {
        contribSum + retainedExpSum
    } else {
        project.phases.sumOf { it.spent } + contribSum
    }
    val ratio = if (project.budget > 0) (spent / project.budget).toFloat() else 0f
    val pct = (ratio * 100f).roundToInt()
    val owedToVendors = if (isRetainedReadOnly) 0.0 else project.vendors.sumOf { (it.quoted - it.paid).coerceAtLeast(0.0) }
    val hotPhase = if (isRetainedReadOnly) null else project.phases.firstOrNull { ph ->
        val extra = visibleContributions.filter { it.phaseName.equals(ph.name, ignoreCase = true) }.sumOf { it.amount }
        ph.budget > 0 && (ph.spent + extra) / ph.budget >= 0.9 && (ph.spent + extra) > 0
    }
    val catalog = project.effectiveProductCatalog()

    val productAggregates = remember(project.payments, visibleContributions) {
        val map = mutableMapOf<String, Pair<Double, Double>>()
        project.payments.forEach { p ->
            p.items.forEach { item ->
                val key = item.name.lowercase()
                val prev = map[key] ?: (0.0 to 0.0)
                map[key] = (prev.first + item.qty) to (prev.second + item.totalPrice)
            }
        }
        visibleContributions.forEach { c ->
            c.items.forEach { item ->
                val key = item.name.lowercase()
                val prev = map[key] ?: (0.0 to 0.0)
                map[key] = (prev.first + item.qty) to (prev.second + item.totalPrice)
            }
        }
        map
    }

    var editingProduct by remember { mutableStateOf<ProjectProduct?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var confirmDeleteContribId by remember(project.id) { mutableStateOf<String?>(null) }

    if (showProductDialog) {
        val existing = editingProduct
        var prodName by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
        var prodUnit by remember(existing) { mutableStateOf(existing?.unit ?: "count") }

        AlertDialog(
            onDismissRequest = { showProductDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = if (existing == null) "Add product & unit" else "Edit product & unit",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelavuTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = "Product name",
                        placeholder = "e.g. Bricks, Steel, Cement",
                        modifier = Modifier.fillMaxWidth().testTag("project_product_dialog_name")
                    )
                    Text("Unit type", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Project.commonUnits.forEach { u ->
                            SelavuFilterChip(
                                label = u,
                                selected = prodUnit == u,
                                onClick = { prodUnit = u }
                            )
                        }
                    }
                    SelavuTextField(
                        value = prodUnit,
                        onValueChange = { prodUnit = it },
                        label = "Custom unit",
                        placeholder = "e.g. count, per kg, per bag",
                        modifier = Modifier.fillMaxWidth().testTag("project_product_dialog_unit")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = prodName.trim()
                        val cleanUnit = prodUnit.trim().ifBlank { "count" }
                        if (cleanName.isNotEmpty()) {
                            val updatedList = if (existing == null) {
                                val newProd = ProjectProduct(
                                    id = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                        .ifBlank { UUID.randomUUID().toString().take(6) },
                                    name = cleanName,
                                    unit = cleanUnit
                                )
                                catalog + newProd
                            } else {
                                catalog.map {
                                    if (it.id == existing.id) it.copy(name = cleanName, unit = cleanUnit) else it
                                }
                            }
                            onUpdateProjectProducts(updatedList)
                            showProductDialog = false
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("project_product_dialog_save")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
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
                SelavuTopNavBar(title = project.name, onBack = onBack)
            }

            if (!isOwner) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isRetainedReadOnly) extendedColors.warnSoft else colorScheme.primaryContainer.copy(alpha = 0.45f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = if (isRetainedReadOnly) "Past workspace · Read-only" else "Shared · ${ownerName ?: "Member"}",
                            status = if (isRetainedReadOnly) ServiceStatus.DUE_SOON else ServiceStatus.OK,
                            isAccent = !isRetainedReadOnly
                        )
                        Text(
                            text = if (isRetainedReadOnly) {
                                "Only your own contributions are shown. Existing entries can be deleted only."
                            } else {
                                "Shared project from Family Workspace. You can view progress and add payments."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SelavuCard {
                    Column {
                        Text(
                            text = if (isRetainedReadOnly) "YOUR CONTRIBUTIONS" else "SPENT SO FAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = IndianFormatters.formatLakhCrore(spent),
                                style = MaterialTheme.typography.displayMedium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "of ${IndianFormatters.formatLakhCrore(project.budget)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SelavuProgressBar(progress = ratio, color = colorScheme.primary, height = 10.dp)
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
                            if (isRetainedReadOnly) {
                                StatCell(
                                    "${visibleContributions.size + ownRetainedExpenses.size + ownRetainedSharedOnly.size}",
                                    "your entries",
                                    Modifier.weight(1f)
                                )
                            } else {
                                StatCell(
                                    IndianFormatters.formatLakhCrore(owedToVendors),
                                    "owed to vendors",
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (isOwner && (inWorkspace || project.hiddenFromWorkspace)) {
                item {
                    SelavuCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Hide from Family Workspace",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (project.hiddenFromWorkspace) {
                                        "Private — members cannot view this project or log contributions"
                                    } else {
                                        "Shared with Family Workspace members"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = project.hiddenFromWorkspace,
                                onCheckedChange = { onToggleHiddenFromWorkspace() },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("project_hidden_workspace_switch")
                            )
                        }
                    }
                }
            }

            if (hotPhase != null) {
                item {
                    val extra = visibleContributions.filter { it.phaseName.equals(hotPhase.name, ignoreCase = true) }.sumOf { it.amount }
                    val phasePct = (((hotPhase.spent + extra) / hotPhase.budget) * 100.0).roundToInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(extendedColors.warnSoft)
                            .padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = extendedColors.warn, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${hotPhase.name} has used $phasePct% of its budget. Check pending bills before approving more work.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = extendedColors.warn
                        )
                    }
                }
            }

            if (!isRetainedReadOnly) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    SelavuSegmentedControl(
                        options = listOf(
                            "budget" to "Budget",
                            "vend" to "Vendors",
                            "pay" to "Payments",
                            "prod" to "Products"
                        ),
                        selectedKey = subTab,
                        onSelect = { subTab = it },
                        modifier = Modifier.testTag("project_detail_tabs")
                    )
                }
            }

            when (subTab) {
                "budget" -> {
                    items(project.phases) { phase ->
                        val extraPhase = visibleContributions
                            .filter { it.phaseName.equals(phase.name, ignoreCase = true) }
                            .sumOf { it.amount }
                        val phaseTotalSpent = phase.spent + extraPhase
                        val pRatio = if (phase.budget > 0) (phaseTotalSpent / phase.budget).toFloat() else 0f
                        val barColor = when {
                            pRatio >= 1f -> colorScheme.error
                            pRatio >= 0.9f -> extendedColors.warn
                            else -> extendedColors.ok
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
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = IndianFormatters.formatLakhCrore(phaseTotalSpent),
                                        style = SelavuTheme.textStyles.numericBody,
                                        color = colorScheme.onSurface
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
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (phaseTotalSpent == 0.0) "Not started" else "${(pRatio * 100).roundToInt()}% used",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurfaceVariant
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
                                Text(
                                    text = "No vendors added yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
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
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${IndianFormatters.formatLakhCrore((vendor.quoted - vendor.paid).coerceAtLeast(0.0))} due",
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SelavuProgressBar(progress = vRatio, color = colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(vendor.role, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "Paid ${IndianFormatters.formatLakhCrore(vendor.paid)} of ${IndianFormatters.formatLakhCrore(vendor.quoted)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "pay" -> {
                    if (isRetainedReadOnly) {
                        item {
                            SectionHeader(
                                title = "Your contributions (${visibleContributions.size + ownRetainedExpenses.size + ownRetainedSharedOnly.size})"
                            )
                        }
                    }
                    item {
                        SelavuListContainer {
                            val ownerPayments = if (isRetainedReadOnly) emptyList() else project.payments
                            val hasAnyPay = ownerPayments.isNotEmpty() ||
                                visibleContributions.isNotEmpty() ||
                                ownRetainedExpenses.isNotEmpty() ||
                                ownRetainedSharedOnly.isNotEmpty() ||
                                extraForeignExpenses.isNotEmpty()
                            if (!hasAnyPay) {
                                Text(
                                    text = if (isRetainedReadOnly) "No remaining contributions found." else "No payments recorded yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                var renderedCount = 0
                                visibleContributions.forEach { contrib ->
                                    if (renderedCount > 0) SelavuDivider()
                                    renderedCount++
                                    val canDeleteContrib = contrib.ownerUid == currentUid
                                    val confirming = confirmDeleteContribId == contrib.id
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("project_contrib_row_${contrib.id}"),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = contrib.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    StatusPill(
                                                        text = if (contrib.ownerUid == currentUid) "You" else contrib.ownerName,
                                                        isAccent = contrib.ownerUid != currentUid
                                                    )
                                                }
                                                val phasePart = contrib.phaseName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                                                Text(
                                                    text = "${IndianFormatters.formatShortDate(contrib.dateEpochMs)} · ${contrib.vendor}$phasePart",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = IndianFormatters.formatInr(contrib.amount),
                                                style = SelavuTheme.textStyles.numericBody,
                                                color = colorScheme.onSurface
                                            )
                                            if (canDeleteContrib) {
                                                IconButton(
                                                    onClick = {
                                                        if (confirming) {
                                                            confirmDeleteContribId = null
                                                            onDeleteContribution(contrib)
                                                        } else {
                                                            confirmDeleteContribId = contrib.id
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_project_contrib_${contrib.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = if (confirming) "Confirm delete contribution" else "Delete contribution",
                                                        tint = if (confirming) colorScheme.error else colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (confirming) {
                                            Text(
                                                text = "Tap the trash icon again to delete this contribution",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.error
                                            )
                                        }
                                        if (contrib.items.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.small)
                                                    .background(colorScheme.surfaceVariant)
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                contrib.items.forEach { item ->
                                                    val qtyStr = if (item.qty % 1.0 == 0.0) {
                                                        IndianFormatters.formatIndianInteger(item.qty.toLong())
                                                    } else "%.2f".format(item.qty)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${item.name}: $qtyStr ${item.unit} × ${IndianFormatters.formatInr(item.unitPrice, 2)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = IndianFormatters.formatInr(item.totalPrice, 2),
                                                            style = SelavuTheme.textStyles.numericCaption,
                                                            color = colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ownerPayments.forEach { payment ->
                                    if (renderedCount > 0) SelavuDivider()
                                    renderedCount++
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = payment.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (!isOwner && !ownerName.isNullOrBlank()) {
                                                        StatusPill(text = ownerName, isAccent = true)
                                                    }
                                                }
                                                val phasePart = payment.phaseName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                                                Text(
                                                    text = "${IndianFormatters.formatShortDate(payment.dateEpochMs)} · ${payment.vendor}$phasePart",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = IndianFormatters.formatInr(payment.amount),
                                                style = SelavuTheme.textStyles.numericBody,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                        if (payment.items.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.small)
                                                    .background(colorScheme.surfaceVariant)
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                payment.items.forEach { item ->
                                                    val qtyStr = if (item.qty % 1.0 == 0.0) {
                                                        IndianFormatters.formatIndianInteger(item.qty.toLong())
                                                    } else "%.2f".format(item.qty)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${item.name}: $qtyStr ${item.unit} × ${IndianFormatters.formatInr(item.unitPrice, 2)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = IndianFormatters.formatInr(item.totalPrice, 2),
                                                            style = SelavuTheme.textStyles.numericCaption,
                                                            color = colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ownRetainedExpenses.forEach { exp ->
                                    if (renderedCount > 0) SelavuDivider()
                                    renderedCount++
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenExpenseDetail(exp) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("project_retained_exp_${exp.id}"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exp.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${IndianFormatters.formatShortDate(exp.dateEpochMs)} · ${exp.paymentMode} · Tap to delete",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = IndianFormatters.formatInr(exp.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }

                                ownRetainedSharedOnly.forEach { se ->
                                    if (renderedCount > 0) SelavuDivider()
                                    renderedCount++
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenSharedExpenseDetail(se) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("project_retained_shared_exp_${se.id}"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = se.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${IndianFormatters.formatShortDate(se.dateEpochMs)} · ${se.paymentMode} · Tap to delete",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = IndianFormatters.formatInr(se.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }

                                extraForeignExpenses.forEach { se ->
                                    if (renderedCount > 0) SelavuDivider()
                                    renderedCount++
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenSharedExpenseDetail(se) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = se.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                StatusPill(text = se.ownerName, isAccent = true)
                                            }
                                            Text(
                                                text = "${IndianFormatters.formatShortDate(se.dateEpochMs)} · ${se.paymentMode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = IndianFormatters.formatInr(se.amount),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        SectionHeader(
                            title = "Products & configurable units (${catalog.size})",
                            actionLabel = if (isOwner) "+ Add product" else null,
                            onAction = {
                                editingProduct = null
                                showProductDialog = true
                            }
                        )
                    }
                    item {
                        SelavuListContainer {
                            catalog.forEachIndexed { idx, prod ->
                                if (idx > 0) SelavuDivider()
                                val stats = productAggregates[prod.name.lowercase()]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("project_product_row_${prod.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            StatusPill(text = prod.unit, isAccent = true)
                                        }
                                        val qtyUsed = stats?.first ?: 0.0
                                        val spendUsed = stats?.second ?: 0.0
                                        Text(
                                            text = if (qtyUsed > 0.0 || spendUsed > 0.0) {
                                                val qtyStr = if (qtyUsed % 1.0 == 0.0) {
                                                    IndianFormatters.formatIndianInteger(qtyUsed.toLong())
                                                } else "%.2f".format(qtyUsed)
                                                "Logged: $qtyStr ${prod.unit} · ${IndianFormatters.formatInr(spendUsed)}"
                                            } else {
                                                "Unit: ${prod.unit} · Ready for granular payments"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isOwner) {
                                        IconButton(
                                            onClick = {
                                                editingProduct = prod
                                                showProductDialog = true
                                            },
                                            modifier = Modifier.size(48.dp).testTag("edit_project_product_${prod.id}")
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${prod.name}", tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                        if (catalog.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    onUpdateProjectProducts(catalog.filterNot { it.id == prod.id })
                                                },
                                                modifier = Modifier.size(48.dp).testTag("delete_project_product_${prod.id}")
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${prod.name}", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (isOwner) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    editingProduct = null
                                    showProductDialog = true
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_project_product_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add product & unit type")
                            }
                        }
                    }
                }
            }

            if (!isRetainedReadOnly) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onOpenAddPayment,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
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
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = value,
            style = SelavuTheme.textStyles.numericTitle,
            color = colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
