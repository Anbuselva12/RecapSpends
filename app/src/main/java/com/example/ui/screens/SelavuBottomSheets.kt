package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.firebase.SampleDataSeeder
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ProjectPaymentItem
import com.example.data.model.ProjectProduct
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceStatus
import com.example.data.model.SharedExpense
import com.example.data.model.Vehicle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.IconButton
import com.example.ui.components.CategoryTile
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTextField
import com.example.ui.components.StatusPill
import com.example.ui.components.iconForKey
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import java.util.UUID

sealed interface ActiveSheet {
    data object QuickAddHub : ActiveSheet
    data class OdometerUpdate(val vehicleId: String) : ActiveSheet
    data class ServiceItemDetail(val vehicleId: String, val itemId: String) : ActiveSheet
    data class AddServiceItem(val vehicleId: String) : ActiveSheet
    data object AddVehicle : ActiveSheet
    data object NewBillReminder : ActiveSheet
    data class EditBillReminder(val bill: Bill) : ActiveSheet
    data object AddProject : ActiveSheet
    data class AddProjectPayment(val projectId: String) : ActiveSheet
    data class CategoryEdit(val category: Category?) : ActiveSheet
    data class ExpenseDetail(val expense: Expense) : ActiveSheet
    data class SharedExpenseDetail(val sharedExpense: SharedExpense) : ActiveSheet
    data class CategoryDrilldown(val category: Category) : ActiveSheet
    data class EditScannedItem(val item: ScannedBillItem, val vehicle: Vehicle?) : ActiveSheet
    data class EditMonthlyBudget(val currentBudget: Double) : ActiveSheet
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelavuModalSheetHost(
    activeSheet: ActiveSheet,
    categories: List<Category>,
    expenses: List<Expense>,
    vehicles: List<Vehicle>,
    projects: List<Project>,
    currentUid: String = "",
    retainedVehicleIds: Set<String> = emptySet(),
    retainedProjectIds: Set<String> = emptySet(),
    reviewDraftCount: Int = 0,
    inWorkspace: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateScan: () -> Unit,
    onNavigateAddExpense: (catId: String?, sub: String?) -> Unit,
    onNavigateReview: () -> Unit = {},
    onNavigateWorkspace: () -> Unit = {},
    onOpenAddVehicle: () -> Unit,
    onOpenNewProject: () -> Unit,
    onOpenNewReminder: () -> Unit,
    onUpdateOdometer: (Vehicle, Int) -> Unit,
    onMarkServiceDone: (Vehicle, String) -> Unit,
    onUpdateServiceInterval: (Vehicle, String, Int, Int) -> Unit,
    onDeleteServiceItem: (Vehicle, String) -> Unit,
    onAddServiceItem: (Vehicle, ServiceItem) -> Unit,
    onCreateVehicle: (String, String, String, Int, Double, Boolean) -> Unit,
    onCreateBill: (Bill) -> Unit,
    onUpdateBill: (Bill) -> Unit = {},
    onDeleteBill: (String) -> Unit = {},
    onCreateProject: (String, Double, String, Boolean) -> Unit,
    onAddProjectPayment: (Project, Int, String, Double, String, List<ProjectPaymentItem>, List<ProjectProduct>?) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onToggleExpensePrivacy: (Expense) -> Unit = {},
    onEditExpense: (Expense) -> Unit = {},
    onDeleteExpense: (String) -> Unit = {},
    onDeleteRetainedExpense: (SharedExpense) -> Unit = {},
    onUpdateScannedItem: (Int, ScannedBillItem) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onShowToast: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeSheet) {
                is ActiveSheet.QuickAddHub -> {
                    Text("Quick add", style = MaterialTheme.typography.headlineMedium, color = colorScheme.onSurface)
                    SheetActionRow(
                        icon = Icons.Filled.DocumentScanner,
                        title = strings.scanBill,
                        subtitle = "AI reads items, HSN, GST & links vehicle services",
                        badge = "AI",
                        onClick = { onDismiss(); onNavigateScan() }
                    )
                    SheetActionRow(
                        icon = Icons.Filled.Add,
                        title = strings.addExpense,
                        subtitle = "Cash, UPI, card or bank transfer",
                        onClick = { onDismiss(); onNavigateAddExpense(null, null) }
                    )
                    SheetActionRow(
                        icon = Icons.Filled.Build,
                        title = strings.logService,
                        subtitle = "Record garage visit & reset service items",
                        onClick = { onDismiss(); onNavigateAddExpense("veh", "Service") }
                    )
                    SheetActionRow(
                        icon = Icons.Filled.Notifications,
                        title = strings.newReminder,
                        subtitle = "Recurring or one-off bill with alert",
                        onClick = onOpenNewReminder
                    )
                    SheetActionRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = strings.reviewAlerts,
                        subtitle = "Approve or map notification expense alerts",
                        badge = if (reviewDraftCount > 0) "$reviewDraftCount" else null,
                        onClick = { onDismiss(); onNavigateReview() }
                    )
                    SheetActionRow(
                        icon = Icons.Filled.Groups,
                        title = strings.familyWorkspace,
                        subtitle = if (inWorkspace) "Manage shared workspace & members" else "Share expenses with family",
                        onClick = { onDismiss(); onNavigateWorkspace() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAddVehicle,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addVehicle, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenNewProject,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.HomeWork, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.newProject, maxLines = 1)
                        }
                    }
                }

                is ActiveSheet.OdometerUpdate -> {
                    val vehicle = vehicles.firstOrNull { it.id == activeSheet.vehicleId }
                    if (vehicle != null) {
                        var odoText by remember(vehicle.id) { mutableStateOf(vehicle.odometer.toString()) }
                        Text("Update odometer", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${vehicle.name} · current ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        SelavuTextField(
                            value = odoText,
                            onValueChange = { if (it.all(Char::isDigit)) odoText = it },
                            label = "New odometer reading (km)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("odo_sheet_input")
                        )
                        Button(
                            onClick = {
                                val next = odoText.toIntOrNull()
                                if (next == null || next < vehicle.odometer) {
                                    onShowToast("Enter at least ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km")
                                } else {
                                    onUpdateOdometer(vehicle, next)
                                    onDismiss()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Save odometer") }
                    }
                }

                is ActiveSheet.ServiceItemDetail -> {
                    val vehicle = vehicles.firstOrNull { it.id == activeSheet.vehicleId }
                    val item = vehicle?.items?.firstOrNull { it.id == activeSheet.itemId }
                    if (vehicle != null && item != null) {
                        val state = item.computeState(vehicle.odometer, System.currentTimeMillis())
                        var kmText by remember(item.id) { mutableStateOf(item.intervalKm.toString()) }
                        var moText by remember(item.id) { mutableStateOf(item.intervalMonths.toString()) }
                        var confirmDelete by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                            StatusPill(state.statusLabel, status = state.status)
                        }
                        Text(state.nextDueLabel, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)

                        Button(
                            onClick = {
                                onMarkServiceDone(vehicle, item.id)
                                onDismiss()
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mark_service_done_btn")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark done today at ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km")
                        }

                        SelavuDivider()
                        Text("Edit service interval", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelavuTextField(
                                value = kmText,
                                onValueChange = { if (it.all(Char::isDigit)) kmText = it },
                                label = "Every km (0 = date only)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            SelavuTextField(
                                value = moText,
                                onValueChange = { if (it.all(Char::isDigit)) moText = it },
                                label = "Every months",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val km = kmText.toIntOrNull() ?: item.intervalKm
                                    val mo = (moText.toIntOrNull() ?: item.intervalMonths).coerceAtLeast(1)
                                    onUpdateServiceInterval(vehicle, item.id, km, mo)
                                    onDismiss()
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) { Text("Save interval") }

                            OutlinedButton(
                                onClick = {
                                    if (!confirmDelete) {
                                        confirmDelete = true
                                    } else {
                                        onDeleteServiceItem(vehicle, item.id)
                                        onDismiss()
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (confirmDelete) "Confirm delete" else "Delete")
                            }
                        }
                    }
                }

                is ActiveSheet.AddServiceItem -> {
                    val vehicle = vehicles.firstOrNull { it.id == activeSheet.vehicleId }
                    if (vehicle != null) {
                        val existingIds = vehicle.items.map { it.id }.toSet()
                        val suggestions = (if (vehicle.type == "car") SampleDataSeeder.carExtraSuggestions else SampleDataSeeder.bikeExtraSuggestions)
                            .filter { it.id !in existingIds }
                        var name by remember { mutableStateOf("") }
                        var kmText by remember { mutableStateOf("5000") }
                        var moText by remember { mutableStateOf("12") }

                        Text("Add service item", style = MaterialTheme.typography.headlineMedium)
                        if (suggestions.isNotEmpty()) {
                            Text("Suggested extras for ${vehicle.shortName}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                suggestions.forEach { sug ->
                                    SelavuFilterChip(
                                        label = sug.name,
                                        selected = name == sug.name,
                                        onClick = {
                                            name = sug.name
                                            kmText = sug.intervalKm.toString()
                                            moText = sug.intervalMonths.toString()
                                        }
                                    )
                                }
                            }
                        }
                        SelavuTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Item name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelavuTextField(
                                value = kmText,
                                onValueChange = { if (it.all(Char::isDigit)) kmText = it },
                                label = "Km (0 = date only)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            SelavuTextField(
                                value = moText,
                                onValueChange = { if (it.all(Char::isDigit)) moText = it },
                                label = "Months",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    onShowToast("Enter a service item name")
                                } else {
                                    val id = "svc_${UUID.randomUUID().toString().take(6)}"
                                    onAddServiceItem(
                                        vehicle,
                                        ServiceItem(
                                            id = id,
                                            name = name.trim(),
                                            iconKey = "wrench",
                                            intervalKm = kmText.toIntOrNull() ?: 0,
                                            intervalMonths = (moText.toIntOrNull() ?: 12).coerceAtLeast(1)
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Start tracking") }
                    }
                }

                is ActiveSheet.AddVehicle -> {
                    var type by remember { mutableStateOf("bike") }
                    var name by remember { mutableStateOf("") }
                    var plate by remember { mutableStateOf("") }
                    var odo by remember { mutableStateOf("") }
                    var mileage by remember { mutableStateOf("") }
                    var hiddenFromWorkspace by remember { mutableStateOf(false) }

                    Text(strings.addVehicle, style = MaterialTheme.typography.headlineMedium)
                    SelavuSegmentedControl(
                        options = listOf("bike" to "Bike / Scooter (8 items)", "car" to "Car (6 items)"),
                        selectedKey = type,
                        onSelect = { type = it }
                    )
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Make & model",
                        placeholder = "e.g. Honda Activa 6G, Hyundai Creta",
                        modifier = Modifier.fillMaxWidth().testTag("new_vehicle_name")
                    )
                    SelavuTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        label = "Registration plate",
                        placeholder = "TN 09 CD 1234",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelavuTextField(
                            value = odo,
                            onValueChange = { if (it.all(Char::isDigit)) odo = it },
                            label = "Odometer (km)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        SelavuTextField(
                            value = mileage,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d?$"))) mileage = it },
                            label = "Avg km/l",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (inWorkspace) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Hide from Family Workspace", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Keep this vehicle and its service alerts private to you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hiddenFromWorkspace,
                                onCheckedChange = { hiddenFromWorkspace = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("add_vehicle_hidden_switch")
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                onShowToast("Enter vehicle name")
                            } else {
                                onCreateVehicle(
                                    name.trim(),
                                    type,
                                    plate.trim(),
                                    odo.toIntOrNull() ?: 0,
                                    mileage.toDoubleOrNull() ?: if (type == "car") 16.0 else 40.0,
                                    hiddenFromWorkspace
                                )
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_vehicle_btn")
                    ) { Text("Create with default checklist") }
                }

                is ActiveSheet.NewBillReminder -> {
                    val nowMs = remember { System.currentTimeMillis() }
                    var name by remember { mutableStateOf("") }
                    var amount by remember { mutableStateOf("") }
                    var dueMs by remember { mutableStateOf(IndianFormatters.addDays(nowMs, 5)) }
                    var showPicker by remember { mutableStateOf(false) }
                    var repeat by remember { mutableStateOf("Monthly") }
                    var reminder by remember { mutableStateOf("3 days before") }
                    var catId by remember { mutableStateOf("util") }
                    var autopay by remember { mutableStateOf(false) }
                    var hiddenFromWorkspace by remember { mutableStateOf(false) }

                    if (showPicker) {
                        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueMs)
                        DatePickerDialog(
                            onDismissRequest = { showPicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    pickerState.selectedDateMillis?.let { dueMs = it }
                                    showPicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
                        ) { DatePicker(state = pickerState) }
                    }

                    Text(strings.newReminder, style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Bill title",
                        placeholder = "e.g. BESCOM electricity, Broadband",
                        modifier = Modifier.fillMaxWidth().testTag("new_bill_name")
                    )
                    SelavuTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                        label = "Amount (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("new_bill_amount")
                    )
                    Text("Due date: ${IndianFormatters.formatDate(dueMs)}", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelavuFilterChip("In 3 days", IndianFormatters.sameDay(dueMs, IndianFormatters.addDays(nowMs, 3)), { dueMs = IndianFormatters.addDays(nowMs, 3) })
                        SelavuFilterChip("In 7 days", IndianFormatters.sameDay(dueMs, IndianFormatters.addDays(nowMs, 7)), { dueMs = IndianFormatters.addDays(nowMs, 7) })
                        SelavuFilterChip("Pick date", false, { showPicker = true }, icon = Icons.Filled.CalendarMonth)
                    }
                    Text("Repeat", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Once", "Monthly", "Bi-monthly", "Half-yearly", "Yearly").forEach { r ->
                            SelavuFilterChip(r, repeat == r, { repeat = r })
                        }
                    }
                    Text("Remind me", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("On the day", "3 days before", "7 days before").forEach { rem ->
                            SelavuFilterChip(rem, reminder == rem, { reminder = rem })
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.take(8).forEach { cat ->
                            SelavuFilterChip(cat.name.substringBefore(" & "), catId == cat.id, { catId = cat.id })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Autopay enabled", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autopay, onCheckedChange = { autopay = it }, colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary))
                    }
                    if (inWorkspace) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Hide from Family Workspace", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Keep this bill reminder private to you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hiddenFromWorkspace,
                                onCheckedChange = { hiddenFromWorkspace = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("new_bill_hidden_switch")
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (name.isBlank() || amt == null || amt <= 0.0) {
                                onShowToast("Enter bill title and valid amount")
                            } else {
                                val catIcon = categories.firstOrNull { it.id == catId }?.iconKey ?: "bell"
                                onCreateBill(
                                    Bill(
                                        name = name.trim(),
                                        amount = amt,
                                        dueEpochMs = dueMs,
                                        repeat = repeat,
                                        reminder = reminder,
                                        categoryId = catId,
                                        iconKey = catIcon,
                                        autopay = autopay,
                                        hiddenFromWorkspace = hiddenFromWorkspace
                                    )
                                )
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_bill_btn")
                    ) { Text("Save bill reminder") }
                }

                is ActiveSheet.EditBillReminder -> {
                    val existing = activeSheet.bill
                    var name by remember(existing.id) { mutableStateOf(existing.name) }
                    var amount by remember(existing.id) {
                        mutableStateOf(
                            if (existing.amount % 1.0 == 0.0) existing.amount.toInt().toString()
                            else "%.2f".format(existing.amount)
                        )
                    }
                    var dueMs by remember(existing.id) { mutableStateOf(existing.dueEpochMs) }
                    var showPicker by remember { mutableStateOf(false) }
                    var repeat by remember(existing.id) { mutableStateOf(existing.repeat) }
                    var reminder by remember(existing.id) { mutableStateOf(existing.reminder) }
                    var catId by remember(existing.id) { mutableStateOf(existing.categoryId) }
                    var autopay by remember(existing.id) { mutableStateOf(existing.autopay) }
                    var hiddenFromWorkspace by remember(existing.id) { mutableStateOf(existing.hiddenFromWorkspace) }
                    var confirmDelete by remember(existing.id) { mutableStateOf(false) }

                    if (showPicker) {
                        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueMs)
                        DatePickerDialog(
                            onDismissRequest = { showPicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    pickerState.selectedDateMillis?.let { dueMs = it }
                                    showPicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
                        ) { DatePicker(state = pickerState) }
                    }

                    Text("Edit bill reminder", style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Bill title",
                        modifier = Modifier.fillMaxWidth().testTag("edit_bill_name")
                    )
                    SelavuTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                        label = "Amount (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("edit_bill_amount")
                    )
                    Text("Due date: ${IndianFormatters.formatDate(dueMs)}", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nowMs = remember { System.currentTimeMillis() }
                        SelavuFilterChip("In 3 days", IndianFormatters.sameDay(dueMs, IndianFormatters.addDays(nowMs, 3)), { dueMs = IndianFormatters.addDays(nowMs, 3) })
                        SelavuFilterChip("In 7 days", IndianFormatters.sameDay(dueMs, IndianFormatters.addDays(nowMs, 7)), { dueMs = IndianFormatters.addDays(nowMs, 7) })
                        SelavuFilterChip("Pick date", false, { showPicker = true }, icon = Icons.Filled.CalendarMonth)
                    }
                    Text("Repeat", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Once", "Monthly", "Bi-monthly", "Half-yearly", "Yearly").forEach { r ->
                            SelavuFilterChip(r, repeat == r, { repeat = r })
                        }
                    }
                    Text("Remind me", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("On the day", "3 days before", "7 days before").forEach { rem ->
                            SelavuFilterChip(rem, reminder == rem, { reminder = rem })
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.take(8).forEach { cat ->
                            SelavuFilterChip(cat.name.substringBefore(" & "), catId == cat.id, { catId = cat.id })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Autopay enabled", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autopay, onCheckedChange = { autopay = it }, colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary))
                    }
                    if (inWorkspace || hiddenFromWorkspace) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Hide from Family Workspace", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Keep this bill reminder private to you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hiddenFromWorkspace,
                                onCheckedChange = { hiddenFromWorkspace = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("edit_bill_hidden_switch")
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (name.isBlank() || amt == null || amt <= 0.0) {
                                onShowToast("Enter bill title and valid amount")
                            } else {
                                val catIcon = categories.firstOrNull { it.id == catId }?.iconKey ?: existing.iconKey
                                onUpdateBill(
                                    existing.copy(
                                        name = name.trim(),
                                        amount = amt,
                                        dueEpochMs = dueMs,
                                        repeat = repeat,
                                        reminder = reminder,
                                        categoryId = catId,
                                        iconKey = catIcon,
                                        autopay = autopay,
                                        hiddenFromWorkspace = hiddenFromWorkspace
                                    )
                                )
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("update_bill_btn")
                    ) { Text("Save changes") }
                    OutlinedButton(
                        onClick = {
                            if (!confirmDelete) {
                                confirmDelete = true
                            } else {
                                onDeleteBill(existing.id)
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_bill_btn")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (confirmDelete) "Tap again to delete reminder" else "Delete reminder")
                    }
                }

                is ActiveSheet.AddProject -> {
                    var name by remember { mutableStateOf("") }
                    var budget by remember { mutableStateOf("") }
                    var template by remember { mutableStateOf("House build") }
                    var hiddenFromWorkspace by remember { mutableStateOf(false) }

                    Text(strings.newProject, style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Project name",
                        placeholder = "e.g. Madurai House, Bakery Launch",
                        modifier = Modifier.fillMaxWidth()
                    )
                    SelavuTextField(
                        value = budget,
                        onValueChange = { if (it.all(Char::isDigit)) budget = it },
                        label = "Total budget (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Phase template", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleDataSeeder.projectTemplates.keys.forEach { t ->
                            SelavuFilterChip(t, template == t, { template = t })
                        }
                    }
                    if (inWorkspace) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Hide from Family Workspace", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Keep this project private to your Individual account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hiddenFromWorkspace,
                                onCheckedChange = { hiddenFromWorkspace = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("add_project_hidden_switch")
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val b = budget.toDoubleOrNull()
                            if (name.isBlank() || b == null || b <= 0.0) {
                                onShowToast("Enter project name and budget")
                            } else {
                                onCreateProject(name.trim(), b, template, hiddenFromWorkspace)
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Create project") }
                }

                is ActiveSheet.AddProjectPayment -> {
                    val project = projects.firstOrNull { it.id == activeSheet.projectId }
                    if (project != null) {
                        var phaseIdx by remember { mutableIntStateOf(0) }
                        var title by remember { mutableStateOf("") }
                        var amount by remember { mutableStateOf("") }
                        var lastAutoAmount by remember { mutableStateOf("") }
                        var vendor by remember { mutableStateOf(project.vendors.firstOrNull()?.name.orEmpty()) }

                        val catalog = remember(project.id) {
                            mutableStateListOf<ProjectProduct>().apply {
                                addAll(project.effectiveProductCatalog())
                            }
                        }
                        var catalogDirty by remember(project.id) { mutableStateOf(false) }

                        data class DraftPayLine(
                            val id: String = UUID.randomUUID().toString(),
                            val productId: String,
                            val name: String,
                            val unit: String,
                            val qtyText: String = "",
                            val unitPriceText: String = ""
                        )

                        val lineItems = remember(project.id) { mutableStateListOf<DraftPayLine>() }

                        var showNewProductForm by remember { mutableStateOf(false) }
                        var newProdName by remember { mutableStateOf("") }
                        var newProdUnit by remember { mutableStateOf("count") }

                        val computedLinesTotal = lineItems.sumOf { line ->
                            val q = line.qtyText.toDoubleOrNull() ?: 0.0
                            val p = line.unitPriceText.toDoubleOrNull() ?: 0.0
                            q * p
                        }
                        val formattedLinesTotal = if (computedLinesTotal > 0.0) {
                            if (computedLinesTotal % 1.0 == 0.0) computedLinesTotal.toInt().toString()
                            else "%.2f".format(computedLinesTotal)
                        } else ""

                        LaunchedEffect(formattedLinesTotal, lineItems.size) {
                            if (lineItems.isNotEmpty() && formattedLinesTotal.isNotEmpty() &&
                                (amount.isBlank() || amount == lastAutoAmount)
                            ) {
                                amount = formattedLinesTotal
                                lastAutoAmount = formattedLinesTotal
                            }
                        }

                        Text("Record project payment", style = MaterialTheme.typography.headlineMedium)
                        Text("Phase", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            project.phases.forEachIndexed { idx, ph ->
                                SelavuFilterChip(ph.name, phaseIdx == idx, { phaseIdx = idx })
                            }
                        }
                        if (project.vendors.isNotEmpty()) {
                            Text("Vendor", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                project.vendors.forEach { v ->
                                    SelavuFilterChip(v.name, vendor == v.name, { vendor = v.name })
                                }
                            }
                        }
                        SelavuTextField(
                            value = vendor,
                            onValueChange = { vendor = it },
                            label = "Vendor name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        SelavuTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Payment description",
                            placeholder = "e.g. Bricks & cement delivery",
                            modifier = Modifier.fillMaxWidth().testTag("project_payment_title_input")
                        )

                        SelavuDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Granular materials / products", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Qty × Price with product units (count, per kg, per bag…)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    val defaultProd = catalog.firstOrNull() ?: ProjectProduct("item", "Material", "count")
                                    lineItems += DraftPayLine(
                                        productId = defaultProd.id,
                                        name = defaultProd.name,
                                        unit = defaultProd.unit
                                    )
                                },
                                modifier = Modifier.testTag("add_project_line_item_btn")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add item")
                            }
                        }

                        if (showNewProductForm) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(colorScheme.surfaceVariant)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Add new product & unit", style = MaterialTheme.typography.labelLarge)
                                    IconButton(onClick = { showNewProductForm = false }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close new product form", modifier = Modifier.size(18.dp))
                                    }
                                }
                                SelavuTextField(
                                    value = newProdName,
                                    onValueChange = { newProdName = it },
                                    label = "Product name",
                                    placeholder = "e.g. TMT Bars, Plumbing Pipes",
                                    modifier = Modifier.fillMaxWidth().testTag("new_project_product_name")
                                )
                                Text("Unit type", style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Project.commonUnits.forEach { u ->
                                        SelavuFilterChip(u, newProdUnit == u, { newProdUnit = u })
                                    }
                                }
                                SelavuTextField(
                                    value = newProdUnit,
                                    onValueChange = { newProdUnit = it },
                                    label = "Custom unit",
                                    placeholder = "e.g. count, per kg, per bag",
                                    modifier = Modifier.fillMaxWidth().testTag("new_project_product_unit")
                                )
                                Button(
                                    onClick = {
                                        val cleanName = newProdName.trim()
                                        val cleanUnit = newProdUnit.trim().ifBlank { "count" }
                                        if (cleanName.isBlank()) {
                                            onShowToast("Enter product name")
                                        } else {
                                            val newProd = ProjectProduct(
                                                id = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                                    .ifBlank { UUID.randomUUID().toString().take(6) },
                                                name = cleanName,
                                                unit = cleanUnit
                                            )
                                            catalog += newProd
                                            catalogDirty = true
                                            lineItems += DraftPayLine(
                                                productId = newProd.id,
                                                name = newProd.name,
                                                unit = newProd.unit
                                            )
                                            newProdName = ""
                                            newProdUnit = "count"
                                            showNewProductForm = false
                                        }
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_inline_project_product_btn")
                                ) {
                                    Text("Save product & add line")
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showNewProductForm = true },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_new_product_inline_btn")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New product & unit type")
                            }
                        }

                        lineItems.forEachIndexed { idx, line ->
                            val qtyVal = line.qtyText.toDoubleOrNull() ?: 0.0
                            val priceVal = line.unitPriceText.toDoubleOrNull() ?: 0.0
                            val lineTotal = qtyVal * priceVal
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${line.name} (${line.unit})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { lineItems.removeAt(idx) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove line", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    catalog.forEach { prod ->
                                        SelavuFilterChip(
                                            label = "${prod.name} · ${prod.unit}",
                                            selected = line.productId == prod.id && line.name == prod.name,
                                            onClick = {
                                                lineItems[idx] = line.copy(
                                                    productId = prod.id,
                                                    name = prod.name,
                                                    unit = prod.unit
                                                )
                                            }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SelavuTextField(
                                        value = line.qtyText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                lineItems[idx] = line.copy(qtyText = input)
                                            }
                                        },
                                        label = "Qty (${line.unit})",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    SelavuTextField(
                                        value = line.unitPriceText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                lineItems[idx] = line.copy(unitPriceText = input)
                                            }
                                        },
                                        label = "Price ₹ / ${line.unit}",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${line.qtyText.ifBlank { "0" }} ${line.unit} × ₹${line.unitPriceText.ifBlank { "0" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "= ${IndianFormatters.formatInr(lineTotal, 2)}",
                                        style = SelavuTheme.textStyles.numericBody,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (computedLinesTotal > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Items total (${lineItems.size} lines)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = IndianFormatters.formatInr(computedLinesTotal, 2),
                                    style = SelavuTheme.textStyles.numericTitle,
                                    color = colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        SelavuTextField(
                            value = amount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                            label = "Total payment amount (₹)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("project_payment_amount_input")
                        )
                        Button(
                            onClick = {
                                val mappedItems = lineItems.mapNotNull { l ->
                                    val q = l.qtyText.toDoubleOrNull() ?: 0.0
                                    val p = l.unitPriceText.toDoubleOrNull() ?: 0.0
                                    if (q > 0.0 && p >= 0.0) {
                                        ProjectPaymentItem(
                                            productId = l.productId,
                                            name = l.name,
                                            unit = l.unit,
                                            qty = q,
                                            unitPrice = p,
                                            totalPrice = q * p
                                        )
                                    } else null
                                }
                                val amt = amount.toDoubleOrNull() ?: computedLinesTotal.takeIf { it > 0.0 }
                                if (amt == null || amt <= 0.0) {
                                    onShowToast("Enter valid amount or item quantities")
                                } else {
                                    val fallbackTitle = title.trim().ifBlank {
                                        if (mappedItems.isNotEmpty()) {
                                            mappedItems.joinToString(", ") { it.name }.take(80)
                                        } else {
                                            "Stage payment"
                                        }
                                    }
                                    onAddProjectPayment(
                                        project,
                                        phaseIdx,
                                        fallbackTitle,
                                        amt,
                                        vendor.trim().ifBlank { "—" },
                                        mappedItems,
                                        if (catalogDirty || project.productCatalog.isEmpty()) catalog.toList() else null
                                    )
                                    onDismiss()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_project_payment_btn")
                        ) { Text("Save payment") }
                    }
                }

                is ActiveSheet.CategoryEdit -> {
                    val existing = activeSheet.category
                    val isEdit = existing != null
                    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
                    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "tag") }
                    var colorIdx by remember { mutableIntStateOf(existing?.colorIndex ?: 1) }
                    var subsText by remember { mutableStateOf(existing?.subcategories?.joinToString(", ").orEmpty()) }
                    var hiddenFromWorkspace by remember { mutableStateOf(existing?.hiddenFromWorkspace ?: false) }
                    var confirmDelete by remember { mutableStateOf(false) }

                    Text(if (isEdit) "Edit category" else strings.newCategory, style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Category name",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("car", "house", "bag", "bolt", "tv", "heart", "building", "fork", "rupee", "tag").forEach { k ->
                            val selected = iconKey == k
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant)
                                    .border(1.dp, if (selected) colorScheme.primary else colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                    .clickable { iconKey = k },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconForKey(k), contentDescription = k, tint = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("Colour", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (1..9).forEach { idx ->
                            val selected = colorIdx == idx
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(extendedColors.categoryColor(idx))
                                    .then(
                                        if (selected) Modifier.border(3.dp, colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { colorIdx = idx }
                            )
                        }
                    }
                    SelavuTextField(
                        value = subsText,
                        onValueChange = { subsText = it },
                        label = "Sub-categories (comma separated)",
                        placeholder = "e.g. Tuition, Books, Uniform",
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Hide category from Family Workspace", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Expenses in this category stay private to your Individual account",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hiddenFromWorkspace,
                            onCheckedChange = { hiddenFromWorkspace = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                            modifier = Modifier.testTag("category_hidden_workspace_switch")
                        )
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                onShowToast("Enter category name")
                            } else {
                                val subs = subsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val cat = (existing ?: Category(sortOrder = categories.size)).copy(
                                    name = name.trim(),
                                    iconKey = iconKey,
                                    colorIndex = colorIdx,
                                    subcategories = subs,
                                    hiddenFromWorkspace = hiddenFromWorkspace
                                )
                                onSaveCategory(cat)
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Save category") }

                    if (isEdit && existing != null) {
                        val usedCount = expenses.count { it.categoryId == existing.id }
                        OutlinedButton(
                            onClick = {
                                if (usedCount > 0) {
                                    onShowToast("Still used by $usedCount entries. Move them first.")
                                } else if (!confirmDelete) {
                                    confirmDelete = true
                                } else {
                                    onDeleteCategory(existing.id)
                                    onDismiss()
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text(
                                when {
                                    usedCount > 0 -> "Used by $usedCount entries (cannot delete)"
                                    confirmDelete -> "Tap again to delete category"
                                    else -> "Delete category"
                                }
                            )
                        }
                    }
                }

                is ActiveSheet.ExpenseDetail -> {
                    val exp = activeSheet.expense
                    val cat = categories.firstOrNull { it.id == exp.categoryId }
                    val isRetainedLocked = (exp.vehicleId != null && exp.vehicleId in retainedVehicleIds) ||
                        (exp.projectId != null && exp.projectId in retainedProjectIds)
                    var isPrivateLocal by remember(exp.id) { mutableStateOf(exp.isPrivate) }
                    var confirmDelete by remember(exp.id) { mutableStateOf(false) }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryTile(iconKey = cat?.iconKey ?: "tag", colorIndex = cat?.colorIndex ?: 9, size = 46.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.headlineMedium)
                            Text("${cat?.name ?: "Other"} · ${exp.subcategory}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isRetainedLocked) {
                        StatusPill(
                            text = "Past workspace contribution · Read-only (delete only)",
                            status = ServiceStatus.DUE_SOON
                        )
                    }
                    Text(
                        text = IndianFormatters.formatInr(exp.amount),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "${IndianFormatters.formatDate(exp.dateEpochMs)} · Paid via ${exp.paymentMode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    val linkedVehicle = exp.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
                    val linkedProject = exp.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }
                    if (linkedVehicle != null || linkedProject != null || exp.odometerReading != null || exp.fuelPricePerLitre != null || exp.fuelLitres != null) {
                        SelavuDivider()
                        Text("Vehicle, Project & Fuel Details", style = MaterialTheme.typography.titleMedium)
                        linkedVehicle?.let { DetailKv("Vehicle", "${it.name} (${it.plate})") }
                        linkedProject?.let { DetailKv("Project", it.name) }
                        exp.odometerReading?.let { DetailKv("Odometer reading", "${IndianFormatters.formatIndianInteger(it.toLong())} km") }
                        exp.fuelPricePerLitre?.let { DetailKv("Per litre cost", "${IndianFormatters.formatInr(it, 2)} / L") }
                        exp.fuelLitres?.let { DetailKv("Total litres bought", "${if (it % 1.0 == 0.0) it.toInt() else "%.2f".format(it)} L") }
                    }

                    if (inWorkspace && !isRetainedLocked) {
                        SelavuDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Hide from Family Workspace", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (cat?.hiddenFromWorkspace == true) {
                                        "Category is already hidden from Family Workspace"
                                    } else if (isPrivateLocal) {
                                        "Private — excluded from shared Family Workspace"
                                    } else {
                                        "Shared with members of your Family Workspace"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPrivateLocal,
                                onCheckedChange = {
                                    val prev = isPrivateLocal
                                    isPrivateLocal = !prev
                                    onToggleExpensePrivacy(exp.copy(isPrivate = prev))
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("expense_detail_private_switch")
                            )
                        }
                    }

                    if (!exp.gstin.isNullOrBlank() || !exp.billNumber.isNullOrBlank() || (exp.cgst ?: 0.0) > 0.0 || (exp.igst ?: 0.0) > 0.0) {
                        SelavuDivider()
                        Text("GST & Invoice Details", style = MaterialTheme.typography.titleMedium)
                        if (!exp.billNumber.isNullOrBlank()) Text("Bill No: ${exp.billNumber}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        if (!exp.gstin.isNullOrBlank()) Text("GSTIN: ${exp.gstin}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        exp.taxableValue?.let { DetailKv("Taxable value", IndianFormatters.formatInr(it, 2)) }
                        exp.cgst?.takeIf { it > 0 }?.let { DetailKv("CGST", IndianFormatters.formatInr(it, 2)) }
                        exp.sgst?.takeIf { it > 0 }?.let { DetailKv("SGST", IndianFormatters.formatInr(it, 2)) }
                        exp.igst?.takeIf { it > 0 }?.let { DetailKv("IGST", IndianFormatters.formatInr(it, 2)) }
                    }

                    if (!exp.items.isNullOrEmpty()) {
                        SelavuDivider()
                        Text("Itemized lines (${exp.items.size})", style = MaterialTheme.typography.titleMedium)
                        exp.items.forEach { line ->
                            val isSub = line.parentItemName.isNotBlank()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = if (isSub) 14.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSub) "↳ ${line.name}" else line.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSub) FontWeight.Normal else FontWeight.SemiBold
                                        ),
                                        color = if (isSub) colorScheme.onSurfaceVariant else colorScheme.onSurface
                                    )
                                    val hsn = if (line.hsn.isNotBlank()) " · HSN ${line.hsn}" else ""
                                    Text(
                                        text = "${line.qty} ${line.unit} @ ${IndianFormatters.formatInr(line.rate)}$hsn",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.outline
                                    )
                                }
                                Text(
                                    text = IndianFormatters.formatInr(line.amount, 2),
                                    style = SelavuTheme.textStyles.numericCaption,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }

                    SelavuDivider()
                    if (!isRetainedLocked) {
                        Button(
                            onClick = { onEditExpense(exp) },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("expense_detail_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit expense")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!confirmDelete) {
                                confirmDelete = true
                            } else {
                                onDeleteExpense(exp.id)
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("expense_detail_delete_button")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (confirmDelete) "Tap again to delete expense" else "Delete expense")
                    }
                }

                is ActiveSheet.SharedExpenseDetail -> {
                    val shared = activeSheet.sharedExpense
                    var confirmDelete by remember(shared.id) { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryTile(iconKey = shared.categoryIconKey, colorIndex = shared.categoryColorIndex, size = 46.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shared.title, style = MaterialTheme.typography.headlineMedium)
                            Text("${shared.categoryName} · ${shared.subcategory}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    if (shared.retainedForDeparture) {
                        StatusPill(
                            text = "Past workspace contribution · Read-only (delete only)",
                            status = ServiceStatus.DUE_SOON
                        )
                    }
                    Text(
                        text = IndianFormatters.formatInr(shared.amount),
                        style = MaterialTheme.typography.displayMedium,
                        color = colorScheme.onSurface
                    )
                    DetailKv("Shared by", shared.ownerName)
                    DetailKv("Date", IndianFormatters.formatDate(shared.dateEpochMs))
                    DetailKv("Payment mode", shared.paymentMode)
                    if (shared.odometerReading != null || shared.fuelPricePerLitre != null || shared.fuelLitres != null) {
                        SelavuDivider()
                        Text("Fuel & Odometer Details", style = MaterialTheme.typography.titleMedium)
                        shared.odometerReading?.let { DetailKv("Odometer", "${IndianFormatters.formatIndianInteger(it.toLong())} km") }
                        shared.fuelPricePerLitre?.let { DetailKv("Per litre cost", "${IndianFormatters.formatInr(it, 2)} / L") }
                        shared.fuelLitres?.let { DetailKv("Litres bought", "${if (it % 1.0 == 0.0) it.toInt() else "%.2f".format(it)} L") }
                    }
                    if (shared.retainedForDeparture && shared.ownerUid == currentUid) {
                        SelavuDivider()
                        OutlinedButton(
                            onClick = {
                                if (!confirmDelete) {
                                    confirmDelete = true
                                } else {
                                    onDeleteRetainedExpense(shared)
                                    onDismiss()
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("shared_expense_detail_delete_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (confirmDelete) "Tap again to delete contribution" else "Delete contribution")
                        }
                    }
                }

                is ActiveSheet.CategoryDrilldown -> {
                    val cat = activeSheet.category
                    val list = expenses.filter { it.categoryId == cat.id }.sortedByDescending { it.dateEpochMs }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CategoryTile(iconKey = cat.iconKey, colorIndex = cat.colorIndex)
                        Column {
                            Text(cat.name, style = MaterialTheme.typography.headlineMedium)
                            Text("${list.size} entries · ${IndianFormatters.formatInr(list.sumOf { it.amount })}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    SelavuDivider()
                    if (list.isEmpty()) {
                        Text("No expenses recorded in this category yet.", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                    } else {
                        list.take(12).forEach { exp ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${exp.subcategory} · ${IndianFormatters.formatDate(exp.dateEpochMs)}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                }
                                Text(IndianFormatters.formatInr(exp.amount), style = SelavuTheme.textStyles.numericBody)
                            }
                        }
                    }
                }

                is ActiveSheet.EditScannedItem -> {
                    val item = activeSheet.item
                    var name by remember { mutableStateOf(item.name) }
                    var qty by remember { mutableStateOf(item.qty.toString()) }
                    var rate by remember { mutableStateOf(item.rate.toString()) }
                    var discount by remember { mutableStateOf(item.discount.toString()) }
                    var hsn by remember { mutableStateOf(item.hsn) }
                    var gst by remember { mutableStateOf(item.gstRate.toInt().toString()) }
                    var mappedSvcId by remember { mutableStateOf(item.mappedServiceItemId) }

                    Text("Edit scanned item", style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Description",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelavuTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = "Qty",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        SelavuTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = "Rate ₹",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        SelavuTextField(
                            value = discount,
                            onValueChange = { discount = it },
                            label = "Disc ₹",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelavuTextField(
                            value = hsn,
                            onValueChange = { hsn = it },
                            label = "HSN / SAC",
                            modifier = Modifier.weight(1f)
                        )
                        SelavuTextField(
                            value = gst,
                            onValueChange = { if (it.all(Char::isDigit)) gst = it },
                            label = "GST %",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (activeSheet.vehicle != null) {
                        Text("Link to vehicle service tracker item", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelavuFilterChip("None", mappedSvcId == null, { mappedSvcId = null })
                            activeSheet.vehicle.items.forEach { svc ->
                                SelavuFilterChip(svc.name, mappedSvcId == svc.id, { mappedSvcId = svc.id })
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onUpdateScannedItem(
                                item.id,
                                item.copy(
                                    name = name.trim().ifBlank { item.name },
                                    qty = qty.toDoubleOrNull() ?: item.qty,
                                    rate = rate.toDoubleOrNull() ?: item.rate,
                                    discount = discount.toDoubleOrNull() ?: 0.0,
                                    hsn = hsn.trim(),
                                    gstRate = gst.toDoubleOrNull() ?: item.gstRate,
                                    mappedServiceItemId = mappedSvcId,
                                    lowConfidenceNote = null
                                )
                            )
                            onDismiss()
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Update line & recompute GST") }
                }

                is ActiveSheet.EditMonthlyBudget -> {
                    var budgetText by remember { mutableStateOf(activeSheet.currentBudget.toInt().toString()) }
                    Text(strings.budgetLabel, style = MaterialTheme.typography.headlineMedium)
                    SelavuTextField(
                        value = budgetText,
                        onValueChange = { if (it.all(Char::isDigit)) budgetText = it },
                        label = "Monthly target (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val valNum = budgetText.toDoubleOrNull()
                            if (valNum == null || valNum < 1000.0) {
                                onShowToast("Enter a budget of at least ₹1,000")
                            } else {
                                onUpdateBudget(valNum)
                                onDismiss()
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Save budget") }
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        if (badge != null) {
            StatusPill(badge, isAccent = true)
        }
    }
}

@Composable
private fun DetailKv(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        Text(value, style = SelavuTheme.textStyles.numericCaption, color = colorScheme.onSurface)
    }
}
