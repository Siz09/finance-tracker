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
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.ui.components.TransactionCardItem
import com.example.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val CategoryColors = listOf(
    TealPrimary,
    MintIncome,
    Color(0xFFFFB703),
    Color(0xFFFB8500),
    Color(0xFF219EBC),
    Color(0xFF8338EC),
    Color(0xFFFF006E),
    Color(0xFF3A86C8),
    Color(0xFF14FFEC),
    Color(0xFF38EF7D)
)

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

    // Recent logs now reflect the dashboard's selected month, not an arbitrary last-7-days window.
    val recentLogs = remember(transactions) {
        transactions.sortedByDescending { it.date }.take(8)
    }

    val dailyTrendPoints = remember(transactions, selectedMonth) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = try { sdf.parse(selectedMonth) } catch(e: Exception) { null }
        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.time = date
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val dayGrouped = transactions.groupBy { tx ->
            try {
                val txDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tx.date)
                val cal = Calendar.getInstance()
                if (txDate != null) {
                    cal.time = txDate
                    cal.get(Calendar.DAY_OF_MONTH)
                } else {
                    1
                }
            } catch(e: Exception) {
                1
            }
        }
        
        var runningSum = 0.0
        val trend = FloatArray(daysInMonth)
        for (day in 1..daysInMonth) {
            val dayTxs = dayGrouped[day] ?: emptyList()
            val netChange = dayTxs.sumOf { if (it.type == "income") it.amount else -it.amount }
            runningSum += netChange
            trend[day - 1] = runningSum.toFloat()
        }
        trend
    }

    val healthScore = remember(totalIncome, totalExpense) {
        if (totalIncome <= 0.0) {
            if (totalExpense > 0.0) 0 else 100
        } else {
            val ratio = (totalIncome - totalExpense) / totalIncome
            val score = (ratio * 100).toInt().coerceIn(0, 100)
            score
        }
    }

    val expenseTransactions = remember(transactions) { transactions.filter { it.type == "expense" } }

    val dailyAverageSpend = remember(expenseTransactions, selectedMonth) {
        if (expenseTransactions.isEmpty()) 0.0
        else {
            val calendar = java.util.Calendar.getInstance()
            val year = selectedMonth.take(4).toIntOrNull() ?: calendar.get(java.util.Calendar.YEAR)
            val month = selectedMonth.drop(5).toIntOrNull()?.minus(1) ?: calendar.get(java.util.Calendar.MONTH)
            calendar.set(year, month, 1)
            val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val totalExpense = expenseTransactions.sumOf { it.amount }
            totalExpense / daysInMonth
        }
    }
    val categorySums = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val totalExpenseSum = remember(categorySums) { categorySums.values.sum() }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(transactions) {
        selectedCategory = null
    }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    val yearsList = listOf("2023", "2024", "2025", "2026", "2027", "2028")
    val currentYear = remember(selectedMonth) { selectedMonth.take(4) }
    val currentMonthIndex = remember(selectedMonth) { selectedMonth.substring(5).toIntOrNull()?.minus(1) ?: 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DarkBg).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.logo),
                            contentDescription = "Kharcha Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "KHARCHA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = WhiteText,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "FINANCE TRACKER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

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
                                        val systemCal = Calendar.getInstance()
                                        val systemYear = systemCal.get(Calendar.YEAR).toString()
                                        val systemMonthIdx = systemCal.get(Calendar.MONTH)
                                        val isSystemTodayMonth = (currentYear == systemYear && idx == systemMonthIdx)

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
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = mth,
                                                    color = if (isSelected) DarkBg else WhiteText,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                if (isSystemTodayMonth) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(color = if (isSelected) DarkBg else TealPrimary, shape = CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Concept 3: The Glassmorphic 2x2 Card Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tile 1: Net Balance
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(TealPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "NET BALANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreyText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = CurrencyFormatter.format(netBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = WhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    
                    // Tile 2: Avg Daily Spend
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE5A93B).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color(0xFFE5A93B), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "AVG DAILY SPEND", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreyText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = CurrencyFormatter.format(dailyAverageSpend), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = WhiteText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tile 3: Total Income
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MintIncome.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MintIncome, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "TOTAL INCOME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreyText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = CurrencyFormatter.format(totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MintIncome, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    
                    // Tile 4: Total Expense
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(RubyExpense.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = RubyExpense, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "TOTAL EXPENSE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreyText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = CurrencyFormatter.format(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RubyExpense, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Concept 2 Premium Savings Index Gauged Dial (Requested by Tri)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Monthly Savings Index",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WhiteText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Fintech Health",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Radial Gauge Composable
                            RadialHealthGauge(
                                score = healthScore,
                                modifier = Modifier.size(110.dp)
                            )
                            
                            // Insights Details on the Right
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val rateText = when {
                                    healthScore >= 70 -> "High Saver"
                                    healthScore >= 40 -> "Balanced spender"
                                    else -> "Heavy spender"
                                }
                                val rateColor = when {
                                    healthScore >= 70 -> MintIncome
                                    healthScore >= 40 -> Color(0xFFFFB703)
                                    else -> RubyExpense
                                }
                                val adviceText = when {
                                    healthScore >= 70 -> "🎉 Amazing job! You are keeping a strong buffer. Consider moving some surplus to savings targets."
                                    healthScore >= 40 -> "⚠️ Spending is balanced. Try cutting minor non-essentials to reach your savings targets faster."
                                    else -> "🚨 Alert! Expenses are high relative to your income. Tap '+' to log a salary or review recent logs."
                                }
                                
                                Column {
                                    Text(
                                        text = "STATUS RATING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreyText
                                    )
                                    Text(
                                        text = rateText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = rateColor
                                    )
                                }
                                
                                Text(
                                    text = adviceText,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = WhiteText.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
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
                                DonutChartSection(
                                    categorySums = categorySums,
                                    totalExpenseSum = totalExpenseSum,
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = { selectedCategory = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                categorySums.keys.forEachIndexed { index, cat ->
                                    val sum = categorySums[cat] ?: 0.0
                                    val color = CategoryColors[index % CategoryColors.size]
                                    val percent = (sum / totalExpenseSum * 100).toInt()
                                    val emoji = Category.getIcon(cat, "expense")
                                    val budgetLimit = budgets.firstOrNull { it.category.equals(cat, true) }?.monthlyLimit
                                    val budgetWarning = budgetLimit != null && sum >= (budgetLimit * 0.8)
                                    val isHighlighted = selectedCategory == cat
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isHighlighted) DarkSurfaceElevated else Color.Transparent)
                                            .clickable { selectedCategory = if (isHighlighted) null else cat }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(color = color, shape = CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = cat, fontWeight = FontWeight.SemiBold, color = if (isHighlighted) TealPrimary else WhiteText)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "$percent% of expenses", style = MaterialTheme.typography.bodySmall, color = GreyText)
                                                    if (budgetWarning) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        val badgeColor = if (sum > (budgetLimit ?: 0.0)) RubyExpense else AmberWarning
                                                        Text(text = if (sum > (budgetLimit ?: 0.0)) "Over Limit" else "80% Warning", color = badgeColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
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

            // ── Smart Spending Insights ────────────────────────────────────────────────────────
            item {
                val insights = remember(transactions, budgets, totalIncome, totalExpense) {
                    val list = mutableListOf<SmartInsight>()

                    // 1. Budget warning per category
                    budgets.forEach { b ->
                        val spent = transactions.filter { it.type == "expense" && it.category.equals(b.category, true) }.sumOf { it.amount }
                        val limit = b.monthlyLimit
                        if (limit > 0) {
                            val ratio = spent / limit
                            if (ratio >= 1.0) list.add(SmartInsight("Budget Exceeded", "${b.category} spending exceeded your Rs.${limit.toInt()} limit (Rs.${spent.toInt()} spent).", "danger"))
                            else if (ratio >= 0.8) list.add(SmartInsight("Budget Warning", "${b.category} is at ${(ratio * 100).toInt()}% of your Rs.${limit.toInt()} limit.", "warning"))
                        }
                    }

                    // 2. High burn rate
                    if (totalIncome > 0 && totalExpense > totalIncome * 0.85) {
                        list.add(SmartInsight("High Burn Rate", "You've spent ${(totalExpense / totalIncome * 100).toInt()}% of your income this month. Consider reviewing non-essentials.", "danger"))
                    }

                    // 3. Positive savings recognition
                    if (totalIncome > 0 && totalExpense < totalIncome * 0.4) {
                        list.add(SmartInsight("Super Saver", "You've saved over 60% of your income this month! Excellent financial discipline.", "success"))
                    }

                    // 4. Concentrated category spike (> 50% of total expense)
                    if (totalExpense > 0) {
                        transactions.filter { it.type == "expense" }.groupBy { it.category }.forEach { (cat, txs) ->
                            val sum = txs.sumOf { it.amount }
                            if (sum > totalExpense * 0.5 && sum > 1000) {
                                list.add(SmartInsight("Spending Spike", "$cat accounts for ${(sum / totalExpense * 100).toInt()}% of this month\'s expenses.", "info"))
                            }
                        }
                    }

                    // 5. No data fallback
                    if (list.isEmpty()) {
                        list.add(SmartInsight("On Track", "No spending anomalies detected. Keep logging to generate deeper insights.", "success"))
                        list.add(SmartInsight("Set Budgets", "Go to Settings › Budgets to set category limits and receive automatic alerts.", "info"))
                    }
                    list
                }

                Text(
                    text = "Smart Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    items(insights) { insight -> SmartInsightCard(insight) }
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
                        text = "Recent — ${getFormattedMonthName(selectedMonth)}",
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
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
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
                            "all" to "All Logs",
                            "income" to "Incomes",
                            "expense" to "Expenses"
                        )

                        filterChips.forEach { (chipType, label) ->
                            val isSelected = sheetFilterType == chipType
                            val containerColor = if (isSelected) {
                                if (chipType == "income") MintIncome else if (chipType == "expense") RubyExpense else TealPrimary
                            } else {
                                DarkSurfaceElevated
                            }
                            val textColor = if (isSelected) DarkBg else WhiteText
                            Box(
                                modifier = Modifier
                                    .background(color = containerColor, shape = RoundedCornerShape(20.dp))
                                    .clickable { sheetFilterType = chipType }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
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
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(
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
fun DonutChartSection(
    categorySums: Map<String, Double>,
    totalExpenseSum: Double,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
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

    Box(modifier = Modifier.size(190.dp).padding(8.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(categorySums, totalExpenseSum, selectedCategory) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius - 32.dp.toPx()

                        if (distance in innerRadius..outerRadius) {
                            var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            angle = (angle + 90f + 360f) % 360f

                            var currentAngle = 0f
                            var clickedCategory: String? = null
                            categorySums.forEach { (cat, amount) ->
                                val sweep = (amount / totalExpenseSum * 360f).toFloat()
                                if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                    clickedCategory = cat
                                }
                                currentAngle += sweep
                            }

                            if (clickedCategory == selectedCategory) {
                                onCategorySelected(null)
                            } else {
                                onCategorySelected(clickedCategory)
                            }
                        } else {
                            onCategorySelected(null)
                        }
                    }
                }
        ) {
            var currentAngle = -90f
            var colorIdx = 0
            categorySums.forEach { (cat, amount) ->
                val sweep = (amount / totalExpenseSum * 360f).toFloat()
                val color = CategoryColors[colorIdx % CategoryColors.size]
                val isSelected = cat == selectedCategory
                val strokeWidth = if (isSelected) 30.dp.toPx() else 22.dp.toPx()

                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweep * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                currentAngle += sweep
                colorIdx++
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            if (selectedCategory != null) {
                val sum = categorySums[selectedCategory] ?: 0.0
                val percent = (sum / totalExpenseSum * 100).toInt()
                Text(text = Category.getIcon(selectedCategory, "expense"), fontSize = 22.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyFormatter.format(sum),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = WhiteText
                )
                Text(text = "$percent%", style = MaterialTheme.typography.labelSmall, color = GreyText)
            } else {
                Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Total Spend", style = MaterialTheme.typography.labelSmall, color = GreyText, fontWeight = FontWeight.Bold)
                Text(
                    text = CurrencyFormatter.format(totalExpenseSum),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = WhiteText
                )
            }
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

@Composable
fun SparklineTrendChart(
    points: FloatArray,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    
    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1f
    val range = (maxVal - minVal).coerceAtLeast(1f)
    
    var animTriggered by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "sparkline_animation"
    )
    
    LaunchedEffect(points) {
        animTriggered = false
        kotlinx.coroutines.delay(50)
        animTriggered = true
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1)
        
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        
        points.forEachIndexed { idx, value ->
            val x = idx * stepX
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * (height - 32.dp.toPx()) + 16.dp.toPx())
            
            // Apply scale animation progress
            val animatedY = height - ((height - y) * progress)
            
            if (idx == 0) {
                path.moveTo(x, animatedY)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, animatedY)
            } else {
                path.lineTo(x, animatedY)
                fillPath.lineTo(x, animatedY)
            }
            
            if (idx == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }
        
        // Draw the glowing fill gradient under the line
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    TealPrimary.copy(alpha = 0.15f),
                    Color.Transparent
                )
            )
        )
        
        // Draw the main curve line
        drawPath(
            path = path,
            color = TealPrimary,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
fun RadialHealthGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    var animTriggered by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animTriggered) score / 100f else 0f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "radial_gauge_progress"
    )
    
    LaunchedEffect(score) {
        animTriggered = false
        kotlinx.coroutines.delay(50)
        animTriggered = true
    }
    
    val color = when {
        score >= 70 -> MintIncome
        score >= 40 -> Color(0xFFFFB703) // Amber
        else -> RubyExpense
    }
    
    val statusText = when {
        score >= 70 -> "Excellent"
        score >= 40 -> "Moderate"
        else -> "Critical"
    }
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(110.dp)) {
            val w = size.width
            val h = size.height
            val strokeW = 10.dp.toPx()
            
            // Draw background track arc
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Draw active arc
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = WhiteText
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

data class SmartInsight(
    val title: String,
    val description: String,
    val type: String // "danger", "warning", "success", "info"
)

@Composable
fun SmartInsightCard(insight: SmartInsight) {
    val (bgColor, iconColor, icon) = when (insight.type) {
        "danger" -> Triple(RubyExpense.copy(alpha = 0.1f), RubyExpense, Icons.Default.Warning)
        "warning" -> Triple(Color(0xFFFB8500).copy(alpha = 0.15f), Color(0xFFFB8500), Icons.Default.Info)
        "success" -> Triple(MintIncome.copy(alpha = 0.1f), MintIncome, Icons.Default.CheckCircle)
        else -> Triple(TealPrimary.copy(alpha = 0.1f), TealPrimary, Icons.Default.Lightbulb)
    }

    Card(
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).background(bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insight.title,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = insight.description,
                color = GreyText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
