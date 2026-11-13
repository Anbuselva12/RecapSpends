package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import com.example.ui.screens.ScanBillScreen
import com.example.ui.screens.SelavuModalSheetHost
import com.example.ui.screens.TrackScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.theme.SelavuTheme
import com.example.ui.viewmodel.AppDataState
import com.example.ui.viewmodel.SelavuViewModel
import com.example.util.EnglishStrings
import com.example.util.IndianFormatters
import com.example.util.LocalSelavuStrings
import com.example.util.TamilStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.selavuPrefs by preferencesDataStore(name = "selavu_prefs")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

private const val ROUTE_HOME = "home"
private const val ROUTE_TRACK = "track"
private const val ROUTE_BILLS = "bills"
private const val ROUTE_INSIGHTS = "insights"
private const val ROUTE_VEHICLE = "vehicle/{vehicleId}"
private const val ROUTE_PROJECT = "project/{projectId}"
private const val ROUTE_ADD = "add?cat={cat}&sub={sub}&veh={veh}"
private const val ROUTE_SCAN = "scan"
private const val ROUTE_ACTIVITY = "activity"
private const val ROUTE_CATEGORIES = "categories"

class MainActivity : ComponentActivity() {
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

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
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
                            onChangeThemeMode = { mode ->
                                scope.launch {
                                    context.selavuPrefs.edit { it[KEY_THEME_MODE] = mode }
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
    onChangeThemeMode: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SelavuViewModel = viewModel(
        key = user.uid,
        factory = SelavuViewModel.factory(context, user.uid, user.displayName)
    )
    val dataState by viewModel.appState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

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

    val showBottomBar = currentRoute in setOf(ROUTE_HOME, ROUTE_TRACK, ROUTE_BILLS, ROUTE_INSIGHTS)

    CompositionLocalProvider(LocalSelavuStrings provides activeStrings) {
        val colors = SelavuTheme.colors
        Scaffold(
            containerColor = colors.bg,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(innerPadding)
            ) {
                when (val state = dataState) {
                    is AppDataState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Syncing RecapSpends…", style = MaterialTheme.typography.bodyMedium, color = colors.ink2)
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
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink2
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { signOutUser(credentialManager, scope) }
                            ) {
                                Text("Sign out")
                            }
                        }
                    }

                    is AppDataState.Ready -> {
                        val alerts = remember(state.vehicles, state.bills, state.categories) {
                            viewModel.computeAttentionAlerts(state.vehicles, state.bills, state.categories)
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    monthlyBudget = state.profile.monthlyBudget,
                                    categories = state.categories,
                                    expenses = state.expenses,
                                    alerts = alerts,
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
                                    }
                                )
                            }

                            composable(ROUTE_TRACK) {
                                TrackScreen(
                                    vehicles = state.vehicles,
                                    projects = state.projects,
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
                                    categories = state.categories,
                                    onMarkPaid = { bill -> viewModel.markBillPaid(bill, state.categories) },
                                    onOpenNewReminder = { activeSheet = ActiveSheet.NewBillReminder },
                                    onOpenProfile = { navController.navigate(ROUTE_CATEGORIES) }
                                )
                            }

                            composable(ROUTE_INSIGHTS) {
                                InsightsScreen(
                                    monthlyBudget = state.profile.monthlyBudget,
                                    categories = state.categories,
                                    expenses = state.expenses,
                                    vehicles = state.vehicles,
                                    onOpenCategoryDrilldown = { cat ->
                                        activeSheet = ActiveSheet.CategoryDrilldown(cat)
                                    },
                                    onExportGstCsv = {
                                        shareGstCsv(context, state.expenses, state.categories)
                                    },
                                    onOpenProfile = { navController.navigate(ROUTE_CATEGORIES) }
                                )
                            }

                            composable(
                                route = ROUTE_VEHICLE,
                                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
                            ) { entry ->
                                val vid = entry.arguments?.getString("vehicleId").orEmpty()
                                val vehicle = state.vehicles.firstOrNull { it.id == vid }
                                if (vehicle != null) {
                                    VehicleDetailScreen(
                                        vehicle = vehicle,
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
                                        onOpenLogVisit = {
                                            navController.navigate("add?cat=veh&sub=Service&veh=${vehicle.id}")
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
                                val project = state.projects.firstOrNull { it.id == pid }
                                if (project != null) {
                                    ProjectDetailScreen(
                                        project = project,
                                        onBack = { navController.popBackStack() },
                                        onOpenAddPayment = {
                                            activeSheet = ActiveSheet.AddProjectPayment(project.id)
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
                                    navArgument("veh") { type = NavType.StringType; nullable = true; defaultValue = null }
                                )
                            ) { entry ->
                                AddExpenseScreen(
                                    categories = state.categories,
                                    vehicles = state.vehicles,
                                    projects = state.projects,
                                    initialCategoryId = entry.arguments?.getString("cat"),
                                    initialSubcategory = entry.arguments?.getString("sub"),
                                    initialVehicleId = entry.arguments?.getString("veh"),
                                    isSaving = isSaving,
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
                                        viewModel.addExpense(expense, veh, odo, doneIds, proj) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_SCAN) {
                                ScanBillScreen(
                                    scanState = scanState,
                                    categories = state.categories,
                                    vehicles = state.vehicles,
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
                                        val veh = state.vehicles.firstOrNull { it.id == draft?.selectedVehicleId }
                                        activeSheet = ActiveSheet.EditScannedItem(item, veh)
                                    },
                                    onSaveBill = { draft ->
                                        viewModel.saveScannedBill(draft, state.vehicles) { linkedVehicleId ->
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
                                    categories = state.categories,
                                    expenses = state.expenses,
                                    onBack = { navController.popBackStack() },
                                    onOpenExpenseDetail = { exp ->
                                        activeSheet = ActiveSheet.ExpenseDetail(exp)
                                    }
                                )
                            }

                            composable(ROUTE_CATEGORIES) {
                                CategoriesAndSettingsScreen(
                                    profile = state.profile,
                                    user = user,
                                    categories = state.categories,
                                    expenses = state.expenses,
                                    themeMode = themeMode,
                                    onBack = { navController.popBackStack() },
                                    onChangeLanguage = viewModel::setLanguage,
                                    onChangeThemeMode = onChangeThemeMode,
                                    onEditBudget = {
                                        activeSheet = ActiveSheet.EditMonthlyBudget(state.profile.monthlyBudget)
                                    },
                                    onAddCategory = { activeSheet = ActiveSheet.CategoryEdit(null) },
                                    onEditCategory = { cat -> activeSheet = ActiveSheet.CategoryEdit(cat) },
                                    onSignOut = { signOutUser(credentialManager, scope) },
                                    onShowToast = viewModel::showToast,
                                    onUpdateDisplayName = viewModel::updateDisplayName,
                                    onSendPasswordReset = viewModel::sendPasswordReset,
                                    onRestoreCategories = viewModel::restoreStarterCategories,
                                    onClearAllData = viewModel::clearAllDataFresh,
                                    onDeleteAccount = { viewModel.deleteAccount(credentialManager) }
                                )
                            }
                        }

                        activeSheet?.let { sheet ->
                            SelavuModalSheetHost(
                                activeSheet = sheet,
                                categories = state.categories,
                                expenses = state.expenses,
                                vehicles = state.vehicles,
                                projects = state.projects,
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
                                onOpenAddVehicle = { activeSheet = ActiveSheet.AddVehicle },
                                onOpenNewProject = { activeSheet = ActiveSheet.AddProject },
                                onOpenNewReminder = { activeSheet = ActiveSheet.NewBillReminder },
                                onUpdateOdometer = viewModel::updateVehicleOdometer,
                                onMarkServiceDone = viewModel::markServiceItemDone,
                                onUpdateServiceInterval = viewModel::updateServiceItemInterval,
                                onDeleteServiceItem = viewModel::deleteServiceItem,
                                onAddServiceItem = viewModel::addServiceItem,
                                onCreateVehicle = { name, type, plate, odo, mileage ->
                                    viewModel.addVehicle(name, type, plate, odo, mileage) { newId ->
                                        navController.navigate("vehicle/$newId")
                                    }
                                },
                                onCreateBill = viewModel::addBill,
                                onCreateProject = { name, budget, tpl ->
                                    viewModel.addProject(name, budget, tpl) { newId ->
                                        navController.navigate("project/$newId")
                                    }
                                },
                                onAddProjectPayment = viewModel::addProjectPayment,
                                onSaveCategory = viewModel::saveCategory,
                                onDeleteCategory = { catId -> viewModel.deleteCategory(catId, state.expenses) },
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

private fun shareGstCsv(
    context: Context,
    expenses: List<Expense>,
    categories: List<Category>
) {
    val csv = buildString {
        appendLine("Date,Merchant,Category,Subcategory,PaymentMode,BillNumber,GSTIN,TaxableValue,CGST,SGST,IGST,RoundOff,GrandTotal")
        expenses.sortedByDescending { it.dateEpochMs }.forEach { e ->
            val catName = categories.firstOrNull { it.id == e.categoryId }?.name ?: e.categoryId
            fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""
            appendLine(
                listOf(
                    esc(IndianFormatters.formatDate(e.dateEpochMs)),
                    esc(e.title),
                    esc(catName),
                    esc(e.subcategory),
                    esc(e.paymentMode),
                    esc(e.billNumber.orEmpty()),
                    esc(e.gstin.orEmpty()),
                    e.taxableValue?.toString().orEmpty(),
                    e.cgst?.toString().orEmpty(),
                    e.sgst?.toString().orEmpty(),
                    e.igst?.toString().orEmpty(),
                    e.roundOff?.toString().orEmpty(),
                    e.amount.toString()
                ).joinToString(",")
            )
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "RecapSpends Expense & GST Report")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export GST CSV for CA"))
}
