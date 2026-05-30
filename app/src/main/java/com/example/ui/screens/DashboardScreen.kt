package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import java.util.Calendar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransaction: (Int) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val savingsGoal by viewModel.savingsGoal.collectAsState()

    var showSeeAllSheet by remember { mutableStateOf(false) }

    val recentLogs = remember(allTransactions) {
        allTransactions.filter { tx ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val txDate = sdf.parse(tx.date) ?: return@filter false
                
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                
                val limitCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                !txDate.before(limitCal.time) && !txDate.after(todayCal.time)
            } catch (e: Exception) {
                false
            }
        }.sortedByDescending { it.date }
    }

    val expenseTransactions = remember(transactions) { transactions.filter { it.type == "expense" } }
    val categorySums = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val totalExpenseSum = remember(categorySums) { categorySums.values.sum() }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    val yearsList = listOf("2023", "2024", "2025", "2026", "2027", "2028")
    val currentYear = remember(selectedMonth) { selectedMonth.take(4) }
    val currentMonthIndex = remember(selectedMonth) { selectedMonth.substring(5).toIntOrNull()?.minus(1) ?: 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DarkBg).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Month Selector Ribbon (Paradigm B - Collapsible & Expandable with zero click highlight & spring physics)
            item {
                var isExpanded by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        // Top Row: Info Title & Expansion Toggle Button (With completely disabled click highlight/indication)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null // Completely removes any touch splash/ripple highlights
                                ) {
                                    isExpanded = !isExpanded
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rotationState by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "caret_rotation"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = getFormattedMonthName(selectedMonth),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WhiteText
                                    )
                                    Text(
                                        text = if (isExpanded) "Tap to collapse" else "Tap to change month",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GreyText
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer(rotationZ = rotationState)
                                )
                            }

                            // Year dropdown trigger (Only visible when expanded)
                            if (isExpanded) {
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                            .clickable { yearMenuExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = currentYear,
                                            color = TealPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Year",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = yearMenuExpanded,
                                        onDismissRequest = { yearMenuExpanded = false },
                                        modifier = Modifier.background(DarkSurfaceElevated)
                                    ) {
                                        yearsList.forEach { yr ->
                                            DropdownMenuItem(
                                                text = { Text(text = yr, color = WhiteText, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    yearMenuExpanded = false
                                                    val monthStr = String.format("%02d", currentMonthIndex + 1)
                                                    viewModel.selectedMonth.value = "$yr-$monthStr"
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Calendar visual icon in collapsed state
                                Box(
                                    modifier = Modifier
                                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Expand Calendar Selector",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Smooth expandable slide and fade animation for Month Ribbon
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(animationSpec = tween(150, delayMillis = 80)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(animationSpec = tween(100))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Horizontally scrollable Months Ribbon
                                val monthsList = listOf(
                                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                                )
                                val listState = rememberLazyListState()

                                // Auto-scroll selected month to the center when selectedMonth changes
                                LaunchedEffect(currentMonthIndex) {
                                    listState.animateScrollToItem(maxOf(0, currentMonthIndex - 2))
                                }

                                LazyRow(
                                    state = listState,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(monthsList) { idx, mth ->
                                        val isSelected = idx == currentMonthIndex
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) TealPrimary else DarkSurfaceElevated,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    val monthStr = String.format("%02d", idx + 1)
                                                    viewModel.selectedMonth.value = "$currentYear-$monthStr"
                                                    isExpanded = false // Collapse card on selection
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mth,
                                                color = if (isSelected) DarkBg else WhiteText,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Balance card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("net_balance_card"),
                    colors = CardDefaults.cardColors(containerColor = LavenderAccentCard),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedMonth,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)).togetherWith(
                                fadeOut(animationSpec = tween(220))
                            )
                        },
                        label = "balance_card_animation"
                    ) { _ ->
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = "NET BALANCE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ActivePillText.copy(alpha = 0.6f), letterSpacing = 1.2.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyFormatter.format(netBalance),
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
                                        color = ActivePillText
                                    )
                                }
                                Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown, contentDescription = null, tint = ActivePillText, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(16.dp)) {
                                    Column {
                                        Text(text = "INCOME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ActivePillText.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = CurrencyFormatter.format(totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeForestGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(16.dp)) {
                                    Column {
                                        Text(text = "EXPENSE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ActivePillText.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = CurrencyFormatter.format(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseWarmRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Savings goal widget on Dashboard
            val goalValue = savingsGoal?.target ?: 0.0
            if (goalValue > 0.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedMonth,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)).togetherWith(
                                    fadeOut(animationSpec = tween(220))
                                )
                            },
                            label = "savings_goal_card_animation"
                        ) { _ ->
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = MintIncome, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Savings Goal — ${getFormattedMonthName(selectedMonth)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WhiteText)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                val progressNorm = if (netBalance <= 0) 0f else (netBalance / goalValue).toFloat().coerceIn(0f, 1f)
                                val progressPct = if (netBalance <= 0) 0 else (netBalance / goalValue * 100).toInt()
                                val progressAnim by animateFloatAsState(
                                    targetValue = progressNorm,
                                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                                    label = "dashboard_savings_progress"
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Saved: ${CurrencyFormatter.format(netBalance.coerceAtLeast(0.0))}", color = GreyText, fontSize = 13.sp)
                                    Text(text = "Target: ${CurrencyFormatter.format(goalValue)}", color = GreyText, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progressAnim },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (netBalance >= goalValue) MintIncome else TealPrimary,
                                    trackColor = DarkSurfaceElevated,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (netBalance >= goalValue) "🎉 Goal achieved! $progressPct%" else "$progressPct% of target reached",
                                    color = if (netBalance >= goalValue) MintIncome else GreyText,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Expense breakdown chart
            item {
                Text(text = "Expense Breakdowns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WhiteText, modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                AnimatedContent(
                    targetState = expenseTransactions,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)).togetherWith(
                            fadeOut(animationSpec = tween(220))
                        )
                    },
                    label = "expense_breakdowns_animation",
                    modifier = Modifier.fillMaxWidth()
                ) { targetExpenses ->
                    if (targetExpenses.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().height(160.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = GreyText, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "No expenses logged this month.", color = GreyText, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                DonutChartSection(categorySums, totalExpenseSum)
                                Spacer(modifier = Modifier.height(16.dp))
                                categorySums.forEach { (cat, sum) ->
                                    val percent = (sum / totalExpenseSum * 100).toInt()
                                    val emoji = Category.getIcon(cat, "expense")
                                    val budgetLimit = budgets.firstOrNull { it.category.equals(cat, true) }?.monthlyLimit
                                    val budgetWarning = budgetLimit != null && sum >= (budgetLimit * 0.8)
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(text = emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = cat, fontWeight = FontWeight.SemiBold, color = WhiteText)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "$percent% of expenses", style = MaterialTheme.typography.bodySmall, color = GreyText)
                                                    if (budgetWarning && budgetLimit != null) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        val badgeColor = if (sum > budgetLimit) RubyExpense else AmberWarning
                                                        Text(text = if (sum > budgetLimit) "Over Limit" else "80% Warning", color = badgeColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                                    }
                                                }
                                            }
                                        }
                                        Text(text = CurrencyFormatter.format(sum), fontWeight = FontWeight.Bold, color = WhiteText)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Logs (Last 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhiteText
                    )
                    Text(
                        text = "See All",
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showSeeAllSheet = true }
                            .testTag("btn_see_all_transactions")
                    )
                }
            }

            item {
                AnimatedContent(
                    targetState = recentLogs,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    }
                ) { targetList ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (targetList.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable { onAddTransactionClick() },
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AddChart, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "No logs in the last 7 days.", color = WhiteText, fontWeight = FontWeight.Bold)
                                    Text(text = "Tap + to log a transaction!", color = GreyText, fontSize = 12.sp)
                                }
                            }
                        } else {
                            val recents = targetList.take(6)
                            recents.forEach { tx ->
                                TransactionCardItem(transaction = tx, onEditClick = { onEditTransaction(tx.id) })
                            }
                        }
                    }
                }
            }
        }

        // Quick-add FAB on Dashboard
        FloatingActionButton(
            onClick = { onAddTransactionClick() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("btn_dashboard_add_transaction"),
            containerColor = TealPrimary,
            contentColor = DarkBg
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }

        // Beautiful Full-Featured See All Bottom Sheet
        if (showSeeAllSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSeeAllSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = DarkBg,
                scrimColor = Color.Black.copy(alpha = 0.65f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = GreyText) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header of sheet
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "All Logs of Month",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = WhiteText
                            )
                            Text(
                                text = getFormattedMonthName(selectedMonth),
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showSeeAllSheet = false },
                            modifier = Modifier.background(DarkSurfaceElevated, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WhiteText)
                        }
                    }

                    var sheetFilterType by remember { mutableStateOf("all") }
                    var sheetSearchQuery by remember { mutableStateOf("") }

                    val sheetFilteredList = remember(transactions, sheetFilterType, sheetSearchQuery) {
                        transactions.filter { tx ->
                            val matchesType = when (sheetFilterType) {
                                "income" -> tx.type == "income"
                                "expense" -> tx.type == "expense"
                                else -> true
                            }
                            val matchesQuery = tx.category.contains(sheetSearchQuery, ignoreCase = true) ||
                                    (tx.note?.contains(sheetSearchQuery, ignoreCase = true) ?: false)
                            matchesType && matchesQuery
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = sheetSearchQuery,
                        onValueChange = { sheetSearchQuery = it },
                        placeholder = { Text(text = "Search logs by note, category...", color = GreyText) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = DarkSurfaceElevated,
                            containerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = GreyText) }
                    )

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterChips = listOf(
                            Triple("all", "All Logs", Icons.Default.List),
                            Triple("income", "Incomes", Icons.Default.TrendingUp),
                            Triple("expense", "Expenses", Icons.Default.TrendingDown)
                        )

                        filterChips.forEach { (chipType, label, icon) ->
                            val isSelected = sheetFilterType == chipType
                            FilterChip(
                                selected = isSelected,
                                onClick = { sheetFilterType = chipType },
                                label = { Text(text = label) },
                                leadingIcon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (chipType == "income") MintIncome else if (chipType == "expense") RubyExpense else TealPrimary,
                                    selectedLabelColor = DarkBg,
                                    selectedLeadingIconColor = DarkBg,
                                    unfocusedContainerColor = DarkSurfaceElevated,
                                    unfocusedLabelColor = WhiteText,
                                    unfocusedLeadingIconColor = GreyText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (sheetFilteredList.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = GreyText, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "No matching records found", color = GreyText, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            items(sheetFilteredList, key = { it.id }) { tx ->
                                TransactionCardItem(
                                    transaction = tx,
                                    onEditClick = {
                                        showSeeAllSheet = false
                                        onEditTransaction(tx.id)
                                    },
                                    modifier = Modifier.animateItemPlacement(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartSection(categorySums: Map<String, Double>, totalExpenseSum: Double) {
    val listColors = listOf(TealPrimary, MintIncome, Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFF219EBC), Color(0xFF8338EC), Color(0xFFFF006E), Color(0xFF3A86C8), Color(0xFF14FFEC), Color(0xFF38EF7D))
    var animTriggered by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "donut_chart_progress"
    )
    LaunchedEffect(categorySums) {
        animTriggered = false
        kotlinx.coroutines.delay(50)
        animTriggered = true
    }

    Box(modifier = Modifier.size(170.dp).padding(12.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var currentAngle = -90f
            var colorIdx = 0
            categorySums.forEach { (_, amount) ->
                val sweep = (amount / totalExpenseSum * 360f).toFloat()
                val color = listColors[colorIdx % listColors.size]
                drawArc(color = color, startAngle = currentAngle, sweepAngle = sweep * progress, useCenter = false, style = Stroke(width = 24.dp.toPx()))
                currentAngle += sweep
                colorIdx++
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
            Text(text = "My Spend", style = MaterialTheme.typography.labelSmall, color = GreyText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit
) {
    val emoji = remember(transaction) { Category.getIcon(transaction.category, transaction.type) }

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEditClick() }.testTag("transaction_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(44.dp).background(DarkSurfaceElevated, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = transaction.category, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = WhiteText)
                        if (transaction.imagePath != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                    // Show note if available, always show date below
                    if (!transaction.note.isNullOrBlank()) {
                        Text(text = transaction.note, style = MaterialTheme.typography.bodySmall, color = GreyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(text = transaction.date, style = MaterialTheme.typography.bodySmall, color = GreyText.copy(alpha = 0.7f))
                }
            }
            Text(
                text = (if (transaction.type == "expense") "- " else "+ ") + CurrencyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (transaction.type == "expense") RubyExpense else MintIncome
            )
        }
    }
}

fun getFormattedMonthName(monthString: String): String {
    return try {
        val sdfSrc = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdfSrc.parse(monthString) ?: return monthString
        val sdfDone = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdfDone.format(date)
    } catch (e: Exception) {
        monthString
    }
}
