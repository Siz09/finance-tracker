package com.example

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.ui.screens.*
import com.example.ui.screens.settings.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceEvent
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

data class TabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as FinanceApplication
            val viewModel: FinanceViewModel = viewModel(
                factory = FinanceViewModel.Factory(app.repository)
            )

            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            @Suppress("DEPRECATION")
            LaunchedEffect(isDark) {
                val color = if (isDark) {
                    android.graphics.Color.parseColor("#0B0813") // Matches DarkBgVal
                } else {
                    android.graphics.Color.parseColor("#F5F5F7") // Matches LightBgVal
                }
                window.statusBarColor = color
                window.navigationBarColor = color

                val decorView = window.decorView
                val wic = WindowCompat.getInsetsController(window, decorView)
                wic.isAppearanceLightStatusBars = !isDark
                wic.isAppearanceLightNavigationBars = !isDark
            }

            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

            MyApplicationTheme(darkTheme = isDark) {
                if (hasCompletedOnboarding != null) {
                    val startDest = remember(hasCompletedOnboarding) {
                        if (intent?.action == "com.example.ACTION_ADD_TRANSACTION") {
                            "add_transaction"
                        } else if (hasCompletedOnboarding == false) {
                            "onboarding"
                        } else {
                            "dashboard"
                        }
                    }
                    MainAppContainer(viewModel = viewModel, startDestination = startDest)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(com.example.ui.theme.DarkBg))
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
    val fluidEaseOut = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    val fluidEaseIn = remember { CubicBezierEasing(0.2f, 0f, 0f, 1f) }
    val duration = 380

    val detailsEnter: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { (it * 0.18f).toInt() },
            animationSpec = tween(duration, easing = fluidEaseOut)
        ) + fadeIn(animationSpec = tween(duration))
    }

    val detailsExit: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -(it * 0.18f).toInt() },
            animationSpec = tween(duration, easing = fluidEaseOut)
        ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
    }

    val detailsPopEnter: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -(it * 0.18f).toInt() },
            animationSpec = tween(duration, easing = fluidEaseOut)
        ) + fadeIn(animationSpec = tween(duration))
    }

    val detailsPopExit: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { (it * 0.18f).toInt() },
            animationSpec = tween(duration, easing = fluidEaseOut)
        ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
    }
    val routeIndex = remember { mapOf("dashboard" to 0, "transactions" to 1, "settings" to 2) }
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
        TabItem("transactions", "History", Icons.AutoMirrored.Filled.ListAlt, "tab_transactions"),
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
                fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
            }
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onComplete = {
                        viewModel.completeOnboarding()
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "dashboard",
                enterTransition = {
                    val initialRoute = initialState.destination.route ?: ""
                    val initialIdx = routeIndex[initialRoute]
                    val targetIdx = routeIndex["dashboard"] ?: 0
                    
                    if (initialIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        }
                    } else if (initialRoute.startsWith("settings_") ||
                        initialRoute == "reports" ||
                        initialRoute == "net_worth"
                    ) {
                        slideInHorizontally(
                            initialOffsetX = { -(it * 0.18f).toInt() },
                            animationSpec = tween(duration, easing = fluidEaseOut)
                        ) + fadeIn(animationSpec = tween(duration))
                    } else if (initialRoute == "add_transaction" ||
                        initialRoute.startsWith("edit_transaction")
                    ) {
                        scaleIn(initialScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeIn(animationSpec = tween(duration))
                    } else {
                        fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
                    }
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route ?: ""
                    val targetIdx = routeIndex[targetRoute]
                    val initialIdx = routeIndex["dashboard"] ?: 0
                    
                    if (targetIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        } else {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        }
                    } else if (targetRoute.startsWith("settings_") ||
                        targetRoute == "reports" ||
                        targetRoute == "net_worth"
                    ) {
                        slideOutHorizontally(
                            targetOffsetX = { -(it * 0.18f).toInt() },
                            animationSpec = tween(duration, easing = fluidEaseOut)
                        ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                    } else if (targetRoute == "add_transaction" ||
                        targetRoute.startsWith("edit_transaction")
                    ) {
                        scaleOut(targetScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeOut(animationSpec = tween(duration))
                    } else {
                        fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                    }
                },
                popEnterTransition = {
                    val initialRoute = initialState.destination.route ?: ""
                    if (initialRoute == "add_transaction" ||
                        initialRoute.startsWith("edit_transaction")
                    ) {
                        scaleIn(initialScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeIn(animationSpec = tween(duration))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -(it * 0.18f).toInt() },
                            animationSpec = tween(duration, easing = fluidEaseOut)
                        ) + fadeIn(animationSpec = tween(duration))
                    }
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                }
            ) {
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
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }
            composable(
                route = "transactions",
                enterTransition = {
                    val initialRoute = initialState.destination.route ?: ""
                    val initialIdx = routeIndex[initialRoute]
                    val targetIdx = routeIndex["transactions"] ?: 1
                    
                    if (initialIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        }
                    } else if (initialRoute == "add_transaction" ||
                        initialRoute.startsWith("edit_transaction")
                    ) {
                        scaleIn(initialScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeIn(animationSpec = tween(duration))
                    } else {
                        fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
                    }
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route ?: ""
                    val targetIdx = routeIndex[targetRoute]
                    val initialIdx = routeIndex["transactions"] ?: 1
                    
                    if (targetIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        } else {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        }
                    } else if (targetRoute == "add_transaction" ||
                        targetRoute.startsWith("edit_transaction")
                    ) {
                        scaleOut(targetScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeOut(animationSpec = tween(duration))
                    } else {
                        fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                    }
                },
                popEnterTransition = {
                    val initialRoute = initialState.destination.route ?: ""
                    if (initialRoute == "add_transaction" ||
                        initialRoute.startsWith("edit_transaction")
                    ) {
                        scaleIn(initialScale = 0.96f, animationSpec = tween(duration, easing = fluidEaseOut)) +
                        fadeIn(animationSpec = tween(duration))
                    } else {
                        fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
                    }
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                }
            ) {
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
            composable(
                route = "settings",
                enterTransition = {
                    val initialRoute = initialState.destination.route ?: ""
                    val initialIdx = routeIndex[initialRoute]
                    val targetIdx = routeIndex["settings"] ?: 2
                    
                    if (initialIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeIn(animationSpec = tween(duration))
                        }
                    } else if (initialRoute.startsWith("settings_") ||
                        initialRoute == "reports" ||
                        initialRoute == "net_worth"
                    ) {
                        slideInHorizontally(
                            initialOffsetX = { -(it * 0.18f).toInt() },
                            animationSpec = tween(duration, easing = fluidEaseOut)
                        ) + fadeIn(animationSpec = tween(duration))
                    } else {
                        fadeIn(animationSpec = tween(duration, easing = fluidEaseOut)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(duration, easing = fluidEaseOut))
                    }
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route ?: ""
                    val targetIdx = routeIndex[targetRoute]
                    val initialIdx = routeIndex["settings"] ?: 2
                    
                    if (targetIdx != null) {
                        if (targetIdx > initialIdx) {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        } else {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(duration, easing = fluidEaseOut)
                            ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                        }
                    } else if (targetRoute.startsWith("settings_") ||
                        targetRoute == "reports" ||
                        targetRoute == "net_worth"
                    ) {
                        slideOutHorizontally(
                            targetOffsetX = { -(it * 0.18f).toInt() },
                            animationSpec = tween(duration, easing = fluidEaseOut)
                        ) + fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                    } else {
                        fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                    }
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -(it * 0.18f).toInt() },
                        animationSpec = tween(duration, easing = fluidEaseOut)
                    ) + fadeIn(animationSpec = tween(duration))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(240, easing = fluidEaseIn))
                }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }},
                    onNavigateToBudget = { navController.navigate("settings_budget") },
                    onNavigateToSavings = { navController.navigate("settings_savings") },
                    onNavigateToDebt = { navController.navigate("settings_debt") },
                    onNavigateToNotifications = { navController.navigate("settings_notifications") },
                    onNavigateToBackup = { navController.navigate("settings_backup") },
                    onNavigateToAccounts = { navController.navigate("settings_accounts") },
                    onNavigateToReports = { navController.navigate("reports") },
                    onNavigateToNetWorth = { navController.navigate("net_worth") },
                    onNavigateToCalendar = { navController.navigate("settings_calendar") },
                    onNavigateToBills = { navController.navigate("bills") }
                )
            }
            composable(
                route = "add_transaction",
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(380, easing = fluidEaseOut)
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(340, easing = fluidEaseOut)
                    ) + fadeOut(animationSpec = tween(180))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(380, easing = fluidEaseOut)
                    ) + fadeOut(animationSpec = tween(220))
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
                        animationSpec = tween(380, easing = fluidEaseOut)
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(340, easing = fluidEaseOut)
                    ) + fadeOut(animationSpec = tween(180))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(380, easing = fluidEaseOut)
                    ) + fadeOut(animationSpec = tween(220))
                }
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                TransactionFormScreen(
                    viewModel = viewModel,
                    transactionId = id,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_budget",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                BudgetScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_savings",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                SavingsGoalScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_debt",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                DebtScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_notifications",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                NotificationsSettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_backup",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                BackupScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_accounts",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                AccountsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "reports",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                ReportsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "net_worth",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                NetWorthScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings_calendar",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                CalendarScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditTransaction = { id -> navController.navigate("edit_transaction/$id") },
                    onNavigateToJournal = { navController.navigate("journal") }
                )
            }
            composable(
                route = "bills",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                BillsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "journal",
                enterTransition = detailsEnter,
                exitTransition = detailsExit,
                popEnterTransition = detailsPopEnter,
                popExitTransition = detailsPopExit
            ) {
                JournalScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
