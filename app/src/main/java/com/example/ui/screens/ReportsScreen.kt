package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    // ── Calculations ──
    val expenses = remember(transactions) { transactions.filter { it.type == "expense" } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    val categorySums = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val biggestExpense = remember(expenses) {
        expenses.maxByOrNull { it.amount }
    }

    val dailyAverage = remember(expenses, selectedMonth) {
        if (expenses.isEmpty()) 0.0
        else {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(selectedMonth) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            totalExpense / daysInMonth
        }
    }

    // MoM comparison calculation
    val previousMonthStr = remember(selectedMonth) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = sdf.parse(selectedMonth) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.MONTH, -1)
        }
        sdf.format(cal.time)
    }

    val previousMonthExpenses = remember(allTransactions, previousMonthStr) {
        allTransactions.filter { it.date.startsWith(previousMonthStr) && it.type == "expense" }
    }

    val previousMonthExpense = remember(previousMonthExpenses) {
        previousMonthExpenses.sumOf { it.amount }
    }

    val percentageChange = remember(totalExpense, previousMonthExpense) {
        if (previousMonthExpense == 0.0) {
            if (totalExpense > 0.0) 100.0 else 0.0
        } else {
            ((totalExpense - previousMonthExpense) / previousMonthExpense) * 100.0
        }
    }

    // Per-category MoM comparison data
    data class CategoryComparison(
        val category: String,
        val thisMonth: Double,
        val lastMonth: Double,
        val percentChange: Double
    )

    val categoryComparisons = remember(categorySums, previousMonthExpenses) {
        val lastByCategory = previousMonthExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Include all categories that appear in either month
        val allCategories = (categorySums.map { it.first } + lastByCategory.keys).distinct()

        allCategories.map { cat ->
            val thisAmt = categorySums.firstOrNull { it.first == cat }?.second ?: 0.0
            val lastAmt = lastByCategory[cat] ?: 0.0
            val change = if (lastAmt == 0.0) {
                if (thisAmt > 0.0) 100.0 else 0.0
            } else {
                ((thisAmt - lastAmt) / lastAmt) * 100.0
            }
            CategoryComparison(cat, thisAmt, lastAmt, change)
        }.sortedByDescending { it.thisMonth }
    }

    // 6-month historical spending trend data
    val historicalTrendData = remember(allTransactions, selectedMonth) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfLabel = SimpleDateFormat("MMM", Locale.getDefault())
        val baseDate = sdf.parse(selectedMonth) ?: Date()
        val cal = Calendar.getInstance().apply { time = baseDate }
        // Go back 5 months so we get 6 data points including current
        cal.add(Calendar.MONTH, -5)
        (0 until 6).map {
            val monthStr = sdf.format(cal.time)
            val label = try { sdfLabel.format(cal.time) } catch (e: Exception) { monthStr }
            val total = allTransactions
                .filter { tx -> tx.date.startsWith(monthStr) && tx.type == "expense" }
                .sumOf { it.amount }
            cal.add(Calendar.MONTH, 1)
            Pair(label, total)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Spending Reports", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = TealPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Analytics Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        val formattedMonth = remember(selectedMonth) {
                            val sdfIn = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            try {
                                sdfOut.format(sdfIn.parse(selectedMonth)!!)
                            } catch (e: Exception) {
                                selectedMonth
                            }
                        }
                        Text(
                            text = "Analysis for $formattedMonth",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreyText
                        )
                    }
                }
            }

            // ── 6-Month Spending Trend Line Chart ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "6-MONTH SPENDING TREND",
                            fontSize = 11.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    MonthlyTrendLineChart(
                        data = historicalTrendData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Daily Average", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("Rs. %.2f", dailyAverage),
                            color = WhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "vs. Prev Month", color = GreyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isIncrease = percentageChange > 0
                            Icon(
                                imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isIncrease) RubyExpense else MintIncome,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f%%", Math.abs(percentageChange)),
                                color = if (isIncrease) RubyExpense else MintIncome,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Biggest expense card
            biggestExpense?.let { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "BIGGEST SINGLE EXPENSE", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Category.getIcon(tx.category, "expense"),
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = tx.category, fontWeight = FontWeight.Bold, color = WhiteText)
                                    Text(text = tx.note ?: "No description", color = GreyText, fontSize = 12.sp)
                                }
                            }
                            Text(
                                text = String.format("Rs. %.2f", tx.amount),
                                color = RubyExpense,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 50/30/20 Rule Analysis
            val totalIncome = incomeTransactions.sumOf { it.amount }
            if (totalIncome > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "50/30/20 RULE ANALYSIS", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        val needsCategories = listOf("Groceries", "Housing", "Bills & Utilities", "Transport", "Health", "Fuel", "EMI")
                        val wantsCategories = listOf("Dining", "Entertainment", "Shopping", "Food", "Drink", "Alcohol", "Travel", "Personal Care")
                        val savingsCategories = listOf("Investment", "Savings", "Transfer")

                        val needsSpent = expenseTransactions.filter { needsCategories.contains(it.category) }.sumOf { it.amount }
                        val wantsSpent = expenseTransactions.filter { wantsCategories.contains(it.category) }.sumOf { it.amount }
                        val savingsSpent = expenseTransactions.filter { savingsCategories.contains(it.category) }.sumOf { it.amount } // Assuming some expenses are categorized as investments

                        val needsPct = (needsSpent / totalIncome) * 100
                        val wantsPct = (wantsSpent / totalIncome) * 100
                        val savingsPct = ((totalIncome - expenseTransactions.sumOf { it.amount } + savingsSpent) / totalIncome) * 100 // Approximation

                        val targetNeeds = 50.0
                        val targetWants = 30.0
                        val targetSavings = 20.0

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Needs", color = WhiteText, fontWeight = FontWeight.Bold)
                                Text("${needsPct.toInt()}%", color = if (needsPct > targetNeeds) RubyExpense else MintIncome, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Target: 50%", color = GreyText, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Wants", color = WhiteText, fontWeight = FontWeight.Bold)
                                Text("${wantsPct.toInt()}%", color = if (wantsPct > targetWants) RubyExpense else MintIncome, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Target: 30%", color = GreyText, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Savings", color = WhiteText, fontWeight = FontWeight.Bold)
                                Text("${savingsPct.toInt()}%", color = if (savingsPct < targetSavings) RubyExpense else MintIncome, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Target: 20%", color = GreyText, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Bar
                        Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                            if (needsPct > 0) Box(modifier = Modifier.weight(needsPct.toFloat().coerceAtLeast(0.1f)).fillMaxHeight().background(Color(0xFFE63946)))
                            if (wantsPct > 0) Box(modifier = Modifier.weight(wantsPct.toFloat().coerceAtLeast(0.1f)).fillMaxHeight().background(Color(0xFFF4A261)))
                            if (savingsPct > 0) Box(modifier = Modifier.weight(savingsPct.toFloat().coerceAtLeast(0.1f)).fillMaxHeight().background(Color(0xFF2A9D8F)))
                        }
                    }
                }
            }

            // Category Breakdown Chart/List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "CATEGORY SPENDING BREAKDOWN", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)

                    if (categorySums.isEmpty()) {
                        Text(
                            text = "No expenses recorded this month.",
                            color = GreyText,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        categorySums.forEach { (cat, amt) ->
                            val pct = if (totalExpense > 0.0) (amt / totalExpense) else 0.0
                            val animatedPct by animateFloatAsState(
                                targetValue = pct.toFloat(),
                                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                                label = "bar_$cat"
                            )
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = Category.getIcon(cat, "expense"), fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, fontWeight = FontWeight.Bold, color = WhiteText)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = String.format("Rs. %.2f", amt), color = WhiteText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = String.format("%.1f%%", pct * 100), color = GreyText, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { animatedPct },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = TealPrimary,
                                    trackColor = DarkSurfaceElevated,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // ── Month-over-Month Per-Category Comparison ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "MONTH-OVER-MONTH COMPARISON",
                            fontSize = 11.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val formattedPrev = remember(previousMonthStr) {
                        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val sdfOut = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                        try { sdfOut.format(sdfIn.parse(previousMonthStr)!!) } catch (e: Exception) { previousMonthStr }
                    }
                    val formattedCur = remember(selectedMonth) {
                        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val sdfOut = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                        try { sdfOut.format(sdfIn.parse(selectedMonth)!!) } catch (e: Exception) { selectedMonth }
                    }

                    if (categoryComparisons.isEmpty()) {
                        Text(
                            text = "No expense data to compare.",
                            color = GreyText,
                            modifier = Modifier.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Column headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Category", fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(text = formattedPrev, fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                            Text(text = formattedCur, fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                            Text(text = "Change", fontSize = 10.sp, color = GreyText, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                        }
                        HorizontalDivider(color = DarkSurfaceElevated, modifier = Modifier.padding(vertical = 6.dp))

                        categoryComparisons.forEach { comp ->
                            val isIncrease = comp.percentChange > 0
                            val isNew = comp.lastMonth == 0.0 && comp.thisMonth > 0.0
                            val changeColor = when {
                                isNew -> TealPrimary
                                isIncrease -> RubyExpense
                                else -> MintIncome
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category name + icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = Category.getIcon(comp.category, "expense"), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comp.category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WhiteText,
                                        maxLines = 1
                                    )
                                }
                                // Last month amount
                                Text(
                                    text = if (comp.lastMonth > 0) String.format("%.0f", comp.lastMonth) else "—",
                                    fontSize = 12.sp,
                                    color = GreyText,
                                    modifier = Modifier.width(72.dp)
                                )
                                // This month amount
                                Text(
                                    text = if (comp.thisMonth > 0) String.format("%.0f", comp.thisMonth) else "—",
                                    fontSize = 12.sp,
                                    color = WhiteText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(72.dp)
                                )
                                // % change badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(56.dp)
                                ) {
                                    if (isNew) {
                                        Text(
                                            text = "NEW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TealPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp
                                                          else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = changeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = String.format("%.0f%%", Math.abs(comp.percentChange)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = changeColor
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = DarkSurfaceElevated.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendLineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = remember(data) {
        val max = data.maxOf { it.second }
        if (max <= 0.0) 1000.0 else max * 1.15
    }

    var animTriggered by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "trend_line_anim"
    )
    LaunchedEffect(data) {
        animTriggered = false
        kotlinx.coroutines.delay(80)
        animTriggered = true
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val spacingX = if (data.size > 1) w / (data.size - 1).toFloat() else w
            val padBottom = 24.dp.toPx()
            val padTop = 16.dp.toPx()
            val chartH = h - padBottom - padTop

            val points = data.mapIndexed { idx, pair ->
                val x = idx * spacingX
                val y = h - padBottom - ((pair.second / maxAmount) * chartH * progress).toFloat()
                Offset(x, y)
            }

            // Horizontal grid lines
            for (i in 0..3) {
                val gy = padTop + chartH * i / 3f
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, gy), end = Offset(w, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Build smooth cubic bezier path
            val linePath = Path()
            val fillPath = Path()
            if (points.isNotEmpty()) {
                linePath.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, h - padBottom)
                fillPath.lineTo(points[0].x, points[0].y)
                for (i in 0 until points.size - 1) {
                    val cx = points[i].x + (points[i + 1].x - points[i].x) / 2f
                    linePath.cubicTo(cx, points[i].y, cx, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                    fillPath.cubicTo(cx, points[i].y, cx, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                }
                fillPath.lineTo(points.last().x, h - padBottom)
                fillPath.close()
            }

            // Gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TealPrimary.copy(alpha = 0.20f), Color.Transparent),
                    startY = padTop, endY = h - padBottom
                )
            )
            // Line stroke
            drawPath(
                path = linePath,
                color = TealPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dots and labels
            points.forEachIndexed { idx, pt ->
                drawCircle(color = TealPrimary.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = pt)
                drawCircle(color = TealPrimary, radius = 3.5.dp.toPx(), center = pt)

                val amountStr = if (data[idx].second >= 1000)
                    String.format("%.1fk", data[idx].second / 1000) else
                    String.format("%.0f", data[idx].second)

                if (progress >= 0.95f) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            amountStr, pt.x, pt.y - 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 9.sp.toPx()
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        data[idx].first, pt.x, h - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#88FFFFFF")
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
