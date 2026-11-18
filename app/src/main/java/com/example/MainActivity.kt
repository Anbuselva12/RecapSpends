package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutUser
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.notifications.ReviewDraftStore
import com.example.data.notifications.SmsExpenseReader
import com.example.data.update.InAppUpdateController
import com.example.ui.components.InAppUpdateBanner
import com.example.ui.components.SelavuBottomBar
import com.example.ui.screens.ActiveSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddExpenseScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillsScreen
import com.example.ui.screens.CategoriesAndSettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ReviewDraftsScreen
import com.example.ui.screens.ScanBillScreen
import com.example.ui.screens.SelavuModalSheetHost
import com.example.ui.screens.TrackScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.SelavuTheme
import com.example.ui.viewmodel.AppDataState
import com.example.ui.viewmodel.SelavuViewModel
import com.example.util.EnglishStrings
import com.example.util.LocalSelavuStrings
import com.example.util.TamilStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.selavuPrefs by preferencesDataStore(name = "selavu_prefs")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_IGNORED_UPDATE_VERSION = intPreferencesKey("ignored_update_version")

private const val ROUTE_HOME = "home"
private const val ROUTE_TRACK = "track"
private const val ROUTE_BILLS = "bills"
private const val ROUTE_INSIGHTS = "insights"
private const val ROUTE_VEHICLE = "vehicle/{vehicleId}"
private const val ROUTE_PROJECT = "project/{projectId}"
private const val ROUTE_ADD = "add?cat={cat}&sub={sub}&veh={veh}&editId={editId}"
private const val ROUTE_SCAN = "scan"
private const val ROUTE_ACTIVITY = "activity"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_REVIEW = "review"
private const val ROUTE_WORKSPACE = "workspace"

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val themeMode by context.selavuPrefs.data
                .map { it[KEY_THEME_MODE] ?: "system" }
                .collectAsStateWithLifecycle(initialValue = "system")

            val ignoredUpdateVersion by context.selavuPrefs.data
                .map { it[KEY_IGNORED_UPDATE_VERSION] }
                .collectAsStateWithLifecycle(initialValue = null)

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xff, 0xff, 0xff),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { darkTheme }
                )
                onDispose {}
            }

            SelavuTheme(darkTheme = darkTheme) {
                val authUser by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                val user = authUser
                if (user == null) {
                    AuthScreen(onAuthSuccess = {})
                } else {
                    key(user.uid) {
                        SelavuAppRoot(
                            user = user,
                            themeMode = themeMode,
                            ignoredUpdateVersion = ignoredUpdateVersion,
                            onChangeThemeMode = { mode ->
                                scope.launch {
                                    context.selavuPrefs.edit { it[KEY_THEME_MODE] = mode }
                                }
                            },
                            onIgnoreUpdateVersion = { versionCode ->
                                scope.launch {
                                    context.selavuPrefs.edit {
                                        if (versionCode == null) {
                                            it.remove(KEY_IGNORED_UPDATE_VERSION)
                                        } else {
                                            it[KEY_IGNORED_UPDATE_VERSION] = versionCode
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelavuAppRoot(
    user: FirebaseUser,
    themeMode: String,
    ignoredUpdateVersion: Int?,
    onChangeThemeMode: (String) -> Unit,
    onIgnoreUpdateVersion: (Int?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val draftStore = remember(context) { ReviewDraftStore(context) }

    val viewModel: SelavuViewModel = viewModel(
        key = user.uid,
        factory = SelavuViewModel.factory(context, user.uid, user.displayName)
    )
    val dataState by viewModel.appState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val reviewDrafts by viewModel.reviewDrafts.collectAsStateWithLifecycle()
    val smsAutoReadEnabled by viewModel.smsAutoReadEnabled.collectAsStateWithLifecycle()
    val smsSinceEpochMs by viewModel.smsSinceEpochMs.collectAsStateWithLifecycle()
    val disclosureAccepted by viewModel.disclosureAccepted.collectAsStateWithLifecycle()

    // Flexible Play In-App Update integration
    val updateController = remember(context) { InAppUpdateController(context) }
    val updateUiState by updateController.uiState.collectAsStateWithLifecycle()
    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            viewModel.showToast("Update download cancelled")
        }
    }

    LaunchedEffect(ignoredUpdateVersion) {
        updateController.updateIgnoredVersion(ignoredUpdateVersion)
    }

    LaunchedEffect(smsAutoReadEnabled, disclosureAccepted) {
        if (smsAutoReadEnabled && disclosureAccepted && SmsExpenseReader.hasReadSmsPermission(context)) {
            viewModel.syncTodaySms(context, userTriggered = false)
        }
    }

    DisposableEffect(lifecycleOwner, updateController, ignoredUpdateVersion, smsAutoReadEnabled, disclosureAccepted) {
        updateController.registerListener()
        updateController.checkForUpdate(ignoredUpdateVersion)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateController.checkForUpdate(ignoredUpdateVersion)
                if (smsAutoReadEnabled && disclosureAccepted && SmsExpenseReader.hasReadSmsPermission(context)) {
                    viewModel.syncTodaySms(context, userTriggered = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            updateController.unregisterListener()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        val msg = toastMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearToast()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }

    val activeStrings = when ((dataState as? AppDataState.Ready)?.profile?.language) {
        "ta" -> TamilStrings
        else -> EnglishStrings
    }
    val userDisplayName = (dataState as? AppDataState.Ready)?.profile?.displayName?.takeIf { it.isNotBlank() }
        ?: user.displayName?.takeIf { it.isNotBlank() }
    val userPhotoUrl = user.photoUrl?.toString()

    val showBottomBar = currentRoute in setOf(ROUTE_HOME, ROUTE_TRACK, ROUTE_BILLS, ROUTE_INSIGHTS)

    CompositionLocalProvider(LocalSelavuStrings provides activeStrings) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    SelavuBottomBar(
                        currentTab = currentRoute,
                        onSelectTab = { target ->
                            navController.navigate(target) {
                                popUpTo(ROUTE_HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onFabClick = { activeSheet = ActiveSheet.QuickAddHub }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                InAppUpdateBanner(
                    state = updateUiState,
                    onStartUpdate = {
                        val started = updateController.startFlexibleUpdate(updateLauncher)
                        if (!started) {
                            viewModel.showToast("Could not start Play update flow right now.")
                        }
                    },
                    onCompleteUpdate = { updateController.completeUpdate() },
                    onIgnoreUpdate = { versionCode ->
                        onIgnoreUpdateVersion(versionCode)
                        updateController.dismissBanner()
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = dataState) {
                        is AppDataState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Syncing RecapSpends…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is AppDataState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Could not load your account",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    shape = MaterialTheme.shapes.small,
                                    onClick = { signOutUser(credentialManager, scope, draftStore) }
                                ) {
                                    Text("Sign out", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        is AppDataState.Ready -> {
                            val alerts = remember(
                                state.vehicles,
                                state.bills,
                                state.effectiveCategories,
                                state.sharedVehicles,
                                state.sharedBills
                            ) {
                                viewModel.computeAttentionAlerts(
                                    vehicles = state.vehicles,
                                    bills = state.bills,
                                    categories = state.effectiveCategories,
                                    sharedVehicles = state.sharedVehicles,
                                    sharedBills = state.sharedBills
                                )
                            }

                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_HOME
                            ) {
                                composable(ROUTE_HOME) {
                                    HomeScreen(
                                        monthlyBudget = state.profile.monthlyBudget,
                                        categories = state.effectiveCategories,
                                        expenses = state.expenses,
                                        alerts = alerts,
                                        userDisplayName = userDisplayName,
                                        userPhotoUrl = userPhotoUrl,
                                        currentUid = user.uid,
                                        reviewDraftCount = reviewDrafts.size,
                                        workspace = state.workspace,
                                        sharedExpenses = state.sharedExpenses,
                                        onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                        onOpenScan = {
                                            viewModel.resetScan()
                                            navController.navigate(ROUTE_SCAN)
                                        },
                                        onOpenAddExpense = {
                                            navController.navigate("add")
                                        },
                                        onOpenLogService = {
                                            navController.navigate("add?cat=veh&sub=Service")
                                        },
                                        onOpenNewReminder = {
                                            activeSheet = ActiveSheet.NewBillReminder
                                        },
                                        onOpenVehicle = { vid ->
                                            navController.navigate("vehicle/$vid")
                                        },
                                        onOpenBills = {
                                            navController.navigate(ROUTE_BILLS) {
                                                popUpTo(ROUTE_HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onOpenInsights = {
                                            navController.navigate(ROUTE_INSIGHTS) {
                                                popUpTo(ROUTE_HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onOpenActivity = { navController.navigate(ROUTE_ACTIVITY) },
                                        onOpenExpenseDetail = { exp ->
                                            activeSheet = ActiveSheet.ExpenseDetail(exp)
                                        },
                                        onEditBudget = {
                                            activeSheet = ActiveSheet.EditMonthlyBudget(state.profile.monthlyBudget)
                                        },
                                        onOpenReview = { navController.navigate(ROUTE_REVIEW) },
                                        onOpenWorkspace = { navController.navigate(ROUTE_WORKSPACE) },
                                        onOpenSharedExpenseDetail = { shared ->
                                            activeSheet = ActiveSheet.SharedExpenseDetail(shared)
                                        }
                                    )
                                }

                                composable(ROUTE_TRACK) {
                                    TrackScreen(
                                        vehicles = state.vehicles,
                                        projects = state.projects,
                                        currentUid = user.uid,
                                        inWorkspace = state.workspace != null,
                                        sharedVehicles = state.sharedVehicles,
                                        sharedProjects = state.sharedProjects,
                                        sharedProjectContributions = state.sharedProjectContributions,
                                        retainedVehicles = state.retainedVehicles,
                                        retainedProjects = state.retainedProjects,
                                        userDisplayName = userDisplayName,
                                        userPhotoUrl = userPhotoUrl,
                                        onOpenVehicle = { vid -> navController.navigate("vehicle/$vid") },
                                        onAddVehicle = { activeSheet = ActiveSheet.AddVehicle },
                                        onOpenProject = { pid -> navController.navigate("project/$pid") },
                                        onAddProject = { activeSheet = ActiveSheet.AddProject },
                                        onOpenProfile = { navController.navigate(ROUTE_CATEGORIES) }
                                    )
                                }

                                composable(ROUTE_BILLS) {
                                    BillsScreen(
                                        bills = state.bills,
                                        categories = state.effectiveCategories,
                                        currentUid = user.uid,
                                        sharedBills = state.sharedBills,
                                        userDisplayName = userDisplayName,
                                        userPhotoUrl = userPhotoUrl,
                                        onMarkPaid = { bill -> viewModel.markBillPaid(bill, state.effectiveCategories) },
                                        onOpenNewReminder = { activeSheet = ActiveSheet.NewBillReminder },
                                        onOpenBillDetail = { bill -> activeSheet = ActiveSheet.EditBillReminder(bill) },
                                        onOpenProfile = { navController.navigate(ROUTE_CATEGORIES) }
                                    )
                                }

                                composable(ROUTE_INSIGHTS) {
                                    InsightsScreen(
                                        monthlyBudget = state.profile.monthlyBudget,
                                        categories = state.effectiveCategories,
                                        expenses = state.expenses,
                                        vehicles = state.pickerVehicles,
                                        userDisplayName = userDisplayName,
                                        userPhotoUrl = userPhotoUrl,
                                        onOpenCategoryDrilldown = { cat ->
                                            activeSheet = ActiveSheet.CategoryDrilldown(cat)
                                        },
                                        onExportGstCsv = {
                                            shareGstCsv(context, state.expenses, state.effectiveCategories)
                                        },
                                        onOpenProfile = { navController.navigate(ROUTE_CATEGORIES) }
                                    )
                                }

                                composable(
                                    route = ROUTE_VEHICLE,
                                    arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
                                ) { entry ->
                                    val vid = entry.arguments?.getString("vehicleId").orEmpty()
                                    val ownVehicle = state.vehicles.firstOrNull { it.id == vid }
                                    val sharedVehicle = state.sharedVehicles.firstOrNull { it.id == vid && it.ownerUid != user.uid }
                                    val retainedVehicle = state.retainedVehicles.firstOrNull { it.id == vid }
                                    val vehicle = ownVehicle
                                        ?: sharedVehicle?.toVehicle(includeServiceData = true)
                                        ?: retainedVehicle?.toVehicle(includeServiceData = false)
                                    val isOwner = ownVehicle != null
                                    val isRetained = !isOwner && sharedVehicle == null && retainedVehicle != null
                                    val ownerName = sharedVehicle?.ownerName ?: retainedVehicle?.ownerName
                                    val sharedVehExpenses = when {
                                        isOwner -> state.sharedExpenses.filter { it.sharedVehicleId == "${user.uid}_${vid}" && it.ownerUid != user.uid }
                                        sharedVehicle != null -> state.sharedExpenses.filter { it.sharedVehicleId == vid }
                                        isRetained -> state.retainedExpenses.filter { it.sharedVehicleId == vid && it.ownerUid == user.uid }
                                        else -> emptyList()
                                    }
                                    if (vehicle != null) {
                                        VehicleDetailScreen(
                                            vehicle = vehicle,
                                            expenses = state.expenses,
                                            currentUid = user.uid,
                                            inWorkspace = state.workspace != null,
                                            isOwner = isOwner,
                                            ownerName = ownerName,
                                            isRetainedReadOnly = isRetained,
                                            sharedVehicleExpenses = sharedVehExpenses,
                                            onBack = { navController.popBackStack() },
                                            onOpenOdometerSheet = {
                                                activeSheet = ActiveSheet.OdometerUpdate(vehicle.id)
                                            },
                                            onOpenServiceItemSheet = { svc ->
                                                activeSheet = ActiveSheet.ServiceItemDetail(vehicle.id, svc.id)
                                            },
                                            onOpenAddServiceItem = {
                                                activeSheet = ActiveSheet.AddServiceItem(vehicle.id)
                                            },
                                            onOpenAddExpense = { sub ->
                                                val route = if (sub != null) {
                                                    "add?cat=veh&sub=$sub&veh=${vehicle.id}"
                                                } else {
                                                    "add?cat=veh&veh=${vehicle.id}"
                                                }
                                                navController.navigate(route)
                                            },
                                            onOpenExpenseDetail = { exp ->
                                                activeSheet = ActiveSheet.ExpenseDetail(exp)
                                            },
                                            onOpenSharedExpenseDetail = { se ->
                                                activeSheet = ActiveSheet.SharedExpenseDetail(se)
                                            },
                                            onToggleHiddenFromWorkspace = {
                                                if (ownVehicle != null) viewModel.toggleVehicleHiddenFromWorkspace(ownVehicle)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { navController.popBackStack() }
                                    }
                                }

                                composable(
                                    route = ROUTE_PROJECT,
                                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                                ) { entry ->
                                    val pid = entry.arguments?.getString("projectId").orEmpty()
                                    val ownProject = state.projects.firstOrNull { it.id == pid }
                                    val sharedProject = state.sharedProjects.firstOrNull { it.id == pid && it.ownerUid != user.uid }
                                    val retainedProject = state.retainedProjects.firstOrNull { it.id == pid }
                                    val project = ownProject
                                        ?: sharedProject?.toProject(includeOwnerDetails = true)
                                        ?: retainedProject?.toProject(includeOwnerDetails = false)
                                    val isOwner = ownProject != null
                                    val isRetained = !isOwner && sharedProject == null && retainedProject != null
                                    val ownerName = sharedProject?.ownerName ?: retainedProject?.ownerName
                                    val memberContribs = when {
                                        isOwner -> state.sharedProjectContributions.filter { it.sharedProjectId == "${user.uid}_${pid}" }
                                        sharedProject != null -> state.sharedProjectContributions.filter { it.sharedProjectId == pid }
                                        isRetained -> state.retainedProjectContributions.filter { it.sharedProjectId == pid && it.ownerUid == user.uid }
                                        else -> emptyList()
                                    }
                                    val projSharedExps = when {
                                        isOwner -> state.sharedExpenses.filter { it.sharedProjectId == "${user.uid}_${pid}" && it.ownerUid != user.uid }
                                        sharedProject != null -> state.sharedExpenses.filter { it.sharedProjectId == pid }
                                        isRetained -> state.retainedExpenses.filter { it.sharedProjectId == pid && it.ownerUid == user.uid }
                                        else -> emptyList()
                                    }
                                    val projPersonalExps = state.expenses.filter {
                                        it.projectId == pid || (isOwner && it.projectId == "${user.uid}_${pid}")
                                    }
                                    if (project != null) {
                                        ProjectDetailScreen(
                                            project = project,
                                            currentUid = user.uid,
                                            inWorkspace = state.workspace != null,
                                            isOwner = isOwner,
                                            ownerName = ownerName,
                                            isRetainedReadOnly = isRetained,
                                            memberContributions = memberContribs,
                                            projectPersonalExpenses = projPersonalExps,
                                            projectSharedExpenses = projSharedExps,
                                            onBack = { navController.popBackStack() },
                                            onOpenAddPayment = {
                                                activeSheet = ActiveSheet.AddProjectPayment(project.id)
                                            },
                                            onUpdateProjectProducts = { updatedProducts ->
                                                if (ownProject != null) viewModel.updateProjectProducts(ownProject, updatedProducts)
                                            },
                                            onToggleHiddenFromWorkspace = {
                                                if (ownProject != null) viewModel.toggleProjectHiddenFromWorkspace(ownProject)
                                            },
                                            onDeleteContribution = viewModel::deleteSharedProjectContribution,
                                            onOpenExpenseDetail = { exp ->
                                                activeSheet = ActiveSheet.ExpenseDetail(exp)
                                            },
                                            onOpenSharedExpenseDetail = { se ->
                                                activeSheet = ActiveSheet.SharedExpenseDetail(se)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { navController.popBackStack() }
                                    }
                                }

                                composable(
                                    route = ROUTE_ADD,
                                    arguments = listOf(
                                        navArgument("cat") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("sub") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("veh") { type = NavType.StringType; nullable = true; defaultValue = null },
                                        navArgument("editId") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    )
                                ) { entry ->
                                    val editId = entry.arguments?.getString("editId")
                                    val editingExpense = editId?.let { id -> state.expenses.firstOrNull { it.id == id } }
                                    AddExpenseScreen(
                                        categories = state.effectiveCategories,
                                        vehicles = state.pickerVehicles,
                                        projects = state.pickerProjects,
                                        initialCategoryId = entry.arguments?.getString("cat"),
                                        initialSubcategory = entry.arguments?.getString("sub"),
                                        initialVehicleId = entry.arguments?.getString("veh"),
                                        editingExpense = editingExpense,
                                        isSaving = isSaving,
                                        inWorkspace = state.workspace != null,
                                        onBack = { navController.popBackStack() },
                                        onSwitchToScan = {
                                            viewModel.resetScan()
                                            navController.navigate(ROUTE_SCAN) {
                                                popUpTo(ROUTE_ADD) { inclusive = true }
                                            }
                                        },
                                        onShowToast = viewModel::showToast,
                                        onRestoreStarterCategories = viewModel::restoreStarterCategories,
                                        onSave = { expense, veh, odo, doneIds, proj ->
                                            if (editingExpense != null) {
                                                viewModel.updateExpense(expense, veh, odo, doneIds, proj) {
                                                    navController.popBackStack()
                                                }
                                            } else {
                                                viewModel.addExpense(expense, veh, odo, doneIds, proj) {
                                                    navController.popBackStack()
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(ROUTE_SCAN) {
                                    ScanBillScreen(
                                        scanState = scanState,
                                        categories = state.effectiveCategories,
                                        vehicles = state.pickerVehicles,
                                        isSaving = isSaving,
                                        onBack = { navController.popBackStack() },
                                        onResetScan = viewModel::resetScan,
                                        onScanSample = viewModel::scanSampleGarageBill,
                                        onScanFromUri = { uri -> viewModel.scanFromUri(context, uri) },
                                        onScanBitmap = viewModel::scanBitmap,
                                        onUpdateDraft = viewModel::updateScanDraft,
                                        onDeleteItem = viewModel::deleteScannedItem,
                                        onEditItem = { item ->
                                            val draft = (scanState as? com.example.ui.viewmodel.ScanUiState.Review)?.draft
                                            val veh = state.pickerVehicles.firstOrNull { it.id == draft?.selectedVehicleId }
                                            activeSheet = ActiveSheet.EditScannedItem(item, veh)
                                        },
                                        onSaveBill = { draft ->
                                            viewModel.saveScannedBill(draft, state.pickerVehicles) { linkedVehicleId ->
                                                if (linkedVehicleId != null && draft.suggestedCategoryId == "veh") {
                                                    navController.navigate("vehicle/$linkedVehicleId") {
                                                        popUpTo(ROUTE_SCAN) { inclusive = true }
                                                    }
                                                } else {
                                                    navController.popBackStack()
                                                }
                                            }
                                        },
                                        onShowToast = viewModel::showToast
                                    )
                                }

                                composable(ROUTE_ACTIVITY) {
                                    ActivityScreen(
                                        categories = state.effectiveCategories,
                                        expenses = state.expenses,
                                        currentUid = user.uid,
                                        workspace = state.workspace,
                                        sharedExpenses = state.sharedExpenses,
                                        onBack = { navController.popBackStack() },
                                        onOpenExpenseDetail = { exp ->
                                            activeSheet = ActiveSheet.ExpenseDetail(exp)
                                        },
                                        onOpenSharedExpenseDetail = { shared ->
                                            activeSheet = ActiveSheet.SharedExpenseDetail(shared)
                                        },
                                        onShowToast = viewModel::showToast
                                    )
                                }

                                composable(ROUTE_CATEGORIES) {
                                    CategoriesAndSettingsScreen(
                                        profile = state.profile,
                                        user = user,
                                        categories = state.categories,
                                        sharedCategories = state.sharedCategories,
                                        expenses = state.expenses,
                                        themeMode = themeMode,
                                        reviewDraftCount = reviewDrafts.size,
                                        smsAutoReadEnabled = smsAutoReadEnabled,
                                        workspace = state.workspace,
                                        onBack = { navController.popBackStack() },
                                        onChangeLanguage = viewModel::setLanguage,
                                        onChangeThemeMode = onChangeThemeMode,
                                        onEditBudget = {
                                            activeSheet = ActiveSheet.EditMonthlyBudget(state.profile.monthlyBudget)
                                        },
                                        onAddCategory = { activeSheet = ActiveSheet.CategoryEdit(null) },
                                        onEditCategory = { cat -> activeSheet = ActiveSheet.CategoryEdit(cat) },
                                        onSignOut = { signOutUser(credentialManager, scope, draftStore) },
                                        onShowToast = viewModel::showToast,
                                        onUpdateDisplayName = viewModel::updateDisplayName,
                                        onRestoreCategories = viewModel::restoreStarterCategories,
                                        onClearAllData = viewModel::clearAllDataFresh,
                                        onDeleteAccount = { viewModel.deleteAccount(credentialManager) },
                                        onCheckForUpdates = {
                                            onIgnoreUpdateVersion(null)
                                            updateController.checkForUpdate(ignoredVersionCode = null) {
                                                viewModel.showToast("RecapSpends is up to date")
                                            }
                                        },
                                        onOpenReview = { navController.navigate(ROUTE_REVIEW) },
                                        onOpenWorkspace = { navController.navigate(ROUTE_WORKSPACE) }
                                    )
                                }

                                composable(ROUTE_REVIEW) {
                                    ReviewDraftsScreen(
                                        drafts = reviewDrafts,
                                        categories = state.effectiveCategories,
                                        vehicles = state.pickerVehicles,
                                        projects = state.pickerProjects,
                                        smsAutoReadEnabled = smsAutoReadEnabled,
                                        smsSinceEpochMs = smsSinceEpochMs,
                                        disclosureAccepted = disclosureAccepted,
                                        inWorkspace = state.workspace != null,
                                        isSaving = isSaving,
                                        onBack = { navController.popBackStack() },
                                        onAcceptDisclosureAndEnable = { viewModel.acceptSmsDisclosureAndEnable(context) },
                                        onToggleSmsAutoRead = { enabled -> viewModel.setSmsAutoReadEnabled(enabled, context) },
                                        onSyncTodaySms = { viewModel.syncTodaySms(context, userTriggered = true) },
                                        onAutoSyncTodaySms = { viewModel.syncTodaySms(context, userTriggered = false) },
                                        onUpdateDraft = viewModel::updateReviewDraft,
                                        onApproveDraft = { draft -> viewModel.approveReviewDraft(draft) },
                                        onDeleteDraft = viewModel::deleteReviewDraft,
                                        onShowToast = viewModel::showToast
                                    )
                                }

                                composable(ROUTE_WORKSPACE) {
                                    WorkspaceScreen(
                                        currentUid = user.uid,
                                        workspace = state.workspace,
                                        isSaving = isSaving,
                                        onBack = { navController.popBackStack() },
                                        onCreateWorkspace = viewModel::createWorkspace,
                                        onJoinWorkspace = viewModel::joinWorkspace,
                                        onRegenerateCode = viewModel::regenerateInviteCode,
                                        onRemoveMember = viewModel::removeWorkspaceMember,
                                        onLeaveWorkspace = viewModel::leaveWorkspace,
                                        onDeleteWorkspace = viewModel::deleteWorkspace,
                                        onShowToast = viewModel::showToast
                                    )
                                }
                            }

                            activeSheet?.let { sheet ->
                                SelavuModalSheetHost(
                                    activeSheet = sheet,
                                    categories = state.effectiveCategories,
                                    expenses = state.expenses,
                                    vehicles = state.pickerVehicles,
                                    projects = state.pickerProjects,
                                    currentUid = user.uid,
                                    retainedVehicleIds = state.retainedVehicleIds,
                                    retainedProjectIds = state.retainedProjectIds,
                                    reviewDraftCount = reviewDrafts.size,
                                    inWorkspace = state.workspace != null,
                                    onDismiss = { activeSheet = null },
                                    onNavigateScan = {
                                        viewModel.resetScan()
                                        navController.navigate(ROUTE_SCAN)
                                    },
                                    onNavigateAddExpense = { cat, sub ->
                                        val route = buildString {
                                            append("add")
                                            if (cat != null) {
                                                append("?cat=$cat")
                                                if (sub != null) append("&sub=$sub")
                                            }
                                        }
                                        navController.navigate(route)
                                    },
                                    onNavigateReview = { navController.navigate(ROUTE_REVIEW) },
                                    onNavigateWorkspace = { navController.navigate(ROUTE_WORKSPACE) },
                                    onOpenAddVehicle = { activeSheet = ActiveSheet.AddVehicle },
                                    onOpenNewProject = { activeSheet = ActiveSheet.AddProject },
                                    onOpenNewReminder = { activeSheet = ActiveSheet.NewBillReminder },
                                    onUpdateOdometer = viewModel::updateVehicleOdometer,
                                    onMarkServiceDone = viewModel::markServiceItemDone,
                                    onRecordLastServiceDone = viewModel::recordLastServiceDone,
                                    onUpdateServiceInterval = viewModel::updateServiceItemInterval,
                                    onDeleteServiceItem = viewModel::deleteServiceItem,
                                    onAddServiceItem = viewModel::addServiceItem,
                                    onCreateVehicle = { name, type, plate, odo, mileage, hiddenFromWorkspace ->
                                        viewModel.addVehicle(name, type, plate, odo, mileage, hiddenFromWorkspace) { newId ->
                                            navController.navigate("vehicle/$newId")
                                        }
                                    },
                                    onCreateBill = viewModel::addBill,
                                    onUpdateBill = viewModel::updateBill,
                                    onDeleteBill = viewModel::deleteBill,
                                    onCreateProject = { name, budget, tpl, hiddenFromWorkspace ->
                                        viewModel.addProject(name, budget, tpl, hiddenFromWorkspace) { newId ->
                                            navController.navigate("project/$newId")
                                        }
                                    },
                                    onAddProjectPayment = viewModel::addProjectPayment,
                                    onSaveCategory = viewModel::saveCategory,
                                    onDeleteCategory = { catId -> viewModel.deleteCategory(catId, state.expenses) },
                                    onToggleExpensePrivacy = viewModel::toggleExpensePrivacy,
                                    onEditExpense = { exp ->
                                        activeSheet = null
                                        navController.navigate("add?editId=${exp.id}")
                                    },
                                    onDeleteExpense = viewModel::deleteExpense,
                                    onDeleteRetainedExpense = viewModel::deleteRetainedSharedExpense,
                                    onUpdateScannedItem = viewModel::updateScannedItem,
                                    onUpdateBudget = viewModel::updateBudget,
                                    onShowToast = viewModel::showToast
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareGstCsv(
    context: Context,
    expenses: List<Expense>,
    categories: List<Category>
) {
    val csv = com.example.util.CsvExporter.exportExpensesCsv(
        expenses = expenses.sortedByDescending { it.dateEpochMs },
        categories = categories
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "RecapSpends Expense & GST Report")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export GST CSV for CA"))
}
