package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.Project
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceStatus
import com.example.data.model.Vehicle
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTextField
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.ui.viewmodel.ScanUiState
import com.example.util.IndianFormatters
import com.example.util.selavuStrings
import java.util.UUID

private data class DraftLineInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val qtyText: String = "1",
    val priceText: String = ""
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    categories: List<Category>,
    vehicles: List<Vehicle>,
    projects: List<Project>,
    initialCategoryId: String?,
    initialSubcategory: String?,
    initialVehicleId: String?,
    editingExpense: Expense? = null,
    isSaving: Boolean,
    inWorkspace: Boolean = false,
    onBack: () -> Unit,
    onSwitchToScan: () -> Unit,
    onShowToast: (String) -> Unit,
    onRestoreStarterCategories: () -> Unit = {},
    onSave: (
        expense: Expense,
        vehicleToReset: Vehicle?,
        odometerReading: Int?,
        serviceItemIdsDone: List<String>,
        projectToCharge: Project?
    ) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }
    val isEditMode = editingExpense != null
    val editKey = editingExpense?.id ?: "__new__"

    fun formatCleanDecimal(value: Double?): String {
        if (value == null || value <= 0.0) return ""
        return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
    }

    var entryType by rememberSaveable(editKey) { mutableStateOf(editingExpense?.entryType ?: "expense") }
    val effectiveCategories = remember(categories, entryType) {
        if (entryType == "income" && categories.none { it.id == "inc" }) {
            categories + Category(
                id = "inc",
                name = "Income",
                iconKey = "rupee",
                colorIndex = 4,
                subcategories = listOf("Salary", "Business / Freelance", "Refund & cashback", "Interest & dividend", "Rental & other"),
                sortOrder = 99
            )
        } else {
            categories
        }
    }

    val defaultCatId = editingExpense?.categoryId?.takeIf { id -> effectiveCategories.any { it.id == id } }
        ?: initialCategoryId?.takeIf { id -> effectiveCategories.any { it.id == id } }
        ?: effectiveCategories.firstOrNull()?.id ?: "groc"
    var selectedCatId by rememberSaveable(editKey) { mutableStateOf(defaultCatId) }
    val selectedCategory = effectiveCategories.firstOrNull { it.id == selectedCatId } ?: effectiveCategories.firstOrNull()

    var selectedSub by rememberSaveable(editKey) {
        mutableStateOf(
            editingExpense?.subcategory
                ?: initialSubcategory?.takeIf { it.isNotBlank() }
                ?: selectedCategory?.subcategories?.firstOrNull().orEmpty()
        )
    }

    LaunchedEffect(effectiveCategories, editKey) {
        if (effectiveCategories.isNotEmpty() && effectiveCategories.none { it.id == selectedCatId }) {
            val editCat = editingExpense?.categoryId
            selectedCatId = if (editCat != null && effectiveCategories.any { it.id == editCat }) {
                editCat
            } else {
                effectiveCategories.first().id
            }
        }
    }

    LaunchedEffect(selectedCatId, effectiveCategories) {
        val subs = effectiveCategories.firstOrNull { it.id == selectedCatId }?.subcategories.orEmpty()
        if (subs.isNotEmpty() && selectedSub !in subs &&
            (editingExpense == null || selectedCatId != editingExpense.categoryId || selectedSub.isBlank())
        ) {
            selectedSub = subs.first()
        }
    }

    var amountText by rememberSaveable(editKey) { mutableStateOf(formatCleanDecimal(editingExpense?.amount)) }
    var titleText by rememberSaveable(editKey) { mutableStateOf(editingExpense?.title.orEmpty()) }
    var dateEpochMs by rememberSaveable(editKey) { mutableStateOf(editingExpense?.dateEpochMs ?: nowMs) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMode by rememberSaveable(editKey) { mutableStateOf(editingExpense?.paymentMode ?: "UPI") }
    var selectedProjectId by rememberSaveable(editKey) { mutableStateOf(editingExpense?.projectId) }
    var isPrivate by rememberSaveable(editKey) { mutableStateOf(editingExpense?.isPrivate ?: false) }

    val defaultVehicleId = if (editingExpense != null) {
        editingExpense.vehicleId
    } else {
        initialVehicleId?.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
    }
    var selectedVehicleId by rememberSaveable(editKey) { mutableStateOf(defaultVehicleId) }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    var odometerText by rememberSaveable(editKey, selectedVehicleId) {
        mutableStateOf(
            editingExpense?.odometerReading?.toString()
                ?: selectedVehicle?.odometer?.toString().orEmpty()
        )
    }
    var fuelOdometerText by rememberSaveable(editKey, selectedVehicleId) {
        mutableStateOf(
            if (editingExpense != null && editingExpense.vehicleId == selectedVehicleId) {
                editingExpense.odometerReading?.toString().orEmpty()
            } else ""
        )
    }
    var fuelPricePerLitreText by rememberSaveable(editKey) {
        mutableStateOf(formatCleanDecimal(editingExpense?.fuelPricePerLitre))
    }
    var fuelLitresText by rememberSaveable(editKey) {
        mutableStateOf(formatCleanDecimal(editingExpense?.fuelLitres))
    }
    var lastAutoFuelAmount by rememberSaveable(editKey) { mutableStateOf("") }
    var checkedServiceIds by rememberSaveable(editKey, selectedVehicleId) { mutableStateOf(setOf<String>()) }

    var showItemLines by rememberSaveable(editKey) {
        mutableStateOf(!editingExpense?.items.isNullOrEmpty())
    }
    val draftLines = remember(editKey) {
        mutableStateListOf<DraftLineInput>().apply {
            editingExpense?.items?.forEach { item ->
                add(
                    DraftLineInput(
                        name = item.name,
                        qtyText = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString(),
                        priceText = formatCleanDecimal(item.rate)
                    )
                )
            }
        }
    }

    val linesTotal = draftLines.sumOf { line ->
        val q = line.qtyText.toDoubleOrNull() ?: 0.0
        val p = line.priceText.toDoubleOrNull() ?: 0.0
        q * p
    }
    LaunchedEffect(linesTotal, showItemLines, draftLines.size) {
        if (showItemLines && draftLines.isNotEmpty() && linesTotal > 0.0) {
            amountText = if (linesTotal % 1.0 == 0.0) linesTotal.toInt().toString() else "%.2f".format(linesTotal)
        }
    }

    val isVehicleCategory = selectedCatId == "veh" && vehicles.isNotEmpty() && entryType != "income"
    val isVehicleService = isVehicleCategory && selectedSub.equals("Service", ignoreCase = true)
    val isVehicleFuel = isVehicleCategory && selectedSub.equals("Fuel", ignoreCase = true)

    LaunchedEffect(fuelPricePerLitreText, fuelLitresText, isVehicleFuel, selectedVehicleId) {
        if (isVehicleFuel && selectedVehicle != null && (!showItemLines || draftLines.isEmpty())) {
            val rate = fuelPricePerLitreText.toDoubleOrNull()
            val litres = fuelLitresText.toDoubleOrNull()
            if (rate != null && rate > 0.0 && litres != null && litres > 0.0) {
                val computed = rate * litres
                val formatted = if (computed % 1.0 == 0.0) computed.toInt().toString() else "%.2f".format(computed)
                if (amountText.isBlank() || amountText == lastAutoFuelAmount) {
                    amountText = formatted
                    lastAutoFuelAmount = formatted
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateEpochMs = it }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
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
                SelavuTopNavBar(
                    title = when {
                        isEditMode && entryType == "income" -> "Edit income"
                        isEditMode -> "Edit expense"
                        entryType == "income" -> strings.addIncome
                        else -> strings.addExpense
                    },
                    onBack = onBack,
                    trailing = {
                        if (!isEditMode && entryType != "income") {
                            SelavuFilterChip(
                                label = strings.scanInstead,
                                selected = true,
                                icon = Icons.Filled.DocumentScanner,
                                onClick = onSwitchToScan,
                                modifier = Modifier.testTag("add_switch_to_scan")
                            )
                        }
                    }
                )
            }

            item {
                SelavuSegmentedControl(
                    options = listOf("expense" to "Expense", "income" to "Income"),
                    selectedKey = entryType,
                    onSelect = { next ->
                        entryType = next
                        if (next == "income") {
                            val inc = effectiveCategories.firstOrNull { it.id == "inc" }
                            if (inc != null) {
                                selectedCatId = inc.id
                                selectedSub = inc.subcategories.firstOrNull().orEmpty()
                            }
                        } else if (selectedCatId == "inc") {
                            val firstNonInc = effectiveCategories.firstOrNull { it.id != "inc" }
                            if (firstNonInc != null) {
                                selectedCatId = firstNonInc.id
                                selectedSub = firstNonInc.subcategories.firstOrNull().orEmpty()
                            }
                        }
                    },
                    modifier = Modifier.testTag("entry_type_selector")
                )
            }

            // Big amount input
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.displayLarge,
                                color = colorScheme.onPrimaryContainer
                            )
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        amountText = input
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "0",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = colorScheme.outline.copy(alpha = 0.45f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.displayLarge.copy(
                                    color = colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outlineVariant,
                                    cursorColor = colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_amount_input")
                            )
                        }

                        SelavuTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = if (entryType == "income") "Income source" else strings.whatWasItFor,
                            placeholder = if (entryType == "income") {
                                "e.g. Monthly salary, Client invoice, Cashback"
                            } else {
                                "e.g. Reliance Fresh, Sri Vel Garage, TNEB"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_title_input")
                        )
                    }
                }
            }

            // Category & sub-category chips
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.category, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                        if (effectiveCategories.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "No categories yet after clearing data. Restore default Indian categories or log under General.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onRestoreStarterCategories,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("add_restore_categories_button")
                                ) {
                                    Text("Restore starter categories")
                                }
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                effectiveCategories.forEach { cat ->
                                    SelavuFilterChip(
                                        label = cat.name.substringBefore(" & "),
                                        selected = selectedCatId == cat.id,
                                        onClick = { selectedCatId = cat.id },
                                        modifier = Modifier.testTag("cat_chip_${cat.id}")
                                    )
                                }
                            }
                        }

                        val subs = selectedCategory?.subcategories.orEmpty()
                        if (subs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(strings.subCategory, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subs.forEach { sub ->
                                    SelavuFilterChip(
                                        label = sub,
                                        selected = selectedSub == sub,
                                        onClick = { selectedSub = sub },
                                        modifier = Modifier.testTag("sub_chip_$sub")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Vehicle linking for all vehicle expenses + Service tracker & Fuel optional inputs
            if (isVehicleCategory) {
                item {
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isVehicleService) "Vehicle service tracker link" else "Link to vehicle",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelavuFilterChip(
                                    label = "None",
                                    selected = selectedVehicleId == null,
                                    onClick = {
                                        selectedVehicleId = null
                                        checkedServiceIds = emptySet()
                                    },
                                    modifier = Modifier.testTag("veh_chip_none")
                                )
                                vehicles.forEach { v ->
                                    SelavuFilterChip(
                                        label = "${v.shortName} (${IndianFormatters.formatIndianInteger(v.odometer.toLong())} km)",
                                        selected = selectedVehicleId == v.id,
                                        onClick = {
                                            selectedVehicleId = v.id
                                            odometerText = v.odometer.toString()
                                            checkedServiceIds = emptySet()
                                        },
                                        modifier = Modifier.testTag("veh_chip_${v.id}")
                                    )
                                }
                            }

                            if (selectedVehicle != null && isVehicleService) {
                                SelavuTextField(
                                    value = odometerText,
                                    onValueChange = { if (it.all(Char::isDigit)) odometerText = it },
                                    label = "Odometer reading now (km)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("service_odometer_input")
                                )

                                Text(
                                    text = "Tap items completed today (resets next-due tracker):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedVehicle.items.forEach { svc ->
                                        val isChecked = svc.id in checkedServiceIds
                                        SelavuFilterChip(
                                            label = svc.name,
                                            selected = isChecked,
                                            icon = if (isChecked) Icons.Filled.Check else null,
                                            onClick = {
                                                checkedServiceIds = if (isChecked) {
                                                    checkedServiceIds - svc.id
                                                } else {
                                                    checkedServiceIds + svc.id
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedVehicle != null && isVehicleFuel) {
                                SelavuDivider()
                                Text(
                                    text = "Fuel & odometer details (optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurface
                                )
                                SelavuTextField(
                                    value = fuelOdometerText,
                                    onValueChange = { if (it.all(Char::isDigit)) fuelOdometerText = it },
                                    label = "Odometer reading (km · optional)",
                                    placeholder = "Current ${IndianFormatters.formatIndianInteger(selectedVehicle.odometer.toLong())} km",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fuel_odometer_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SelavuTextField(
                                        value = fuelPricePerLitreText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                fuelPricePerLitreText = input
                                            }
                                        },
                                        label = "Per litre ₹ (opt.)",
                                        placeholder = "e.g. 102.50",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("fuel_price_per_litre_input")
                                    )
                                    SelavuTextField(
                                        value = fuelLitresText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                fuelLitresText = input
                                            }
                                        },
                                        label = "Total litres (opt.)",
                                        placeholder = "e.g. 8.5",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("fuel_litres_input")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date, Payment Mode & Optional Project
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.date, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelavuFilterChip(
                                label = "Today (${IndianFormatters.formatShortDate(nowMs)})",
                                selected = IndianFormatters.sameDay(dateEpochMs, nowMs),
                                onClick = { dateEpochMs = nowMs }
                            )
                            val yestMs = IndianFormatters.addDays(nowMs, -1)
                            SelavuFilterChip(
                                label = "Yesterday",
                                selected = IndianFormatters.sameDay(dateEpochMs, yestMs),
                                onClick = { dateEpochMs = yestMs }
                            )
                            SelavuFilterChip(
                                label = IndianFormatters.formatDate(dateEpochMs),
                                selected = !IndianFormatters.sameDay(dateEpochMs, nowMs) && !IndianFormatters.sameDay(dateEpochMs, yestMs),
                                icon = Icons.Filled.CalendarMonth,
                                onClick = { showDatePicker = true }
                            )
                        }

                        SelavuDivider()

                        Text(
                            if (entryType == "income") "Received via" else strings.paidWith,
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("UPI", "Cash", "Card", "Bank").forEach { mode ->
                                SelavuFilterChip(
                                    label = if (mode == "Bank") "Bank transfer" else mode,
                                    selected = paymentMode == mode,
                                    onClick = { paymentMode = mode },
                                    modifier = Modifier.testTag("pay_mode_$mode")
                                )
                            }
                        }

                        if (projects.isNotEmpty()) {
                            SelavuDivider()
                            Text("${strings.project} (optional)", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelavuFilterChip(
                                    label = "None",
                                    selected = selectedProjectId == null,
                                    onClick = { selectedProjectId = null }
                                )
                                projects.forEach { proj ->
                                    SelavuFilterChip(
                                        label = proj.name,
                                        selected = selectedProjectId == proj.id,
                                        onClick = { selectedProjectId = proj.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Optional item-level lines
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showItemLines = !showItemLines
                                    if (showItemLines && draftLines.isEmpty()) {
                                        draftLines += DraftLineInput()
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.itemLevelDetails, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                                Text("Total auto-fills from line items", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (showItemLines) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle item lines",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }

                        if (showItemLines) {
                            draftLines.forEachIndexed { idx, line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SelavuTextField(
                                        value = line.name,
                                        onValueChange = { v -> draftLines[idx] = line.copy(name = v) },
                                        label = "Item",
                                        modifier = Modifier.weight(1.8f)
                                    )
                                    SelavuTextField(
                                        value = line.qtyText,
                                        onValueChange = { v -> draftLines[idx] = line.copy(qtyText = v) },
                                        label = "Qty",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    SelavuTextField(
                                        value = line.priceText,
                                        onValueChange = { v -> draftLines[idx] = line.copy(priceText = v) },
                                        label = "Rate ₹",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { draftLines.removeAt(idx) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove line", tint = colorScheme.error)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { draftLines += DraftLineInput() },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add item line")
                            }
                        }
                    }
                }
            }

            if (inWorkspace) {
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
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedCategory?.hiddenFromWorkspace == true) {
                                        "This entire category is already hidden from your workspace."
                                    } else {
                                        "Private entries (${if (entryType == "income") "income" else "expenses"}) stay only in your Individual account."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPrivate,
                                onCheckedChange = { isPrivate = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("add_expense_private_switch")
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            onShowToast("Enter a valid amount greater than ₹0")
                            return@Button
                        }
                        val validLines = if (showItemLines) {
                            draftLines.filter { it.name.isNotBlank() }.map { line ->
                                val q = line.qtyText.toDoubleOrNull() ?: 1.0
                                val r = line.priceText.toDoubleOrNull() ?: 0.0
                                ExpenseItem(
                                    name = line.name.trim(),
                                    qty = q,
                                    unit = "pcs",
                                    rate = r,
                                    amount = q * r
                                )
                            }
                        } else emptyList()

                        val finalTitle = titleText.trim().ifBlank {
                            when {
                                entryType == "income" -> selectedSub.ifBlank { "Income" }
                                isVehicleService && selectedVehicle != null -> "${selectedVehicle.shortName} service"
                                isVehicleFuel && selectedVehicle != null -> "${selectedVehicle.shortName} fuel"
                                else -> selectedSub.ifBlank { selectedCategory?.name ?: "Expense" }
                            }
                        }
                        val project = projects.firstOrNull { it.id == selectedProjectId }
                        val vehicle = if (isVehicleCategory) selectedVehicle else null
                        val odo = when {
                            isVehicleService && vehicle != null -> odometerText.toIntOrNull()
                            isVehicleFuel && vehicle != null -> fuelOdometerText.toIntOrNull()
                            else -> null
                        }
                        val fuelRate = if (isVehicleFuel && vehicle != null) {
                            fuelPricePerLitreText.toDoubleOrNull()?.takeIf { it > 0.0 }
                        } else null
                        val fuelQty = if (isVehicleFuel && vehicle != null) {
                            fuelLitresText.toDoubleOrNull()?.takeIf { it > 0.0 }
                        } else null

                        val baseExpense = editingExpense ?: Expense()
                        onSave(
                            baseExpense.copy(
                                title = finalTitle,
                                amount = amt,
                                dateEpochMs = dateEpochMs,
                                categoryId = selectedCatId,
                                subcategory = selectedSub,
                                paymentMode = paymentMode,
                                entryType = entryType,
                                projectId = project?.id,
                                vehicleId = vehicle?.id,
                                isPrivate = isPrivate,
                                items = validLines.takeIf { it.isNotEmpty() },
                                odometerReading = odo,
                                fuelPricePerLitre = fuelRate,
                                fuelLitres = fuelQty
                            ),
                            vehicle,
                            odo,
                            if (isVehicleService) checkedServiceIds.toList() else emptyList(),
                            project
                        )
                    },
                    enabled = !isSaving,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_expense_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = when {
                                isEditMode -> "Save changes"
                                entryType == "income" -> strings.saveIncome
                                else -> strings.saveExpense
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanBillScreen(
    scanState: ScanUiState,
    categories: List<Category>,
    vehicles: List<Vehicle>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onResetScan: () -> Unit,
    onScanSample: () -> Unit,
    onScanFromUri: (Uri) -> Unit,
    onScanBitmap: (Bitmap) -> Unit,
    onUpdateDraft: ((ScannedBillDraft) -> ScannedBillDraft) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onEditItem: (ScannedBillItem) -> Unit,
    onSaveBill: (ScannedBillDraft) -> Unit,
    onShowToast: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onScanFromUri(uri)
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onScanFromUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) onScanBitmap(bitmap)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            onShowToast("Camera permission denied. You can still pick from Gallery or PDF.")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (scanState) {
            is ScanUiState.Capture -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SelavuTopNavBar(title = strings.scanTitle, onBack = onBack)
                    }

                    // Framing Viewfinder Guide
                    item {
                        val viewfinderFg = colorScheme.onSecondary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(colorScheme.secondary)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val guideColor = viewfinderFg.copy(alpha = 0.72f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val corner = 28.dp.toPx()
                                val sw = 3.dp.toPx()
                                drawLine(guideColor, Offset(0f, 0f), Offset(corner, 0f), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(0f, 0f), Offset(0f, corner), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(size.width, 0f), Offset(size.width - corner, 0f), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(size.width, 0f), Offset(size.width, corner), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(0f, size.height), Offset(corner, size.height), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(0f, size.height), Offset(0f, size.height - corner), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(size.width, size.height), Offset(size.width - corner, size.height), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(size.width, size.height), Offset(size.width, size.height - corner), sw, StrokeCap.Round)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(viewfinderFg.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "AI Bill Scan",
                                        tint = viewfinderFg,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Frame any Indian receipt or GST bill",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = viewfinderFg
                                )
                                Text(
                                    text = "Shop receipts · GST invoices · EB & water bills · Handwritten bills",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = viewfinderFg.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }

                    // Capture Options
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CaptureOptionTile(
                                icon = Icons.Filled.CameraAlt,
                                label = "Camera",
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) cameraLauncher.launch(null)
                                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier.weight(1f).testTag("scan_camera_button")
                            )
                            CaptureOptionTile(
                                icon = Icons.Filled.PhotoLibrary,
                                label = strings.gallery,
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("scan_gallery_button")
                            )
                            CaptureOptionTile(
                                icon = Icons.Filled.PictureAsPdf,
                                label = strings.pdf,
                                onClick = {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                },
                                modifier = Modifier.weight(1f).testTag("scan_pdf_button")
                            )
                        }
                    }

                    // Sample Garage Bill trigger (uses real Firebase AI Gemini extraction)
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusPill("Live AI", isAccent = true)
                                    Text(
                                        text = "No paper receipt handy?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Generate a realistic Sri Vel Two-Wheeler Care GST invoice with handwritten alignment lines & chain kit sub-items, and run live Gemini multimodal extraction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onScanSample,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("scan_sample_bill_button")
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.trySampleBill, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            is ScanUiState.Processing -> {
                val steps = listOf(
                    "Reading text, merchant & GSTIN",
                    "Splitting items & kit sub-items",
                    "Matching HSN/SAC & GST rates",
                    "Linking category & vehicle tracker"
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = strings.readingBill,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            steps.forEachIndexed { idx, label ->
                                val stepNum = idx + 1
                                val done = scanState.completedStep >= stepNum
                                val active = scanState.completedStep + 1 == stepNum
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (done) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = extendedColors.ok, modifier = Modifier.size(20.dp))
                                    } else if (active) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colorScheme.primary)
                                    } else {
                                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(1.5.dp, colorScheme.outlineVariant, CircleShape))
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (done || active) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (done || active) colorScheme.onSurface else colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is ScanUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SelavuTopNavBar(title = strings.scanTitle, onBack = onBack)
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = colorScheme.error)
                                Text("Could not extract bill", style = MaterialTheme.typography.titleMedium, color = colorScheme.error)
                            }
                            Text(scanState.message, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                            Button(
                                onClick = onResetScan,
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Try another photo or sample")
                            }
                        }
                    }
                }
            }

            is ScanUiState.Review -> {
                val draft = scanState.draft
                val selectedVehicle = vehicles.firstOrNull { it.id == draft.selectedVehicleId }
                val linkedServiceItems = remember(draft, selectedVehicle) {
                    if (draft.suggestedCategoryId != "veh" || selectedVehicle == null) emptyList()
                    else {
                        val mappedIds = draft.items.mapNotNull { it.mappedServiceItemId }.toSet()
                        selectedVehicle.items.filter { it.id in mappedIds }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SelavuTopNavBar(
                            title = strings.reviewBill,
                            onBack = onResetScan,
                            trailing = {
                                StatusPill(
                                    text = "${draft.confidencePercent}% AI confidence",
                                    status = if (draft.confidencePercent >= 85) ServiceStatus.OK else ServiceStatus.DUE_SOON
                                )
                            }
                        )
                    }

                    // Merchant, GSTIN & Bill number
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SelavuTextField(
                                    value = draft.merchant,
                                    onValueChange = { v -> onUpdateDraft { it.copy(merchant = v) } },
                                    label = "Merchant / workshop",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SelavuTextField(
                                        value = draft.gstin,
                                        onValueChange = { v -> onUpdateDraft { it.copy(gstin = v.uppercase()) } },
                                        label = "GSTIN",
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    SelavuTextField(
                                        value = draft.billNumber,
                                        onValueChange = { v -> onUpdateDraft { it.copy(billNumber = v) } },
                                        label = "Bill no.",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Bill date: ${IndianFormatters.formatDate(draft.dateEpochMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Category + Vehicle Auto-link
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(strings.category, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { cat ->
                                        SelavuFilterChip(
                                            label = cat.name.substringBefore(" & "),
                                            selected = draft.suggestedCategoryId == cat.id,
                                            onClick = {
                                                onUpdateDraft {
                                                    it.copy(
                                                        suggestedCategoryId = cat.id,
                                                        suggestedSubcategory = cat.subcategories.firstOrNull().orEmpty()
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                if (draft.suggestedCategoryId == "veh" && vehicles.isNotEmpty()) {
                                    SelavuDivider()
                                    Text("Linked vehicle", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        vehicles.forEach { v ->
                                            SelavuFilterChip(
                                                label = v.shortName,
                                                selected = draft.selectedVehicleId == v.id,
                                                onClick = { onUpdateDraft { it.copy(selectedVehicleId = v.id) } }
                                            )
                                        }
                                    }

                                    if (linkedServiceItems.isNotEmpty() && selectedVehicle != null) {
                                        val nextPreview = linkedServiceItems.first().let { svc ->
                                            val previewState = svc.copy(
                                                lastDoneEpochMs = draft.dateEpochMs,
                                                lastDoneKm = selectedVehicle.odometer
                                            ).computeState(selectedVehicle.odometer, draft.dateEpochMs)
                                            previewState.nextDueLabel
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.small)
                                                .background(extendedColors.okSoft)
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(Icons.Filled.Link, contentDescription = null, tint = extendedColors.ok, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(
                                                    text = "${linkedServiceItems.size} tracker item${if (linkedServiceItems.size > 1) "s" else ""} will reset on save",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = extendedColors.ok
                                                )
                                                Text(
                                                    text = "${linkedServiceItems.joinToString(", ") { it.name }} · $nextPreview",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Line items
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted items (${draft.items.size})",
                                style = SelavuTheme.textStyles.sectionTitle,
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = "+ Add line",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable {
                                        val nextId = (draft.items.maxOfOrNull { it.id } ?: 0) + 1
                                        val newItem = ScannedBillItem(
                                            id = nextId,
                                            name = "New item",
                                            qty = 1.0,
                                            unit = "pcs",
                                            rate = 100.0,
                                            gstRate = 18.0
                                        )
                                        onUpdateDraft { it.copy(items = it.items + newItem) }
                                        onEditItem(newItem)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    items(draft.items, key = { it.id }) { item ->
                        SelavuCard(onClick = { onEditItem(item) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        val hsnPart = if (item.hsn.isNotBlank()) "HSN ${item.hsn} · " else ""
                                        Text(
                                            text = "${item.qty} ${item.unit} × ${IndianFormatters.formatInr(item.rate)} · ${hsnPart}GST ${item.gstRate.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = IndianFormatters.formatInr(item.taxableAmount, decimals = 2),
                                            style = SelavuTheme.textStyles.numericBody,
                                            color = colorScheme.onSurface
                                        )
                                        Row {
                                            IconButton(onClick = { onEditItem(item) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit line", tint = colorScheme.outline, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete line", tint = colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (item.subItems.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(colorScheme.surfaceVariant)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item.subItems.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("↳ ${sub.name} (${sub.qty.toInt()}x)", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                                Text(IndianFormatters.formatInr(sub.lineAmount), style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }

                                if (!item.lowConfidenceNote.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(extendedColors.warnSoft)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = extendedColors.warn, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Check: ${item.lowConfidenceNote}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = extendedColors.warn
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Tax Summary & Item-Level Toggle
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tax breakdown (IGST inter-state)", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = draft.isInterStateIgst,
                                        onCheckedChange = { v -> onUpdateDraft { it.copy(isInterStateIgst = v) } },
                                        colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary)
                                    )
                                }
                                SummaryRow("Taxable value", IndianFormatters.formatInr(draft.taxableTotal, 2))
                                if (draft.isInterStateIgst) {
                                    SummaryRow("IGST", IndianFormatters.formatInr(draft.igst, 2))
                                } else {
                                    SummaryRow("CGST", IndianFormatters.formatInr(draft.cgst, 2))
                                    SummaryRow("SGST", IndianFormatters.formatInr(draft.sgst, 2))
                                }
                                val roPrefix = if (draft.roundOff >= 0) "+" else ""
                                SummaryRow("Round-off", "$roPrefix${"%.2f".format(draft.roundOff)}")
                                SelavuDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Grand total", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                                    Text(
                                        text = IndianFormatters.formatInr(draft.grandTotal),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }

                                SelavuDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.keepItemLevel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                        Text("Searchable items, HSN & GST splits", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = draft.keepItemLevel,
                                        onCheckedChange = { v -> onUpdateDraft { it.copy(keepItemLevel = v) } },
                                        colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }

                    // Save Scanned Bill Button
                    item {
                        Button(
                            onClick = { onSaveBill(draft) },
                            enabled = !isSaving && draft.items.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_scanned_bill_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("${strings.saveBill} · ${IndianFormatters.formatInr(draft.grandTotal)}", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureOptionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = SelavuTheme.textStyles.numericCaption,
            color = colorScheme.onSurface
        )
    }
}
