package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ai.BillScannerService
import com.example.data.firebase.SelavuRepository
import com.example.data.firebase.deleteAccountCompletely
import com.example.data.firebase.sendPasswordResetEmail
import com.example.data.firebase.updateAuthDisplayName
import com.example.data.model.AttentionAlert
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceStatus
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.util.IndianFormatters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Capture : ScanUiState
    data class Processing(val completedStep: Int) : ScanUiState
    data class Review(val draft: ScannedBillDraft) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface AppDataState {
    data object Loading : AppDataState
    data class Ready(
        val profile: UserProfile,
        val categories: List<Category>,
        val expenses: List<Expense>,
        val vehicles: List<Vehicle>,
        val bills: List<Bill>,
        val projects: List<Project>
    ) : AppDataState
    data class Error(val message: String) : AppDataState
}

class SelavuViewModel(
    private val repository: SelavuRepository,
    private val scannerService: BillScannerService,
    private val currentUserId: String,
    private val displayName: String?
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Capture)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val appState: StateFlow<AppDataState> = combine(
        repository.observeProfile(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeExpenses(currentUserId),
        repository.observeVehicles(currentUserId),
        repository.observeBills(currentUserId),
        repository.observeProjects(currentUserId)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profile = (values[0] as? UserProfile) ?: UserProfile(id = currentUserId, userId = currentUserId)
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[2] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val vehicles = values[3] as List<Vehicle>
        @Suppress("UNCHECKED_CAST")
        val bills = values[4] as List<Bill>
        @Suppress("UNCHECKED_CAST")
        val projects = values[5] as List<Project>
        AppDataState.Ready(profile, categories, expenses, vehicles, bills, projects) as AppDataState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing Selavu collections", rootError)
        emit(AppDataState.Error(rootError.message ?: "Failed to load financial data."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppDataState.Loading
    )

    init {
        viewModelScope.launch {
            try {
                repository.ensureSampleDataSeeded(currentUserId, displayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed initial sample data", e)
                showToast(e.localizedMessage ?: "Failed to initialize data.")
            }
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            try {
                repository.updateLanguage(lang)
                showToast(if (lang == "ta") "மொழி தமிழ்-க்கு மாற்றப்பட்டது" else "Language switched to English")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not update language")
            }
        }
    }

    fun updateBudget(newBudget: Double) {
        viewModelScope.launch {
            try {
                repository.updateMonthlyBudget(newBudget)
                showToast("Monthly budget set to ${IndianFormatters.formatInr(newBudget)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not update budget")
            }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val authRes = updateAuthDisplayName(newName)
                if (authRes.isFailure) {
                    showToast(authRes.exceptionOrNull()?.localizedMessage ?: "Could not update name")
                    return@launch
                }
                repository.updateDisplayName(newName)
                showToast("Display name updated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not update display name")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            val res = sendPasswordResetEmail(email)
            if (res.isSuccess) {
                showToast("Password reset email sent to ${email.trim()}")
            } else {
                showToast(res.exceptionOrNull()?.localizedMessage ?: "Could not send password reset email")
            }
        }
    }

    fun restoreStarterCategories() {
        viewModelScope.launch {
            try {
                repository.restoreStarterCategories(currentUserId)
                showToast("Starter categories added")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not restore categories")
            }
        }
    }

    fun clearAllDataFresh() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.deleteAllUserData(currentUserId, deleteProfileDoc = false)
                showToast("All expenses, vehicles, bills, projects & categories deleted. Fresh start ready.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not delete data")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteAccount(credentialManager: CredentialManager, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val res = deleteAccountCompletely(credentialManager, repository)
                if (res.isSuccess) {
                    onDeleted()
                } else {
                    showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to delete account")
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun addExpense(
        expense: Expense,
        vehicleToReset: Vehicle?,
        odometerReading: Int?,
        serviceItemIdsDone: List<String>,
        projectToCharge: Project?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.addExpense(
                    expense = expense,
                    vehicleToReset = vehicleToReset,
                    odometerReading = odometerReading,
                    serviceItemIdsDone = serviceItemIdsDone,
                    projectToCharge = projectToCharge
                )
                val extra = if (serviceItemIdsDone.isNotEmpty()) {
                    ". ${serviceItemIdsDone.size} tracker item${if (serviceItemIdsDone.size > 1) "s" else ""} reset"
                } else ""
                showToast("Saved ${IndianFormatters.formatInr(expense.amount)}$extra")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save expense")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun addVehicle(
        name: String,
        type: String,
        plate: String,
        odometer: Int,
        avgMileage: Double,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.addVehicle(name, type, plate, odometer, avgMileage)
                val count = if (type == "car") 6 else 8
                showToast("$name added with $count service items")
                onCreated(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to add vehicle")
            }
        }
    }

    fun updateVehicleOdometer(vehicle: Vehicle, newOdo: Int) {
        if (newOdo < vehicle.odometer) {
            showToast("Enter a reading of at least ${IndianFormatters.formatIndianInteger(vehicle.odometer.toLong())} km")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(odometer = newOdo))
                showToast("Odometer updated to ${IndianFormatters.formatIndianInteger(newOdo.toLong())} km")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update odometer")
            }
        }
    }

    fun markServiceItemDone(vehicle: Vehicle, itemId: String) {
        val nowMs = System.currentTimeMillis()
        val updatedItems = vehicle.items.map { item ->
            if (item.id == itemId) {
                item.copy(lastDoneEpochMs = nowMs, lastDoneKm = vehicle.odometer)
            } else item
        }
        val target = updatedItems.firstOrNull { it.id == itemId } ?: return
        val state = target.computeState(vehicle.odometer, nowMs)
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = updatedItems))
                showToast("${target.name} done. ${state.nextDueLabel}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update service item")
            }
        }
    }

    fun updateServiceItemInterval(vehicle: Vehicle, itemId: String, km: Int, months: Int) {
        val safeKm = km.coerceAtLeast(0)
        val safeMonths = if (safeKm == 0 && months <= 0) 12 else months.coerceAtLeast(1)
        val updatedItems = vehicle.items.map { item ->
            if (item.id == itemId) item.copy(intervalKm = safeKm, intervalMonths = safeMonths) else item
        }
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = updatedItems))
                showToast("Interval updated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save interval")
            }
        }
    }

    fun deleteServiceItem(vehicle: Vehicle, itemId: String) {
        val updatedItems = vehicle.items.filterNot { it.id == itemId }
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = updatedItems))
                showToast("Service item removed")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete item")
            }
        }
    }

    fun addServiceItem(vehicle: Vehicle, newItem: ServiceItem) {
        val nowMs = System.currentTimeMillis()
        val itemWithToday = newItem.copy(
            lastDoneEpochMs = nowMs,
            lastDoneKm = vehicle.odometer
        )
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = vehicle.items + itemWithToday))
                showToast("${newItem.name} added. Tracking starts today.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to add item")
            }
        }
    }

    fun addBill(bill: Bill) {
        viewModelScope.launch {
            try {
                repository.addBill(bill)
                showToast("Reminder set for ${IndianFormatters.formatDate(bill.dueEpochMs)}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to add reminder")
            }
        }
    }

    fun markBillPaid(bill: Bill, categories: List<Category>) {
        viewModelScope.launch {
            try {
                val nextDue = repository.markBillPaid(bill)
                val catName = categories.firstOrNull { it.id == bill.categoryId }?.name ?: "Expenses"
                val nextSuffix = if (nextDue != null) ". Next due ${IndianFormatters.formatDate(nextDue)}" else ""
                showToast("Paid ${IndianFormatters.formatInr(bill.amount)} and logged to $catName$nextSuffix")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to mark bill paid")
            }
        }
    }

    fun addProject(name: String, budget: Double, template: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addProject(name, budget, template)
                showToast("Project created with $template phases")
                onCreated(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to create project")
            }
        }
    }

    fun addProjectPayment(project: Project, phaseIdx: Int, title: String, amount: Double, vendor: String) {
        viewModelScope.launch {
            try {
                repository.addProjectPayment(project, phaseIdx, title, amount, vendor)
                val phaseName = project.phases.getOrNull(phaseIdx)?.name ?: "project"
                showToast("${IndianFormatters.formatInr(amount)} added to $phaseName")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save payment")
            }
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.saveCategory(category)
                showToast("Category saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save category")
            }
        }
    }

    fun deleteCategory(categoryId: String, expenses: List<Expense>) {
        val count = expenses.count { it.categoryId == categoryId }
        if (count > 0) {
            showToast("Still used by $count entries. Move them first.")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryId)
                showToast("Category deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete category")
            }
        }
    }

    // --- AI Bill Scan Flow ---

    fun resetScan() {
        _scanState.value = ScanUiState.Capture
    }

    fun scanSampleGarageBill() {
        val bmp = scannerService.renderSampleGarageInvoiceBitmap()
        scanBitmap(bmp)
    }

    fun scanFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Processing(1)
            try {
                val bmp = scannerService.loadBitmapFromUri(context, uri)
                runExtraction(bmp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load selected file", e)
                _scanState.value = ScanUiState.Error(e.localizedMessage ?: "Unable to read selected bill.")
            }
        }
    }

    fun scanBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            runExtraction(bitmap)
        }
    }

    private suspend fun runExtraction(bitmap: Bitmap) {
        _scanState.value = ScanUiState.Processing(1)
        val stepperJob = viewModelScope.launch {
            delay(500)
            if (_scanState.value is ScanUiState.Processing) _scanState.value = ScanUiState.Processing(2)
            delay(650)
            if (_scanState.value is ScanUiState.Processing) _scanState.value = ScanUiState.Processing(3)
        }
        try {
            val draft = scannerService.extractBillFromBitmap(bitmap)
            stepperJob.cancel()
            _scanState.value = ScanUiState.Processing(4)
            delay(350)
            _scanState.value = ScanUiState.Review(draft)
        } catch (e: CancellationException) {
            stepperJob.cancel()
            throw e
        } catch (e: Exception) {
            stepperJob.cancel()
            _scanState.value = ScanUiState.Error(
                e.localizedMessage ?: "AI bill scan failed. Check network/App Check and try again."
            )
        }
    }

    fun updateScanDraft(transform: (ScannedBillDraft) -> ScannedBillDraft) {
        val cur = _scanState.value as? ScanUiState.Review ?: return
        _scanState.value = ScanUiState.Review(transform(cur.draft))
    }

    fun updateScannedItem(itemId: Int, updated: ScannedBillItem) {
        updateScanDraft { draft ->
            draft.copy(items = draft.items.map { if (it.id == itemId) updated else it })
        }
    }

    fun deleteScannedItem(itemId: Int) {
        updateScanDraft { draft ->
            draft.copy(items = draft.items.filterNot { it.id == itemId })
        }
    }

    fun saveScannedBill(draft: ScannedBillDraft, vehicles: List<Vehicle>, onSaved: (String?) -> Unit) {
        val selectedVehicle = vehicles.firstOrNull { it.id == draft.selectedVehicleId }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val resetCount = repository.saveScannedBill(draft, selectedVehicle)
                val msg = buildString {
                    append("Saved ${draft.items.size} items · ${IndianFormatters.formatInr(draft.grandTotal)}")
                    if (resetCount > 0) append(". $resetCount tracker items reset.")
                }
                showToast(msg)
                resetScan()
                onSaved(selectedVehicle?.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save scanned bill")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun computeAttentionAlerts(
        vehicles: List<Vehicle>,
        bills: List<Bill>,
        categories: List<Category>
    ): List<AttentionAlert> {
        val nowMs = System.currentTimeMillis()
        val alerts = mutableListOf<AttentionAlert>()

        vehicles.forEach { v ->
            v.items.forEach { item ->
                val state = item.computeState(v.odometer, nowMs)
                if (state.status != ServiceStatus.OK) {
                    alerts += AttentionAlert(
                        id = "${v.id}_${item.id}",
                        title = "${item.name} · ${v.shortName}",
                        statusLabel = state.statusLabel,
                        severity = state.status,
                        iconKey = item.iconKey,
                        colorIndex = 1,
                        daysWeight = state.daysLeft,
                        targetVehicleId = v.id,
                        isBill = false
                    )
                }
            }
        }

        bills.filter { !it.paid }.forEach { b ->
            val dl = IndianFormatters.daysBetween(b.dueEpochMs, nowMs)
            if (dl <= 7) {
                val catColor = categories.firstOrNull { it.id == b.categoryId }?.colorIndex ?: 4
                val status = if (dl < 0) ServiceStatus.OVERDUE else ServiceStatus.DUE_SOON
                val label = when {
                    dl < 0 -> "Overdue ${-dl} days"
                    dl == 0 -> "Due today"
                    else -> "Due in $dl days"
                }
                alerts += AttentionAlert(
                    id = b.id,
                    title = "${b.name} · ${IndianFormatters.formatInr(b.amount)}",
                    statusLabel = label,
                    severity = status,
                    iconKey = b.iconKey,
                    colorIndex = catColor,
                    daysWeight = dl,
                    targetVehicleId = null,
                    isBill = true
                )
            }
        }

        return alerts.sortedWith(compareBy({ it.severity.severity }, { it.daysWeight }))
    }

    companion object {
        private const val TAG = "SelavuVM"

        fun factory(
            context: Context,
            userId: String,
            displayName: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val dbId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                return SelavuViewModel(
                    repository = SelavuRepository(db),
                    scannerService = BillScannerService(),
                    currentUserId = userId,
                    displayName = displayName
                ) as T
            }
        }
    }
}
