package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.screens.settings.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceEvent
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val app = context.applicationContext as FinanceApplication
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.Factory(app.repository)
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
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
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navController.navigate("transactions") },
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") }
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }},
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
                    onNavigateToBackup = { navController.navigate("settings_backup") }
                )
            }
            composable("add_transaction") {
                TransactionFormScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_transaction/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
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
        }
    }
}

data class TabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name! Welcome to Finance Tracker.", modifier = modifier)
}

