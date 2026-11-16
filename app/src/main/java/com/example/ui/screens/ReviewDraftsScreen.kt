package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Category
import com.example.data.model.Project
import com.example.data.model.ServiceStatus
import com.example.data.model.Vehicle
import com.example.data.notifications.ReviewDraft
import com.example.data.notifications.ReviewDraftItem
import com.example.data.notifications.SmsExpenseReader
import com.example.ui.components.CategoryTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelavuCard
import com.example.ui.components.SelavuDivider
import com.example.ui.components.SelavuEmptyState
import com.example.ui.components.SelavuFilterChip
import com.example.ui.components.SelavuTextField
import com.example.ui.components.SelavuTopNavBar
import com.example.ui.components.StatusPill
import com.example.ui.theme.SelavuTheme
import com.example.util.IndianFormatters
import java.util.UUID

@Composable
fun ReviewDraftsScreen(
    drafts: List<ReviewDraft>,
    categories: List<Category>,
    vehicles: List<Vehicle> = emptyList(),
    projects: List<Project> = emptyList(),
    smsAutoReadEnabled: Boolean,
    smsSinceEpochMs: Long,
    disclosureAccepted: Boolean,
    inWorkspace: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onAcceptDisclosureAndEnable: () -> Unit,
    onToggleSmsAutoRead: (Boolean) -> Unit,
    onSyncTodaySms: () -> Unit,
    onUpdateDraft: (ReviewDraft) -> Unit,
    onApproveDraft: (ReviewDraft) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var smsPermissionGranted by remember {
        mutableStateOf(SmsExpenseReader.hasAllSmsPermissions(context))
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val readGranted = results[android.Manifest.permission.READ_SMS] == true ||
            SmsExpenseReader.hasReadSmsPermission(context)
        smsPermissionGranted = SmsExpenseReader.hasAllSmsPermissions(context)
        if (readGranted) {
            onAcceptDisclosureAndEnable()
        } else {
            onShowToast("SMS permission denied. You can enable it anytime in App Settings.")
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsPermissionGranted = SmsExpenseReader.hasAllSmsPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showDisclosureDialog by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<ReviewDraft?>(null) }

    if (showDisclosureDialog) {
        SmsDisclosureDialog(
            sinceEpochMs = smsSinceEpochMs,
            onDismiss = { showDisclosureDialog = false },
            onConfirm = {
                showDisclosureDialog = false
                if (!SmsExpenseReader.hasAllSmsPermissions(context)) {
                    smsPermissionLauncher.launch(SmsExpenseReader.requiredPermissions)
                } else {
                    onAcceptDisclosureAndEnable()
                }
            }
        )
    }

    editingDraft?.let { draft ->
        ReviewDraftEditorSheet(
            draft = draft,
            categories = categories,
            vehicles = vehicles,
            projects = projects,
            inWorkspace = inWorkspace,
            isSaving = isSaving,
            onDismiss = { editingDraft = null },
            onSaveDraftChanges = { updated ->
                onUpdateDraft(updated)
                editingDraft = null
            },
            onApprove = { updated ->
                onUpdateDraft(updated)
                onApproveDraft(updated)
                editingDraft = null
            },
            onDelete = {
                onDeleteDraft(draft.id)
                editingDraft = null
            },
            onShowToast = onShowToast
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelavuTopNavBar(title = "Review alerts", onBack = onBack)
            }

            // SMS Access & Privacy Control Card
            item {
                val readGranted = smsPermissionGranted || SmsExpenseReader.hasReadSmsPermission(context)
                val sinceLabel = IndianFormatters.formatDate(smsSinceEpochMs)
                SelavuCard(modifier = Modifier.testTag("review_permission_card")) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sms,
                                    contentDescription = "Read SMS expense alerts",
                                    tint = colorScheme.onPrimaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Read SMS expenses (today onwards)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (readGranted) {
                                        "SMS permission granted · Reading from $sinceLabel onwards"
                                    } else {
                                        "Internal testing · SMS permission required (opt-in)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (readGranted) extendedColors.ok else colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = smsAutoReadEnabled && disclosureAccepted && readGranted,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!disclosureAccepted || !readGranted) {
                                            showDisclosureDialog = true
                                        } else {
                                            onToggleSmsAutoRead(true)
                                        }
                                    } else {
                                        onToggleSmsAutoRead(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary),
                                modifier = Modifier.testTag("review_sms_switch")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = extendedColors.ok,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = "Reads new bank/UPI debit SMS messages from today ($sinceLabel) onwards only. OTPs, verification codes, and non-spend alerts are ignored immediately in memory. Drafts stay device-local (preserved across sign out) until approved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        if (!readGranted) {
                            OutlinedButton(
                                onClick = {
                                    if (!disclosureAccepted) {
                                        showDisclosureDialog = true
                                    } else {
                                        smsPermissionLauncher.launch(SmsExpenseReader.requiredPermissions)
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("grant_sms_permission_button")
                            ) {
                                Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant SMS permission")
                            }
                        } else if (smsAutoReadEnabled) {
                            OutlinedButton(
                                onClick = onSyncTodaySms,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sync_today_sms_button")
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync SMS from $sinceLabel onwards")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Pending review (${drafts.size})")
            }

            if (drafts.isEmpty()) {
                item {
                    val readGranted = smsPermissionGranted || SmsExpenseReader.hasReadSmsPermission(context)
                    SelavuEmptyState(
                        icon = Icons.Filled.NotificationsActive,
                        title = "No pending expense drafts",
                        subtitle = if (smsAutoReadEnabled && readGranted) {
                            "New bank or UPI debit SMS messages from today onwards will appear here so you can categorize, add items, and approve them."
                        } else {
                            "Enable Read SMS above to queue new bank or UPI spend SMS alerts from today onwards for review."
                        }
                    )
                }
            } else {
                items(drafts, key = { it.id }) { draft ->
                    val cat = categories.firstOrNull { it.id == draft.categoryId }
                    SelavuCard(
                        onClick = { editingDraft = draft },
                        modifier = Modifier.testTag("review_draft_card_${draft.id}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryTile(
                                    iconKey = cat?.iconKey ?: "tag",
                                    colorIndex = cat?.colorIndex ?: 9
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = draft.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val catLabel = cat?.name ?: "Other"
                                    val subLabel = draft.subcategory.takeIf { it.isNotBlank() }?.let { " › $it" }.orEmpty()
                                    Text(
                                        text = "$catLabel$subLabel · ${draft.paymentMode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = IndianFormatters.formatInr(draft.amount),
                                        style = SelavuTheme.textStyles.numericTitle,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = IndianFormatters.formatShortDate(draft.dateEpochMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.outline
                                    )
                                }
                            }

                            if (draft.snippet.isNotBlank()) {
                                Text(
                                    text = "\"${draft.snippet}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.outline,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(text = draft.sourceLabel, status = ServiceStatus.OK)
                                val draftVeh = draft.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
                                val draftProj = draft.projectId?.let { pid -> projects.firstOrNull { it.id == pid } }
                                if (draftVeh != null) {
                                    StatusPill(text = draftVeh.shortName, isAccent = true)
                                }
                                if (draftProj != null) {
                                    StatusPill(text = draftProj.name, isAccent = true)
                                }
                                if (draft.items.isNotEmpty()) {
                                    StatusPill(text = "${draft.items.size} items", isAccent = true)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { onDeleteDraft(draft.id) },
                                    modifier = Modifier.size(48.dp).testTag("delete_draft_${draft.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete draft",
                                        tint = colorScheme.error
                                    )
                                }
                                OutlinedButton(
                                    onClick = { editingDraft = draft },
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(48.dp).testTag("edit_draft_${draft.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Map / Edit", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = { onApproveDraft(draft) },
                                    enabled = !isSaving,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(48.dp).testTag("approve_draft_${draft.id}")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve", style = MaterialTheme.typography.labelMedium)
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
private fun SmsDisclosureDialog(
    sinceEpochMs: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sinceLabel = IndianFormatters.formatDate(sinceEpochMs)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = "SMS reader disclosure",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RecapSpends uses Read SMS permission (internal testing opt-in) only to detect bank & UPI spend SMS messages from today ($sinceLabel) onwards.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• SMS messages before $sinceLabel are skipped.\n• OTPs, verification codes, and non-spend alerts are ignored immediately in memory without storing or uploading.\n• Drafts remain device-local (even across sign-out) until you edit and approve them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_sms_disclosure")
            ) {
                Text("Agree & grant SMS", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReviewDraftEditorSheet(
    draft: ReviewDraft,
    categories: List<Category>,
    vehicles: List<Vehicle>,
    projects: List<Project>,
    inWorkspace: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveDraftChanges: (ReviewDraft) -> Unit,
    onApprove: (ReviewDraft) -> Unit,
    onDelete: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nowMs = remember { System.currentTimeMillis() }

    var title by remember(draft.id) { mutableStateOf(draft.title) }
    var amountText by remember(draft.id) {
        mutableStateOf(
            if (draft.amount % 1.0 == 0.0) draft.amount.toInt().toString() else "%.2f".format(draft.amount)
        )
    }
    var dateEpochMs by remember(draft.id) { mutableStateOf(draft.dateEpochMs) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMode by remember(draft.id) { mutableStateOf(draft.paymentMode) }
    var categoryId by remember(draft.id) {
        mutableStateOf(
            draft.categoryId.takeIf { id -> categories.any { it.id == id } }
                ?: categories.firstOrNull()?.id ?: "groc"
        )
    }
    val selectedCategory = categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull()
    var subcategory by remember(draft.id) {
        mutableStateOf(
            draft.subcategory.takeIf { it.isNotBlank() }
                ?: selectedCategory?.subcategories?.firstOrNull().orEmpty()
        )
    }
    var selectedVehicleId by remember(draft.id) {
        mutableStateOf(draft.vehicleId?.takeIf { vid -> vehicles.any { it.id == vid } })
    }
    var selectedProjectId by remember(draft.id) {
        mutableStateOf(draft.projectId?.takeIf { pid -> projects.any { it.id == pid } })
    }
    var fuelOdometerText by remember(draft.id) {
        mutableStateOf(draft.odometerReading?.toString().orEmpty())
    }
    var fuelPricePerLitreText by remember(draft.id) {
        mutableStateOf(
            draft.fuelPricePerLitre?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else "%.2f".format(it)
            }.orEmpty()
        )
    }
    var fuelLitresText by remember(draft.id) {
        mutableStateOf(
            draft.fuelLitres?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else "%.2f".format(it)
            }.orEmpty()
        )
    }
    var isPrivate by remember(draft.id) { mutableStateOf(draft.isPrivate) }
    val draftItems = remember(draft.id) {
        mutableStateListOf<ReviewDraftItem>().apply { addAll(draft.items) }
    }

    val isVehicleFuelOrService = selectedVehicleId != null && categoryId == "veh" && (
        subcategory.equals("Fuel", ignoreCase = true) ||
            subcategory.equals("Service", ignoreCase = true)
        )
    val isVehicleFuel = selectedVehicleId != null && categoryId == "veh" &&
        subcategory.equals("Fuel", ignoreCase = true)

    fun buildUpdated(): ReviewDraft? {
        val parsedItems = draftItems.filter { it.name.isNotBlank() }
        val itemsSum = parsedItems.sumOf { it.amount }
        val amt = (amountText.toDoubleOrNull() ?: if (itemsSum > 0) itemsSum else null)
        if (amt == null || amt <= 0.0) {
            onShowToast("Enter a valid amount greater than ₹0")
            return null
        }
        val odo = if (isVehicleFuelOrService) fuelOdometerText.toIntOrNull()?.takeIf { it >= 0 } else null
        val fuelRate = if (isVehicleFuel) fuelPricePerLitreText.toDoubleOrNull()?.takeIf { it > 0.0 } else null
        val fuelQty = if (isVehicleFuel) fuelLitresText.toDoubleOrNull()?.takeIf { it > 0.0 } else null
        return draft.copy(
            title = title.trim().ifBlank { "Transaction" },
            amount = amt,
            dateEpochMs = dateEpochMs,
            paymentMode = paymentMode,
            categoryId = categoryId,
            subcategory = subcategory,
            vehicleId = selectedVehicleId,
            projectId = selectedProjectId,
            odometerReading = odo,
            fuelPricePerLitre = fuelRate,
            fuelLitres = fuelQty,
            isPrivate = isPrivate,
            items = parsedItems
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateEpochMs = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Review & map expense", style = MaterialTheme.typography.headlineMedium)

            if (draft.snippet.isNotBlank()) {
                Text(
                    text = draft.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }

            SelavuTextField(
                value = title,
                onValueChange = { title = it },
                label = "Merchant / Title",
                modifier = Modifier.fillMaxWidth().testTag("review_edit_title")
            )

            SelavuTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = "Amount (₹)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("review_edit_amount")
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    SelavuFilterChip(
                        label = cat.name.substringBefore(" & "),
                        selected = categoryId == cat.id,
                        onClick = {
                            categoryId = cat.id
                            val subs = cat.subcategories
                            if (subs.isNotEmpty() && subcategory !in subs) {
                                subcategory = subs.first()
                            }
                        }
                    )
                }
            }

            val subs = selectedCategory?.subcategories.orEmpty()
            if (subs.isNotEmpty()) {
                Text("Sub-category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subs.forEach { sub ->
                        SelavuFilterChip(
                            label = sub,
                            selected = subcategory == sub,
                            onClick = { subcategory = sub }
                        )
                    }
                }
            }

            if (vehicles.isNotEmpty()) {
                Text("Link vehicle (optional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelavuFilterChip(
                        label = "None",
                        selected = selectedVehicleId == null,
                        onClick = { selectedVehicleId = null },
                        modifier = Modifier.testTag("review_veh_chip_none")
                    )
                    vehicles.forEach { v ->
                        SelavuFilterChip(
                            label = "${v.shortName} (${IndianFormatters.formatIndianInteger(v.odometer.toLong())} km)",
                            selected = selectedVehicleId == v.id,
                            onClick = {
                                selectedVehicleId = v.id
                                val vehCat = categories.firstOrNull { it.id == "veh" }
                                if (vehCat != null && categoryId != "veh") {
                                    categoryId = "veh"
                                    if (subcategory !in vehCat.subcategories) {
                                        subcategory = vehCat.subcategories.firstOrNull { it.equals("Fuel", true) }
                                            ?: vehCat.subcategories.firstOrNull().orEmpty()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("review_veh_chip_${v.id}")
                        )
                    }
                }

                val selectedVeh = vehicles.firstOrNull { it.id == selectedVehicleId }
                if (selectedVeh != null && isVehicleFuelOrService) {
                    SelavuTextField(
                        value = fuelOdometerText,
                        onValueChange = { if (it.all(Char::isDigit)) fuelOdometerText = it },
                        label = "Odometer reading (km · optional)",
                        placeholder = "Current ${IndianFormatters.formatIndianInteger(selectedVeh.odometer.toLong())} km",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("review_fuel_odometer_input")
                    )
                }
                if (selectedVeh != null && isVehicleFuel) {
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
                            modifier = Modifier.weight(1f).testTag("review_fuel_price_input")
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
                            modifier = Modifier.weight(1f).testTag("review_fuel_litres_input")
                        )
                    }
                }
            }

            if (projects.isNotEmpty()) {
                Text("Link project (optional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelavuFilterChip(
                        label = "None",
                        selected = selectedProjectId == null,
                        onClick = { selectedProjectId = null },
                        modifier = Modifier.testTag("review_proj_chip_none")
                    )
                    projects.forEach { proj ->
                        SelavuFilterChip(
                            label = proj.name,
                            selected = selectedProjectId == proj.id,
                            onClick = { selectedProjectId = proj.id },
                            modifier = Modifier.testTag("review_proj_chip_${proj.id}")
                        )
                    }
                }
            }

            Text("Date & payment", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelavuFilterChip(
                    label = IndianFormatters.formatDate(dateEpochMs),
                    selected = true,
                    onClick = { showDatePicker = true }
                )
                listOf("UPI", "Cash", "Card", "Bank").forEach { mode ->
                    SelavuFilterChip(
                        label = mode,
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode }
                    )
                }
            }

            SelavuDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item-level details (${draftItems.size})", style = MaterialTheme.typography.labelLarge)
                TextButton(
                    onClick = { draftItems += ReviewDraftItem(id = UUID.randomUUID().toString().take(8)) }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add item")
                }
            }

            draftItems.forEachIndexed { idx, line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelavuTextField(
                        value = line.name,
                        onValueChange = { v -> draftItems[idx] = line.copy(name = v) },
                        label = "Item",
                        modifier = Modifier.weight(1.7f)
                    )
                    SelavuTextField(
                        value = if (line.qty % 1.0 == 0.0) line.qty.toInt().toString() else line.qty.toString(),
                        onValueChange = { v -> draftItems[idx] = line.copy(qty = v.toDoubleOrNull() ?: 1.0) },
                        label = "Qty",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.7f)
                    )
                    SelavuTextField(
                        value = if (line.rate == 0.0) "" else if (line.rate % 1.0 == 0.0) line.rate.toInt().toString() else line.rate.toString(),
                        onValueChange = { v -> draftItems[idx] = line.copy(rate = v.toDoubleOrNull() ?: 0.0) },
                        label = "Rate ₹",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { draftItems.removeAt(idx) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item", tint = colorScheme.error)
                    }
                }
            }

            if (inWorkspace) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide from Family Workspace", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Keep this expense private to you", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary)
                    )
                }
            }

            Button(
                onClick = { buildUpdated()?.let(onApprove) },
                enabled = !isSaving,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("review_sheet_approve_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Approve & save expense", style = MaterialTheme.typography.titleSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { buildUpdated()?.let(onSaveDraftChanges) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Save draft")
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Delete draft")
                }
            }
        }
    }
}
