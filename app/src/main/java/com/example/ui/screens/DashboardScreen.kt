package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onEditTransaction: (Int) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == "expense" }
    }
    val categorySums = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }
    }
    val totalExpenseSum = remember(categorySums) { categorySums.values.sum() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- 1. Month Selector Header ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectPreviousMonth() },
                        modifier = Modifier.testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TealPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = getFormattedMonthName(selectedMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Text(
                            text = "Monthly Overview",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreyText
                        )
                    }

                    IconButton(
                        onClick = { viewModel.selectNextMonth() },
                        modifier = Modifier.testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TealPrimary
                        )
                    }
                }
            }
        }

        // --- 2. Combined Balance & Income/Expense Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("net_balance_card"),
                colors = CardDefaults.cardColors(containerColor = LavenderAccentCard),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Net Balance Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET BALANCE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ActivePillText.copy(alpha = 0.6f),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormatter.format(netBalance),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = ActivePillText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = ActivePillText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Income / Expense Section side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ActivePillText.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormatter.format(totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeForestGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Expense Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "EXPENSE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ActivePillText.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormatter.format(totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseWarmRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Custom Donut Chart & Category Breakdowns ---
        item {
            Text(
                text = "Expense Breakdowns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhiteText,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (expenseTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = GreyText,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "No expenses logged this month.", color = GreyText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DonutChartSection(categorySums, totalExpenseSum)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category List Detail
                        categorySums.forEach { (cat, sum) ->
                            val percent = (sum / totalExpenseSum * 100).toInt()
                            val emoji = Category.getIcon(cat, "expense")
                            
                            // Check if category has a budget
                            val budgetLimit = budgets.firstOrNull { it.category.equals(cat, true) }?.monthlyLimit
                            val budgetWarning = budgetLimit != null && sum >= (budgetLimit * 0.8)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = cat, fontWeight = FontWeight.SemiBold, color = WhiteText)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$percent% of expenses",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GreyText
                                            )
                                            if (budgetWarning && budgetLimit != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val badgeColor = if (sum > budgetLimit) RubyExpense else AmberWarning
                                                Text(
                                                    text = if (sum > budgetLimit) "Over Limit" else "80% Limit Warning",
                                                    color = badgeColor,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = currencyFormatter.format(sum),
                                    fontWeight = FontWeight.Bold,
                                    color = WhiteText
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Recent Transactions ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Logs (${getFormattedMonthName(selectedMonth)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                Text(
                    text = "See All",
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToTransactions() }
                        .testTag("btn_see_all_transactions")
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { onNavigateToTransactions() },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.AddChart, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No transactions recorded yet.", color = WhiteText, fontWeight = FontWeight.Bold)
                        Text(text = "Tap to start logging your first expense!", color = GreyText, fontSize = 12.sp)
                    }
                }
            }
        } else {
            val recents = transactions.take(4)
            items(recents) { tx ->
                TransactionCardItem(
                    transaction = tx,
                    onEditClick = { onEditTransaction(tx.id) },
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
fun DonutChartSection(categorySums: Map<String, Double>, totalExpenseSum: Double) {
    // Generate distinct colors for segments
    val listColors = listOf(
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

    Box(
        modifier = Modifier
            .size(170.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var currentAngle = -90f
            var colorIdx = 0

            categorySums.forEach { (_, amount) ->
                val sweep = (amount / totalExpenseSum * 360f).toFloat()
                val color = listColors[colorIdx % listColors.size]
                
                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx())
                )
                currentAngle += sweep
                colorIdx++
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DonutLarge,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "My Spend",
                style = MaterialTheme.typography.labelSmall,
                color = GreyText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: Transaction,
    onEditClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val emoji = remember(transaction) {
        Category.getIcon(transaction.category, transaction.type)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEditClick() }
            .testTag("transaction_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji Icon bubble
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = WhiteText
                        )
                        if (transaction.imagePath != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Receipt Attachment attached",
                                tint = TealPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = transaction.note ?: transaction.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = (if (transaction.type == "expense") "- " else "+ ") + currencyFormatter.format(transaction.amount),
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
