package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.ui.screens.*
import com.example.ui.screens.settings.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceEvent
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.BiometricHelper
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val app = context.applicationContext as FinanceApplication
                val viewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModel.Factory(app.repository)
                )

                val isLockEnabled by viewModel.isAppLockEnabled.collectAsState()

                // Trigger biometric authentication if enabled and not yet unlocked
                LaunchedEffect(isLockEnabled) {
                    if (isLockEnabled && !isUnlocked && BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onFailure = { 
                                // Silent retry or wait for user manual unlock tap
                            }
                        )
                    } else {
                        isUnlocked = true
                    }
                }

                if (isUnlocked) {
                    val startDest = remember {
                        if (intent?.action == "com.example.ACTION_ADD_TRANSACTION") "add_transaction" else "dashboard"
                    }
                    MainAppContainer(viewModel = viewModel, startDestination = startDest)
                } else {
                    // Modern premium glassmorphic lock screen
                    Box(
                        modifier = Modifier.fillMaxSize().background(com.example.ui.theme.DarkBg),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = com.example.ui.theme.TealPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.padding(12.dp))
                            Text("Kharcha Locked", color = com.example.ui.theme.WhiteText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(
                                onClick = {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = this@MainActivity,
                                        onSuccess = { isUnlocked = true },
                                        onFailure = {}
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.TealPrimary)
                            ) {
                                Text("Unlock App", color = com.example.ui.theme.DarkBg)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppContainer(viewModel: FinanceViewModel, startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
            viewModel.selectedMonth.value = todayStr
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModel.events.collectLatest { event ->
            when (event) {
                is FinanceEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is FinanceEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val tabs = listOf(
        TabItem("dashboard", "Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
        TabItem("transactions", "History", Icons.Default.ListAlt, "tab_transactions"),
        TabItem("settings", "Settings", Icons.Default.Settings, "tab_settings")
    )

    val showBottomBar = currentRoute in listOf("dashboard", "transactions", "settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_bar"),
                    containerColor = com.example.ui.theme.PaleSurface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.ActivePillText,
                                selectedTextColor = com.example.ui.theme.ActivePillText,
                                indicatorColor = com.example.ui.theme.ActivePill,
                                unselectedIconColor = com.example.ui.theme.TextSecondary,
                                unselectedTextColor = com.example.ui.theme.TextSecondary
                            ),
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(text = tab.label) },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = {
                        navController.navigate("transactions") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddTransactionClick = { navController.navigate("add_transaction") },
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") }
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onAddTransactionClick = { navController.navigate("add_transaction") },
                    onEditTransactionClick = { id -> navController.navigate("edit_transaction/$id") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBackClick = { navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }},
                    onNavigateToBudget = { navController.navigate("settings_budget") },
                    onNavigateToSavings = { navController.navigate("settings_savings") },
                    onNavigateToNotifications = { navController.navigate("settings_notifications") },
                    onNavigateToBackup = { navController.navigate("settings_backup") },
                    onNavigateToAccounts = { navController.navigate("settings_accounts") },
                    onNavigateToReports = { navController.navigate("reports") }
                )
            }
            composable(
                route = "add_transaction",
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                TransactionFormScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_transaction/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                TransactionFormScreen(
                    viewModel = viewModel,
                    transactionId = id,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable("settings_budget") {
                BudgetScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings_savings") {
                SavingsGoalScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings_notifications") {
                NotificationsSettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings_backup") {
                BackupScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings_accounts") {
                AccountsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("reports") {
                ReportsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
