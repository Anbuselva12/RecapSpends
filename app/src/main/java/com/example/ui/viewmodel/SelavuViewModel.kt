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
import com.example.data.firebase.RetainedWorkspaceBundle
import com.example.data.firebase.SelavuRepository
import com.example.data.firebase.deleteAccountCompletely
import com.example.data.firebase.updateAuthDisplayName
import com.example.data.model.AttentionAlert
import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseItem
import com.example.data.model.Project
import com.example.data.model.ProjectPaymentItem
import com.example.data.model.ProjectProduct
import com.example.data.model.ScannedBillDraft
import com.example.data.model.ScannedBillItem
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceStatus
import com.example.data.model.SharedBill
import com.example.data.model.SharedCategory
import com.example.data.model.SharedExpense
import com.example.data.model.SharedProject
import com.example.data.model.SharedProjectContribution
import com.example.data.model.SharedVehicle
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.data.model.Workspace
import com.example.data.notifications.ReviewDraft
import com.example.data.notifications.ReviewDraftStore
import com.example.data.notifications.SmsExpenseReader
import com.example.util.IndianFormatters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        val effectiveCategories: List<Category> = categories,
        val sharedCategoryOwners: Map<String, String> = emptyMap(),
        val expenses: List<Expense>,
        val vehicles: List<Vehicle>,
        val bills: List<Bill>,
        val projects: List<Project>,
        val workspace: Workspace? = null,
        val sharedExpenses: List<SharedExpense> = emptyList(),
        val sharedVehicles: List<SharedVehicle> = emptyList(),
        val sharedProjects: List<SharedProject> = emptyList(),
        val sharedProjectContributions: List<SharedProjectContribution> = emptyList(),
        val sharedBills: List<SharedBill> = emptyList(),
        val sharedCategories: List<SharedCategory> = emptyList(),
        val retainedVehicles: List<SharedVehicle> = emptyList(),
        val retainedProjects: List<SharedProject> = emptyList(),
        val retainedExpenses: List<SharedExpense> = emptyList(),
        val retainedProjectContributions: List<SharedProjectContribution> = emptyList()
    ) : AppDataState {
        val pickerVehicles: List<Vehicle>
            get() = vehicles + sharedVehicles.filter { it.ownerUid != profile.userId }.map { it.toVehicle() }

        val pickerProjects: List<Project>
            get() = projects + sharedProjects.filter { it.ownerUid != profile.userId }.map { it.toProject() }

        val retainedVehicleIds: Set<String>
            get() = retainedVehicles.mapTo(HashSet()) { it.id }

        val retainedProjectIds: Set<String>
            get() = retainedProjects.mapTo(HashSet()) { it.id }

        fun isRetainedExpense(expense: Expense): Boolean =
            (expense.vehicleId != null && expense.vehicleId in retainedVehicleIds) ||
                (expense.projectId != null && expense.projectId in retainedProjectIds)
    }
    data class Error(val message: String) : AppDataState
}

private data class PersonalBundle(
    val profile: UserProfile?,
    val categories: List<Category>,
    val expenses: List<Expense>,
    val vehicles: List<Vehicle>,
    val bills: List<Bill>
)

private data class ProjectsAndWsBundle(
    val projects: List<Project>,
    val workspace: Workspace?,
    val sharedExpenses: List<SharedExpense>,
    val sharedVehicles: List<SharedVehicle>,
    val sharedProjects: List<SharedProject>
)

private data class ExtraSharedBundle(
    val sharedProjectContributions: List<SharedProjectContribution>,
    val sharedBills: List<SharedBill>,
    val sharedCategories: List<SharedCategory>,
    val retained: RetainedWorkspaceBundle
)

