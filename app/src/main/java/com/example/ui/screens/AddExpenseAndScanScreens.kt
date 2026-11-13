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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.Project
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceStatus
import com.example.data.model.Vehicle
import com.example.ui.components.CategoryTile
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuFilterChip
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
    isSaving: Boolean,
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
    val colors = SelavuTheme.colors
    val strings = selavuStrings()
    val nowMs = remember { System.currentTimeMillis() }

    val defaultCatId = initialCategoryId?.takeIf { id -> categories.any { it.id == id } }
        ?: categories.firstOrNull()?.id ?: "groc"
    var selectedCatId by rememberSaveable { mutableStateOf(defaultCatId) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCatId } ?: categories.firstOrNull()

    var selectedSub by rememberSaveable {
        mutableStateOf(
            initialSubcategory?.takeIf { it.isNotBlank() }
                ?: selectedCategory?.subcategories?.firstOrNull().orEmpty()
        )
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && categories.none { it.id == selectedCatId }) {
            selectedCatId = categories.first().id
        }
    }

    LaunchedEffect(selectedCatId, categories) {
        val subs = categories.firstOrNull { it.id == selectedCatId }?.subcategories.orEmpty()
        if (subs.isNotEmpty() && selectedSub !in subs) {
            selectedSub = subs.first()
        }
    }

    var amountText by rememberSaveable { mutableStateOf("") }
    var titleText by rememberSaveable { mutableStateOf("") }
    var dateEpochMs by rememberSaveable { mutableStateOf(nowMs) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMode by rememberSaveable { mutableStateOf("UPI") }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }

    val defaultVehicleId = initialVehicleId?.takeIf { id -> vehicles.any { it.id == id } }
        ?: vehicles.firstOrNull()?.id
    var selectedVehicleId by rememberSaveable { mutableStateOf(defaultVehicleId) }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    var odometerText by rememberSaveable(selectedVehicleId) {
        mutableStateOf(selectedVehicle?.odometer?.toString().orEmpty())
    }
    var checkedServiceIds by rememberSaveable(selectedVehicleId) { mutableStateOf(setOf<String>()) }

    var showItemLines by rememberSaveable { mutableStateOf(false) }
    val draftLines = remember { mutableStateListOf<DraftLineInput>() }

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

    val isVehicleService = selectedCatId == "veh" && selectedSub.equals("Service", ignoreCase = true)

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
                    title = strings.addExpense,
                    onBack = onBack,
                    trailing = {
                        SelavuFilterChip(
                            label = strings.scanInstead,
                            selected = true,
                            icon = Icons.Filled.DocumentScanner,
                            onClick = onSwitchToScan,
                            modifier = Modifier.testTag("add_switch_to_scan")
                        )
                    }
                )
            }

            // Big amount input
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.ink3
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                                color = colors.accentInk
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
                                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                                        color = colors.ink3.copy(alpha = 0.45f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 34.sp,
                                    color = colors.ink
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.line
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_amount_input")
                            )
                        }

                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text(strings.whatWasItFor) },
                            placeholder = { Text("e.g. Reliance Fresh, Sri Vel Garage, TNEB") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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
                        Text(strings.category, style = MaterialTheme.typography.labelLarge, color = colors.ink)
                        if (categories.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "No categories yet after clearing data. Restore default Indian categories or log under General.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                                OutlinedButton(
                                    onClick = onRestoreStarterCategories,
                                    shape = RoundedCornerShape(12.dp),
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
                                categories.forEach { cat ->
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
                            Text(strings.subCategory, style = MaterialTheme.typography.labelLarge, color = colors.ink)
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

            // Signature Vehicle > Service section
            if (isVehicleService && vehicles.isNotEmpty()) {
                item {
                    SelavuCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Vehicle service tracker link",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vehicles.forEach { v ->
                                    SelavuFilterChip(
                                        label = "${v.shortName} (${IndianFormatters.formatIndianInteger(v.odometer.toLong())} km)",
                                        selected = selectedVehicleId == v.id,
                                        onClick = {
                                            selectedVehicleId = v.id
                                            odometerText = v.odometer.toString()
                                            checkedServiceIds = emptySet()
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = odometerText,
                                onValueChange = { if (it.all(Char::isDigit)) odometerText = it },
                                label = { Text("Odometer reading now (km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("service_odometer_input")
                            )

                            if (selectedVehicle != null) {
                                Text(
                                    text = "Tap items completed today (resets next-due tracker):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
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
                        }
                    }
                }
            }

            // Date, Payment Mode & Optional Project
            item {
                SelavuCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.date, style = MaterialTheme.typography.labelLarge, color = colors.ink)
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

                        Text(strings.paidWith, style = MaterialTheme.typography.labelLarge, color = colors.ink)
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
                            Text("${strings.project} (optional)", style = MaterialTheme.typography.labelLarge, color = colors.ink)
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
                                Text(strings.itemLevelDetails, style = MaterialTheme.typography.labelLarge, color = colors.ink)
                                Text("Total auto-fills from line items", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                            }
                            Icon(
                                imageVector = if (showItemLines) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle item lines",
                                tint = colors.ink2
                            )
                        }

                        if (showItemLines) {
                            draftLines.forEachIndexed { idx, line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = line.name,
                                        onValueChange = { v -> draftLines[idx] = line.copy(name = v) },
                                        label = { Text("Item") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.8f)
                                    )
                                    OutlinedTextField(
                                        value = line.qtyText,
                                        onValueChange = { v -> draftLines[idx] = line.copy(qtyText = v) },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    OutlinedTextField(
                                        value = line.priceText,
                                        onValueChange = { v -> draftLines[idx] = line.copy(priceText = v) },
                                        label = { Text("Rate ₹") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { draftLines.removeAt(idx) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove line", tint = colors.bad)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { draftLines += DraftLineInput() },
                                shape = RoundedCornerShape(12.dp),
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
                            if (isVehicleService && selectedVehicle != null) {
                                "${selectedVehicle.shortName} service"
                            } else {
                                selectedSub.ifBlank { selectedCategory?.name ?: "Expense" }
                            }
                        }
                        val project = projects.firstOrNull { it.id == selectedProjectId }
                        val vehicle = if (isVehicleService) selectedVehicle else null
                        val odo = if (isVehicleService) odometerText.toIntOrNull() else null

                        onSave(
                            Expense(
                                title = finalTitle,
                                amount = amt,
                                dateEpochMs = dateEpochMs,
                                categoryId = selectedCatId,
                                subcategory = selectedSub,
                                paymentMode = paymentMode,
                                projectId = project?.id,
                                vehicleId = vehicle?.id,
                                items = validLines.takeIf { it.isNotEmpty() }
                            ),
                            vehicle,
                            odo,
                            checkedServiceIds.toList(),
                            project
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_expense_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.onAccent, strokeWidth = 2.dp)
                    } else {
                        Text(strings.saveExpense, style = MaterialTheme.typography.titleMedium)
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
    val colors = SelavuTheme.colors
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.hero)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val guideColor = colors.heroFg.copy(alpha = 0.72f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val corner = 28.dp.toPx()
                                val sw = 3.dp.toPx()
                                // Top-left
                                drawLine(guideColor, Offset(0f, 0f), Offset(corner, 0f), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(0f, 0f), Offset(0f, corner), sw, StrokeCap.Round)
                                // Top-right
                                drawLine(guideColor, Offset(size.width, 0f), Offset(size.width - corner, 0f), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(size.width, 0f), Offset(size.width, corner), sw, StrokeCap.Round)
                                // Bottom-left
                                drawLine(guideColor, Offset(0f, size.height), Offset(corner, size.height), sw, StrokeCap.Round)
                                drawLine(guideColor, Offset(0f, size.height), Offset(0f, size.height - corner), sw, StrokeCap.Round)
                                // Bottom-right
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
                                        .background(colors.heroFg.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "AI Bill Scan",
                                        tint = colors.heroFg,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Frame any Indian receipt or GST bill",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.heroFg
                                )
                                Text(
                                    text = "Shop receipts · GST invoices · EB & water bills · Handwritten bills",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.heroFg.copy(alpha = 0.78f)
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
                                        color = colors.ink
                                    )
                                }
                                Text(
                                    text = "Generate a realistic Sri Vel Two-Wheeler Care GST invoice with handwritten alignment lines & chain kit sub-items, and run live Gemini multimodal extraction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                                Button(
                                    onClick = onScanSample,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.onAccent
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
                        color = colors.accent,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = strings.readingBill,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.ink
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
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.ok, modifier = Modifier.size(20.dp))
                                    } else if (active) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                                    } else {
                                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(1.5.dp, colors.line, CircleShape))
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (done || active) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (done || active) colors.ink else colors.ink3
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
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.bad)
                                Text("Could not extract bill", style = MaterialTheme.typography.titleMedium, color = colors.bad)
                            }
                            Text(scanState.message, style = MaterialTheme.typography.bodyMedium, color = colors.ink2)
                            Button(
                                onClick = onResetScan,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
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
                                OutlinedTextField(
                                    value = draft.merchant,
                                    onValueChange = { v -> onUpdateDraft { it.copy(merchant = v) } },
                                    label = { Text("Merchant / workshop") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = draft.gstin,
                                        onValueChange = { v -> onUpdateDraft { it.copy(gstin = v.uppercase()) } },
                                        label = { Text("GSTIN") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = draft.billNumber,
                                        onValueChange = { v -> onUpdateDraft { it.copy(billNumber = v) } },
                                        label = { Text("Bill no.") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Bill date: ${IndianFormatters.formatDate(draft.dateEpochMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.ink2
                                )
                            }
                        }
                    }

                    // Category + Vehicle Auto-link
                    item {
                        SelavuCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(strings.category, style = MaterialTheme.typography.labelLarge, color = colors.ink)
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
                                    Text("Linked vehicle", style = MaterialTheme.typography.labelLarge, color = colors.ink)
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
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.okSoft)
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(Icons.Filled.Link, contentDescription = null, tint = colors.ok, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(
                                                    text = "${linkedServiceItems.size} tracker item${if (linkedServiceItems.size > 1) "s" else ""} will reset on save",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = colors.ok
                                                )
                                                Text(
                                                    text = "${linkedServiceItems.joinToString(", ") { it.name }} · $nextPreview",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.ink2
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = colors.ink
                            )
                            Text(
                                text = "+ Add line",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.accentInk,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
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
                                            color = colors.ink
                                        )
                                        val hsnPart = if (item.hsn.isNotBlank()) "HSN ${item.hsn} · " else ""
                                        Text(
                                            text = "${item.qty} ${item.unit} × ${IndianFormatters.formatInr(item.rate)} · ${hsnPart}GST ${item.gstRate.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.ink2
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = IndianFormatters.formatInr(item.taxableAmount, decimals = 2),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = colors.ink
                                        )
                                        Row {
                                            IconButton(onClick = { onEditItem(item) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit line", tint = colors.ink3, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete line", tint = colors.bad, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (item.subItems.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.sunk)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item.subItems.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("↳ ${sub.name} (${sub.qty.toInt()}x)", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                                                Text(IndianFormatters.formatInr(sub.lineAmount), style = MaterialTheme.typography.bodySmall, color = colors.ink)
                                            }
                                        }
                                    }
                                }

                                if (!item.lowConfidenceNote.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.warnSoft)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.warn, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Check: ${item.lowConfidenceNote}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.warn
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
                                    Text("Tax breakdown (IGST inter-state)", style = MaterialTheme.typography.bodyMedium, color = colors.ink2)
                                    Switch(
                                        checked = draft.isInterStateIgst,
                                        onCheckedChange = { v -> onUpdateDraft { it.copy(isInterStateIgst = v) } },
                                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
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
                                    Text("Grand total", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                                    Text(
                                        text = IndianFormatters.formatInr(draft.grandTotal),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = colors.accentInk
                                    )
                                }

                                SelavuDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.keepItemLevel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                                        Text("Searchable items, HSN & GST splits", style = MaterialTheme.typography.bodySmall, color = colors.ink2)
                                    }
                                    Switch(
                                        checked = draft.keepItemLevel,
                                        onCheckedChange = { v -> onUpdateDraft { it.copy(keepItemLevel = v) } },
                                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_scanned_bill_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.onAccent, strokeWidth = 2.dp)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SelavuTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = colors.accentInk, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colors = SelavuTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.ink2)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = colors.ink
        )
    }
}
