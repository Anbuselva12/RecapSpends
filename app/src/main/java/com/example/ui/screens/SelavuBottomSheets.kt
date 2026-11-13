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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.SampleDataSeeder
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceItem
import com.example.data.model.Vehicle
import com.example.ui.components.CategoryTile
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuSegmentedControl
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
    data object AddProject : ActiveSheet
    data class AddProjectPayment(val projectId: String) : ActiveSheet
    data class CategoryEdit(val category: Category?) : ActiveSheet
    data class ExpenseDetail(val expense: Expense) : ActiveSheet
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
    onDismiss: () -> Unit,
    onNavigateScan: () -> Unit,
    onNavigateAddExpense: (catId: String?, sub: String?) -> Unit,
    onOpenAddVehicle: () -> Unit,
    onOpenNewProject: () -> Unit,
    onOpenNewReminder: () -> Unit,
    onUpdateOdometer: (Vehicle, Int) -> Unit,
    onMarkServiceDone: (Vehicle, String) -> Unit,
    onUpdateServiceInterval: (Vehicle, String, Int, Int) -> Unit,
    onDeleteServiceItem: (Vehicle, String) -> Unit,
    onAddServiceItem: (Vehicle, ServiceItem) -> Unit,
    onCreateVehicle: (String, String, String, Int, Double) -> Unit,
    onCreateBill: (Bill) -> Unit,
    onCreateProject: (String, Double, String) -> Unit,
    onAddProjectPayment: (Project, Int, String, Double, String) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onUpdateScannedItem: (Int, ScannedBillItem) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onShowToast: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = SelavuTheme.colors
    val strings = selavuStrings()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.ink
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
                    Text("Quick add", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAddVehicle,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addVehicle, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenNewProject,
                            shape = RoundedCornerShape(12.dp),
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
                            color = colors.ink2
                        )
                        OutlinedTextField(
                            value = odoText,
                            onValueChange = { if (it.all(Char::isDigit)) odoText = it },
                            label = { Text("New odometer reading (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
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
                        Text(state.nextDueLabel, style = MaterialTheme.typography.bodyMedium, color = colors.ink2)

                        Button(
                            onClick = {
                                onMarkServiceDone(vehicle, item.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mark_service_done_btn")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark done today at ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km")
                        }

                        SelavuDivider()
                        Text("Edit service interval", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = kmText,
                                onValueChange = { if (it.all(Char::isDigit)) kmText = it },
                                label = { Text("Every km (0 = date only)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = moText,
                                onValueChange = { if (it.all(Char::isDigit)) moText = it },
                                label = { Text("Every months") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
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
                                shape = RoundedCornerShape(12.dp),
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
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.bad),
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
                            Text("Suggested extras for ${vehicle.shortName}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
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
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Item name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = kmText,
                                onValueChange = { if (it.all(Char::isDigit)) kmText = it },
                                label = { Text("Km (0 = date only)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = moText,
                                onValueChange = { if (it.all(Char::isDigit)) moText = it },
                                label = { Text("Months") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
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

                    Text(strings.addVehicle, style = MaterialTheme.typography.headlineMedium)
                    SelavuSegmentedControl(
                        options = listOf("bike" to "Bike / Scooter (8 items)", "car" to "Car (6 items)"),
                        selectedKey = type,
                        onSelect = { type = it }
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Make & model") },
                        placeholder = { Text("e.g. Honda Activa 6G, Hyundai Creta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_vehicle_name")
                    )
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        label = { Text("Registration plate") },
                        placeholder = { Text("TN 09 CD 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = odo,
                            onValueChange = { if (it.all(Char::isDigit)) odo = it },
                            label = { Text("Odometer (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mileage,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d?$"))) mileage = it },
                            label = { Text("Avg km/l") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
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
                                    mileage.toDoubleOrNull() ?: if (type == "car") 16.0 else 40.0
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Bill title") },
                        placeholder = { Text("e.g. BESCOM electricity, Broadband") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_bill_name")
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
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
                        categories.take(6).forEach { cat ->
                            SelavuFilterChip(cat.name.substringBefore(" & "), catId == cat.id, { catId = cat.id })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Autopay enabled", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autopay, onCheckedChange = { autopay = it }, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent))
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
                                        autopay = autopay
                                    )
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_bill_btn")
                    ) { Text("Save bill reminder") }
                }

                is ActiveSheet.AddProject -> {
                    var name by remember { mutableStateOf("") }
                    var budget by remember { mutableStateOf("") }
                    var template by remember { mutableStateOf("House build") }

                    Text(strings.newProject, style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project name") },
                        placeholder = { Text("e.g. Madurai House, Bakery Launch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { if (it.all(Char::isDigit)) budget = it },
                        label = { Text("Total budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Phase template", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleDataSeeder.projectTemplates.keys.forEach { t ->
                            SelavuFilterChip(t, template == t, { template = t })
                        }
                    }
                    Button(
                        onClick = {
                            val b = budget.toDoubleOrNull()
                            if (name.isBlank() || b == null || b <= 0.0) {
                                onShowToast("Enter project name and budget")
                            } else {
                                onCreateProject(name.trim(), b, template)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Create project") }
                }

                is ActiveSheet.AddProjectPayment -> {
                    val project = projects.firstOrNull { it.id == activeSheet.projectId }
                    if (project != null) {
                        var phaseIdx by remember { mutableIntStateOf(0) }
                        var title by remember { mutableStateOf("") }
                        var amount by remember { mutableStateOf("") }
                        var vendor by remember { mutableStateOf(project.vendors.firstOrNull()?.name.orEmpty()) }

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
                        OutlinedTextField(
                            value = vendor,
                            onValueChange = { vendor = it },
                            label = { Text("Vendor name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Payment description") },
                            placeholder = { Text("e.g. River sand 2 loads") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                            label = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val amt = amount.toDoubleOrNull()
                                if (amt == null || amt <= 0.0) {
                                    onShowToast("Enter valid amount")
                                } else {
                                    onAddProjectPayment(
                                        project,
                                        phaseIdx,
                                        title.trim().ifBlank { "Stage payment" },
                                        amt,
                                        vendor.trim().ifBlank { "—" }
                                    )
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
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
                    var confirmDelete by remember { mutableStateOf(false) }

                    Text(if (isEdit) "Edit category" else strings.newCategory, style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("car", "house", "bag", "bolt", "tv", "heart", "building", "fork", "rupee", "tag").forEach { k ->
                            val selected = iconKey == k
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) colors.accentSoft else colors.sunk)
                                    .border(1.dp, if (selected) colors.accent else colors.line, RoundedCornerShape(12.dp))
                                    .clickable { iconKey = k },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconForKey(k), contentDescription = k, tint = if (selected) colors.accentInk else colors.ink2)
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
                                    .background(colors.categoryColor(idx))
                                    .then(
                                        if (selected) Modifier.border(3.dp, colors.ink, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { colorIdx = idx }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subsText,
                        onValueChange = { subsText = it },
                        label = { Text("Sub-categories (comma separated)") },
                        placeholder = { Text("e.g. Tuition, Books, Uniform") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                    subcategories = subs
                                )
                                onSaveCategory(cat)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.bad),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CategoryTile(iconKey = cat?.iconKey ?: "tag", colorIndex = cat?.colorIndex ?: 9, size = 46.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, style = MaterialTheme.typography.headlineMedium)
                            Text("${cat?.name ?: "Other"} · ${exp.subcategory}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                        }
                    }
                    Text(
                        text = IndianFormatters.formatInr(exp.amount),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                        color = colors.ink
                    )
                    Text(
                        text = "${IndianFormatters.formatDate(exp.dateEpochMs)} · Paid via ${exp.paymentMode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink2
                    )

                    if (!exp.gstin.isNullOrBlank() || !exp.billNumber.isNullOrBlank() || (exp.cgst ?: 0.0) > 0.0 || (exp.igst ?: 0.0) > 0.0) {
                        SelavuDivider()
                        Text("GST & Invoice Details", style = MaterialTheme.typography.titleMedium)
                        if (!exp.billNumber.isNullOrBlank()) Text("Bill No: ${exp.billNumber}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                        if (!exp.gstin.isNullOrBlank()) Text("GSTIN: ${exp.gstin}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
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
                                        color = if (isSub) colors.ink2 else colors.ink
                                    )
                                    val hsn = if (line.hsn.isNotBlank()) " · HSN ${line.hsn}" else ""
                                    Text(
                                        text = "${line.qty} ${line.unit} @ ${IndianFormatters.formatInr(line.rate)}$hsn",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.ink3
                                    )
                                }
                                Text(
                                    text = IndianFormatters.formatInr(line.amount, 2),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
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
                            Text("${list.size} entries · ${IndianFormatters.formatInr(list.sumOf { it.amount })}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                        }
                    }
                    SelavuDivider()
                    if (list.isEmpty()) {
                        Text("No expenses recorded in this category yet.", style = MaterialTheme.typography.bodyMedium, color = colors.ink2)
                    } else {
                        list.take(12).forEach { exp ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${exp.subcategory} · ${IndianFormatters.formatDate(exp.dateEpochMs)}", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                                }
                                Text(IndianFormatters.formatInr(exp.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
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
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rate,
                            onValueChange = { rate = it },
                            label = { Text("Rate ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { discount = it },
                            label = { Text("Disc ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hsn, onValueChange = { hsn = it }, label = { Text("HSN / SAC") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = gst,
                            onValueChange = { if (it.all(Char::isDigit)) gst = it },
                            label = { Text("GST %") },
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Update line & recompute GST") }
                }

                is ActiveSheet.EditMonthlyBudget -> {
                    var budgetText by remember { mutableStateOf(activeSheet.currentBudget.toInt().toString()) }
                    Text(strings.budgetLabel, style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { if (it.all(Char::isDigit)) budgetText = it },
                        label = { Text("Monthly target (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Save budget") }
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val colors = SelavuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.sunk)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.ink2)
        }
        if (badge != null) {
            StatusPill(badge, isAccent = true)
        }
    }
}

@Composable
private fun DetailKv(label: String, value: String) {
    val colors = SelavuTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.ink2)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colors.ink)
    }
}