class SelavuViewModel(
    private val repository: SelavuRepository,
    private val scannerService: BillScannerService,
    private val draftStore: ReviewDraftStore,
    private val currentUserId: String,
    private val displayName: String?
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Capture)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val syncMutex = Mutex()

    val reviewDrafts: StateFlow<List<ReviewDraft>> = draftStore.observeDrafts(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val smsAutoReadEnabled: StateFlow<Boolean> = draftStore.observeSmsEnabled(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    val smsSinceEpochMs: StateFlow<Long> = draftStore.observeSmsSinceEpochMs(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ReviewDraftStore.startOfTodayEpochMs()
        )

    val disclosureAccepted: StateFlow<Boolean> = draftStore.observeDisclosureAccepted(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    private val profileFlow = repository.observeProfile(currentUserId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workspaceFlow = profileFlow
        .map { it?.workspaceId }
        .distinctUntilChanged()
        .flatMapLatest { wsId -> repository.observeWorkspace(wsId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeWsIdFlow = combine(profileFlow, workspaceFlow) { profile, ws ->
        val wsId = profile?.workspaceId
        if (wsId != null && ws != null && currentUserId in ws.memberIds) wsId else null
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedExpensesFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedExpenses(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedVehiclesFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedVehicles(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedProjectsFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedProjects(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedProjectContributionsFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedProjectContributions(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedBillsFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedBills(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedCategoriesFlow = activeWsIdFlow
        .flatMapLatest { wsId -> repository.observeSharedCategories(wsId, currentUserId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val retainedBundleFlow = combine(profileFlow, activeWsIdFlow) { profile, activeWsId ->
        (profile?.contributedWorkspaceIds ?: emptyList()) to activeWsId
    }.distinctUntilChanged()
        .flatMapLatest { (contributedIds, activeWsId) ->
            repository.observeRetainedWorkspaceBundle(currentUserId, contributedIds, activeWsId)
        }

    private val personalBundleFlow = combine(
        profileFlow,
        repository.observeCategories(currentUserId),
        repository.observeExpenses(currentUserId),
        repository.observeVehicles(currentUserId),
        repository.observeBills(currentUserId)
    ) { profile, cats, exps, vehs, bills ->
        PersonalBundle(profile, cats, exps, vehs, bills)
    }

    private val projectsAndWsBundleFlow = combine(
        repository.observeProjects(currentUserId),
        workspaceFlow,
        sharedExpensesFlow,
        sharedVehiclesFlow,
        sharedProjectsFlow
    ) { projects, workspace, shExps, shVehs, shProjs ->
        ProjectsAndWsBundle(projects, workspace, shExps, shVehs, shProjs)
    }

    private val extraSharedBundleFlow = combine(
        sharedProjectContributionsFlow,
        sharedBillsFlow,
        sharedCategoriesFlow,
        retainedBundleFlow
    ) { contribs, shBills, shCats, retained ->
        ExtraSharedBundle(contribs, shBills, shCats, retained)
    }

    val appState: StateFlow<AppDataState> = combine(
        personalBundleFlow,
        projectsAndWsBundleFlow,
        extraSharedBundleFlow
    ) { personal, pws, extra ->
        val profile = personal.profile ?: UserProfile(id = currentUserId, userId = currentUserId)
        val (effectiveCats, sharedCatOwners) = mergeEffectiveCategories(
            ownCategories = personal.categories,
            sharedCategories = extra.sharedCategories,
            currentUid = currentUserId
        )
        val activeVehIds = (personal.vehicles.map { "${currentUserId}_${it.id}" } + pws.sharedVehicles.map { it.id }).toSet()
        val activeProjIds = (pws.projects.map { "${currentUserId}_${it.id}" } + pws.sharedProjects.map { it.id }).toSet()
        AppDataState.Ready(
            profile = profile,
            categories = personal.categories,
            effectiveCategories = effectiveCats,
            sharedCategoryOwners = sharedCatOwners,
            expenses = personal.expenses,
            vehicles = personal.vehicles,
            bills = personal.bills,
            projects = pws.projects,
            workspace = pws.workspace,
            sharedExpenses = pws.sharedExpenses,
            sharedVehicles = pws.sharedVehicles,
            sharedProjects = pws.sharedProjects,
            sharedProjectContributions = extra.sharedProjectContributions,
            sharedBills = extra.sharedBills,
            sharedCategories = extra.sharedCategories,
            retainedVehicles = extra.retained.vehicles.filter { it.id !in activeVehIds },
            retainedProjects = extra.retained.projects.filter { it.id !in activeProjIds },
            retainedExpenses = extra.retained.expenses,
            retainedProjectContributions = extra.retained.projectContributions
        ) as AppDataState
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
            draftStore.setActiveUser(currentUserId)
            draftStore.ensureSmsSinceEpochMs(currentUserId)
        }
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
        viewModelScope.launch {
            @Suppress("OPT_IN_USAGE")
            appState
                .debounce(250L)
                .collect { state ->
                    if (state is AppDataState.Ready) {
                        val wsId = state.profile.workspaceId
                        val ws = state.workspace
                        if (wsId != null && ws != null && currentUserId in ws.memberIds) {
                            syncMutex.withLock {
                                try {
                                    val ownerName = state.profile.displayName?.takeIf { it.isNotBlank() }
                                        ?: displayName?.takeIf { it.isNotBlank() }
                                        ?: "Member"
                                    val myShared = state.sharedExpenses.filter { it.ownerUid == currentUserId }
                                    val myContribs = state.sharedProjectContributions.filter { it.ownerUid == currentUserId }
                                    repository.syncMyWorkspaceShares(
                                        uid = currentUserId,
                                        ownerName = ownerName,
                                        workspace = ws,
                                        categories = state.categories,
                                        expenses = state.expenses,
                                        vehicles = state.vehicles,
                                        bills = state.bills,
                                        projects = state.projects,
                                        existingSharedByMe = myShared,
                                        existingSharedVehicles = state.sharedVehicles,
                                        existingSharedProjects = state.sharedProjects,
                                        existingSharedBills = state.sharedBills,
                                        existingSharedCategories = state.sharedCategories,
                                        existingMyContributions = myContribs
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.w(TAG, "Workspace share sync skipped: ${e.message}")
                                }
                            }
                        } else if (wsId != null && ws != null && currentUserId !in ws.memberIds) {
                            try {
                                repository.clearUserWorkspaceId(currentUserId)
                                showToast("You were removed from ${ws.name}")
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
        }
    }

    private fun mergeEffectiveCategories(
        ownCategories: List<Category>,
        sharedCategories: List<SharedCategory>,
        currentUid: String
    ): Pair<List<Category>, Map<String, String>> {
        val merged = ownCategories.map { it.copy() }.toMutableList()
        val ownersMap = mutableMapOf<String, String>()
        val others = sharedCategories.filter { it.ownerUid != currentUid }
        for (sc in others) {
            val idx = merged.indexOfFirst {
                it.id == sc.sourceCategoryId || it.name.equals(sc.name, ignoreCase = true)
            }
            if (idx >= 0) {
                val existing = merged[idx]
                val combinedSubs = (existing.subcategories + sc.subcategories)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                merged[idx] = existing.copy(subcategories = combinedSubs)
            } else {
                val virtual = sc.toCategory()
                merged += virtual
                ownersMap[virtual.id] = sc.ownerName
            }
        }
        return merged.sortedBy { it.sortOrder } to ownersMap
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
                val res = deleteAccountCompletely(credentialManager, repository, draftStore)
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

    // --- Read SMS (Current Date Only) & Device-Local Review Queue ---

    fun acceptSmsDisclosureAndEnable(context: Context) {
        viewModelScope.launch {
            val todayStart = draftStore.ensureSmsSinceEpochMs(currentUserId)
            draftStore.setSmsEnabled(
                uid = currentUserId,
                enabled = true,
                acceptedDisclosure = true,
                sinceEpochMs = todayStart
            )
            if (SmsExpenseReader.hasReadSmsPermission(context)) {
                val added = SmsExpenseReader.syncInboxSince(context, currentUserId, todayStart, draftStore)
                showToast(
                    if (added > 0) "SMS reader enabled · $added transaction alerts queued for today"
                    else "SMS auto-detect enabled for today (debits & credits; OTPs ignored)"
                )
            } else {
                showToast("SMS auto-detect enabled for today (debits & credits; OTPs ignored)")
            }
        }
    }

    fun setSmsAutoReadEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            val todayStart = draftStore.ensureSmsSinceEpochMs(currentUserId)
            draftStore.setSmsEnabled(currentUserId, enabled = enabled, sinceEpochMs = todayStart)
            if (enabled) {
                if (SmsExpenseReader.hasReadSmsPermission(context)) {
                    val added = SmsExpenseReader.syncInboxSince(context, currentUserId, todayStart, draftStore)
                    showToast(
                        if (added > 0) "SMS reader ON · $added alerts found today"
                        else "SMS reader turned ON (today · debits & credits; OTPs ignored)"
                    )
                } else {
                    showToast("SMS reader turned ON (grant READ_SMS permission to scan)")
                }
            } else {
                showToast("SMS transaction reader paused")
            }
        }
    }

    fun syncTodaySms(context: Context, userTriggered: Boolean = false) {
        if (!SmsExpenseReader.hasReadSmsPermission(context)) {
            if (userTriggered) showToast("Grant SMS permission first to read today's transaction alerts")
            return
        }
        viewModelScope.launch {
            val todayStart = draftStore.ensureSmsSinceEpochMs(currentUserId)
            val added = SmsExpenseReader.syncInboxSince(context, currentUserId, todayStart, draftStore)
            if (added > 0) {
                showToast("Queued $added new alert${if (added > 1) "s" else ""} from today's SMS")
            } else if (userTriggered) {
                showToast("No new transaction SMS found for today (${IndianFormatters.formatDate(todayStart)})")
            }
        }
    }

    fun updateReviewDraft(draft: ReviewDraft) {
        viewModelScope.launch {
            draftStore.updateDraft(currentUserId, draft)
        }
    }

    fun deleteReviewDraft(draftId: String) {
        viewModelScope.launch {
            draftStore.removeDraft(currentUserId, draftId)
            showToast("Draft discarded")
        }
    }

    fun approveReviewDraft(draft: ReviewDraft, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val ready = appState.value as? AppDataState.Ready
                val selectedVehicle = draft.vehicleId?.let { id ->
                    ready?.pickerVehicles?.firstOrNull { it.id == id }
                }
                val selectedProject = draft.projectId?.let { id ->
                    ready?.pickerProjects?.firstOrNull { it.id == id }
                }
                val isVehFuelOrService = draft.categoryId == "veh" && (
                    draft.subcategory.equals("Fuel", ignoreCase = true) ||
                        draft.subcategory.equals("Service", ignoreCase = true)
                    )
                val odo = if (selectedVehicle != null && isVehFuelOrService) draft.odometerReading else null
                val fuelRate = if (selectedVehicle != null && draft.categoryId == "veh" && draft.subcategory.equals("Fuel", ignoreCase = true)) {
                    draft.fuelPricePerLitre
                } else null
                val fuelQty = if (selectedVehicle != null && draft.categoryId == "veh" && draft.subcategory.equals("Fuel", ignoreCase = true)) {
                    draft.fuelLitres
                } else null

                val items = draft.items
                    .filter { it.name.isNotBlank() }
                    .map { item ->
                        ExpenseItem(
                            name = item.name.trim(),
                            qty = item.qty,
                            unit = "pcs",
                            rate = item.rate,
                            amount = item.amount
                        )
                    }
                    .takeIf { it.isNotEmpty() }
                val entryType = if (draft.entryType == "income") "income" else "expense"
                val expense = Expense(
                    title = draft.title.trim().ifBlank {
                        if (entryType == "income") "SMS income" else "Notification expense"
                    },
                    amount = draft.amount.coerceAtLeast(0.01),
                    dateEpochMs = draft.dateEpochMs,
                    categoryId = draft.categoryId.ifBlank { if (entryType == "income") "inc" else "groc" },
                    subcategory = draft.subcategory,
                    paymentMode = draft.paymentMode,
                    entryType = entryType,
                    projectId = selectedProject?.id,
                    vehicleId = selectedVehicle?.id,
                    isPrivate = draft.isPrivate,
                    items = items,
                    odometerReading = odo,
                    fuelPricePerLitre = fuelRate,
                    fuelLitres = fuelQty
                )
                repository.addExpense(
                    expense = expense,
                    vehicleToReset = selectedVehicle,
                    odometerReading = odo,
                    serviceItemIdsDone = emptyList(),
                    projectToCharge = selectedProject
                )
                recordForeignContributionIfNeeded(ready, selectedVehicle, selectedProject, draft.isPrivate)
                draftStore.removeDraft(currentUserId, draft.id)
                val prefix = if (entryType == "income") "Income +" else ""
                showToast("Approved & saved $prefix${IndianFormatters.formatInr(expense.amount)}")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to approve draft")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun recordForeignContributionIfNeeded(
        ready: AppDataState.Ready?,
        vehicle: Vehicle?,
        project: Project?,
        isPrivate: Boolean
    ) {
        val wsId = ready?.workspace?.id ?: return
        if (currentUserId !in ready.workspace.memberIds || isPrivate) return
        if (vehicle != null && vehicle.userId.isNotBlank() && vehicle.userId != currentUserId) {
            repository.recordVehicleContributor(wsId, vehicle.id, vehicle.userId)
        }
        if (project != null && project.userId.isNotBlank() && project.userId != currentUserId) {
            repository.recordProjectContributor(wsId, project.id, project.userId)
        }
    }

    // --- Shared Family Workspace & Privacy ---

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val ownerName = (appState.value as? AppDataState.Ready)?.profile?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: displayName?.takeIf { it.isNotBlank() }
                    ?: "Owner"
                val ws = repository.createWorkspace(name, ownerName)
                showToast("Workspace \"${ws.name}\" created · Code: ${ws.inviteCode}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to create workspace")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun joinWorkspace(inviteCode: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val joinerName = (appState.value as? AppDataState.Ready)?.profile?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: displayName?.takeIf { it.isNotBlank() }
                    ?: "Member"
                repository.joinWorkspaceByCode(inviteCode, joinerName)
                showToast("Joined family workspace")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Could not join workspace")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun regenerateInviteCode(workspace: Workspace) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val ownerName = (appState.value as? AppDataState.Ready)?.profile?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: displayName?.takeIf { it.isNotBlank() }
                    ?: "Owner"
                val code = repository.regenerateInviteCode(workspace, ownerName)
                showToast("New invite code: $code")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to refresh invite code")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun removeWorkspaceMember(workspace: Workspace, targetUid: String, targetName: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.removeMemberFromWorkspace(workspace, targetUid)
                showToast("$targetName removed and their shared expenses deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to remove member")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun leaveWorkspace(workspaceId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.leaveWorkspace(workspaceId)
                showToast("Left workspace. All your shared expenses were removed.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to leave workspace")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteWorkspace(workspace: Workspace) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.deleteWorkspace(workspace)
                showToast("Workspace deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete workspace")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun toggleExpensePrivacy(expense: Expense) {
        val nextPrivate = !expense.isPrivate
        viewModelScope.launch {
            try {
                repository.updateExpensePrivacy(expense.id, nextPrivate)
                showToast(if (nextPrivate) "Hidden from Family Workspace" else "Shared with Family Workspace")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update visibility")
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val ready = appState.value as? AppDataState.Ready
                repository.deleteExpense(expenseId)
                val retainedExp = ready?.retainedExpenses?.firstOrNull { it.sourceExpenseId == expenseId }
                if (retainedExp != null) {
                    repository.deleteRetainedSharedExpense(retainedExp)
                }
                val matchingContrib = ready?.sharedProjectContributions?.firstOrNull {
                    it.linkedExpenseId == expenseId && it.ownerUid == currentUserId
                } ?: ready?.retainedProjectContributions?.firstOrNull {
                    it.linkedExpenseId == expenseId && it.ownerUid == currentUserId
                }
                if (matchingContrib != null) {
                    repository.deleteSharedProjectContribution(matchingContrib.workspaceId, matchingContrib)
                }
                showToast("Expense deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete expense")
            }
        }
    }

    fun deleteRetainedSharedExpense(sharedExpense: SharedExpense) {
        viewModelScope.launch {
            try {
                repository.deleteRetainedSharedExpense(sharedExpense)
                showToast("Contribution deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete contribution")
            }
        }
    }

    fun deleteSharedProjectContribution(contribution: SharedProjectContribution) {
        viewModelScope.launch {
            try {
                repository.deleteSharedProjectContribution(contribution.workspaceId, contribution)
                showToast("Project payment deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete payment")
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
                val ready = appState.value as? AppDataState.Ready
                repository.addExpense(
                    expense = expense,
                    vehicleToReset = vehicleToReset,
                    odometerReading = odometerReading,
                    serviceItemIdsDone = serviceItemIdsDone,
                    projectToCharge = projectToCharge
                )
                recordForeignContributionIfNeeded(ready, vehicleToReset, projectToCharge, expense.isPrivate)
                val extra = if (serviceItemIdsDone.isNotEmpty()) {
                    ". ${serviceItemIdsDone.size} tracker item${if (serviceItemIdsDone.size > 1) "s" else ""} reset"
                } else ""
                val prefix = if (expense.entryType == "income") "Income +" else ""
                showToast("Saved $prefix${IndianFormatters.formatInr(expense.amount)}$extra")
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

    fun updateExpense(
        expense: Expense,
        vehicleToReset: Vehicle?,
        odometerReading: Int?,
        serviceItemIdsDone: List<String>,
        projectToCharge: Project?,
        onDone: () -> Unit
    ) {
        val ready = appState.value as? AppDataState.Ready
        if (ready?.isRetainedExpense(expense) == true) {
            showToast("Past workspace contributions are read-only (delete only).")
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateExpense(
                    expense = expense,
                    vehicleToReset = vehicleToReset,
                    odometerReading = odometerReading,
                    serviceItemIdsDone = serviceItemIdsDone
                )
                recordForeignContributionIfNeeded(ready, vehicleToReset, projectToCharge, expense.isPrivate)
                val prefix = if (expense.entryType == "income") "Income +" else ""
                showToast("Updated $prefix${IndianFormatters.formatInr(expense.amount)}")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update expense")
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
        hiddenFromWorkspace: Boolean = false,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.addVehicle(name, type, plate, odometer, avgMileage, hiddenFromWorkspace)
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

    fun toggleVehicleHiddenFromWorkspace(vehicle: Vehicle) {
        if (vehicle.userId.isNotBlank() && vehicle.userId != currentUserId) {
            showToast("Only the creator can change sharing for this vehicle.")
            return
        }
        val nextHidden = !vehicle.hiddenFromWorkspace
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(hiddenFromWorkspace = nextHidden))
                showToast(if (nextHidden) "Vehicle hidden from Family Workspace" else "Vehicle shared with Family Workspace")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update vehicle visibility")
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

    fun recordLastServiceDone(
        vehicle: Vehicle,
        itemId: String,
        doneDateEpochMs: Long,
        doneKm: Int
    ) {
        val nowMs = System.currentTimeMillis()
        val safeDateMs = doneDateEpochMs.coerceIn(0L, nowMs)
        val safeKm = doneKm.coerceIn(0, 2_000_000)
        val updatedItems = vehicle.items.map { item ->
            if (item.id == itemId) {
                item.copy(lastDoneEpochMs = safeDateMs, lastDoneKm = safeKm)
            } else item
        }
        val target = updatedItems.firstOrNull { it.id == itemId } ?: return
        val state = target.computeState(vehicle.odometer, nowMs)
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = updatedItems))
                showToast("Last service updated for ${target.name} (${IndianFormatters.formatDate(safeDateMs)}). ${state.nextDueLabel}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update last service")
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
        val effectiveDate = if (newItem.lastDoneEpochMs > 0L) newItem.lastDoneEpochMs.coerceAtMost(nowMs) else nowMs
        val effectiveKm = if (newItem.lastDoneKm >= 0) newItem.lastDoneKm else vehicle.odometer
        val itemToSave = newItem.copy(
            lastDoneEpochMs = effectiveDate,
            lastDoneKm = effectiveKm
        )
        viewModelScope.launch {
            try {
                repository.updateVehicle(vehicle.copy(items = vehicle.items + itemToSave))
                showToast("${newItem.name} added (${IndianFormatters.formatDate(effectiveDate)}).")
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

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            try {
                repository.updateBill(bill)
                showToast("Reminder updated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update reminder")
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBill(billId)
                showToast("Reminder deleted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to delete reminder")
            }
        }
    }

    fun markBillPaid(bill: Bill, categories: List<Category>) {
        if (bill.userId.isNotBlank() && bill.userId != currentUserId) {
            showToast("Only ${bill.userId} can mark their shared reminder paid.")
            return
        }
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

    fun addProject(
        name: String,
        budget: Double,
        template: String,
        hiddenFromWorkspace: Boolean = false,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.addProject(name, budget, template, hiddenFromWorkspace)
                showToast("Project created with $template phases")
                onCreated(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to create project")
            }
        }
    }

    fun toggleProjectHiddenFromWorkspace(project: Project) {
        if (project.userId.isNotBlank() && project.userId != currentUserId) {
            showToast("Only the creator can change sharing for this project.")
            return
        }
        val nextHidden = !project.hiddenFromWorkspace
        viewModelScope.launch {
            try {
                repository.updateProject(project.copy(hiddenFromWorkspace = nextHidden))
                showToast(if (nextHidden) "Project hidden from Family Workspace" else "Project shared with Family Workspace")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update project visibility")
            }
        }
    }

    fun addProjectPayment(
        project: Project,
        phaseIdx: Int,
        title: String,
        amount: Double,
        vendor: String,
        items: List<ProjectPaymentItem> = emptyList(),
        updatedProductCatalog: List<ProjectProduct>? = null
    ) {
        viewModelScope.launch {
            try {
                val ready = appState.value as? AppDataState.Ready
                val sharedProj = ready?.sharedProjects?.firstOrNull { it.id == project.id && it.ownerUid != currentUserId }
                if (sharedProj != null && ready.workspace != null && currentUserId in ready.workspace.memberIds) {
                    val ownerName = ready.profile.displayName?.takeIf { it.isNotBlank() }
                        ?: displayName?.takeIf { it.isNotBlank() }
                        ?: "Member"
                    val phaseName = project.phases.getOrNull(phaseIdx)?.name ?: "Contribution"
                    repository.addSharedProjectContribution(
                        workspace = ready.workspace,
                        sharedProject = sharedProj,
                        ownerName = ownerName,
                        phaseName = phaseName,
                        title = title,
                        amount = amount,
                        vendor = vendor,
                        items = items
                    )
                    showToast("${IndianFormatters.formatInr(amount)} added to shared project")
                } else {
                    repository.addProjectPayment(
                        project = project,
                        phaseIndex = phaseIdx,
                        title = title,
                        amount = amount,
                        vendorName = vendor,
                        items = items,
                        updatedProductCatalog = updatedProductCatalog
                    )
                    val phaseName = project.phases.getOrNull(phaseIdx)?.name ?: "project"
                    showToast("${IndianFormatters.formatInr(amount)} added to $phaseName")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to save payment")
            }
        }
    }

    fun updateProjectProducts(project: Project, products: List<ProjectProduct>) {
        if (project.userId.isNotBlank() && project.userId != currentUserId) {
            showToast("Only the creator can edit project products.")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateProjectProducts(project, products)
                showToast("Project products & units updated (${products.size})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "Failed to update project products")
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
            Log.e(TAG, "Bill scan extraction error", e)
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
        categories: List<Category>,
        sharedVehicles: List<SharedVehicle> = emptyList(),
        sharedBills: List<SharedBill> = emptyList()
    ): List<AttentionAlert> {
        val nowMs = System.currentTimeMillis()
        val alerts = mutableListOf<AttentionAlert>()

        val allVehicles = vehicles + sharedVehicles
            .filter { it.ownerUid != currentUserId }
            .map { it.toVehicle(includeServiceData = true) }

        allVehicles.forEach { v ->
            val ownerTag = if (v.userId.isNotBlank() && v.userId != currentUserId) {
                sharedVehicles.firstOrNull { it.id == v.id }?.ownerName?.let { " ($it)" }.orEmpty()
            } else ""
            v.items.forEach { item ->
                val state = item.computeState(v.odometer, nowMs)
                if (state.status != ServiceStatus.OK) {
                    alerts += AttentionAlert(
                        id = "${v.id}_${item.id}",
                        title = "${item.name} · ${v.shortName}$ownerTag",
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

        val allBills = bills + sharedBills
            .filter { it.ownerUid != currentUserId }
            .map { it.toBill() }

        allBills.filter { !it.paid }.forEach { b ->
            val dl = IndianFormatters.daysBetween(b.dueEpochMs, nowMs)
            if (dl <= 7) {
                val catColor = categories.firstOrNull { it.id == b.categoryId }?.colorIndex ?: 4
                val status = if (dl < 0) ServiceStatus.OVERDUE else ServiceStatus.DUE_SOON
                val label = when {
                    dl < 0 -> "Overdue ${-dl} days"
                    dl == 0 -> "Due today"
                    else -> "Due in $dl days"
                }
                val ownerTag = if (b.userId.isNotBlank() && b.userId != currentUserId) {
                    sharedBills.firstOrNull { it.id == b.id }?.ownerName?.let { " · $it" }.orEmpty()
                } else ""
                alerts += AttentionAlert(
                    id = b.id,
                    title = "${b.name}$ownerTag · ${IndianFormatters.formatInr(b.amount)}",
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
                    draftStore = ReviewDraftStore(appContext),
                    currentUserId = userId,
                    displayName = displayName
                ) as T
            }
        }
    }
}
