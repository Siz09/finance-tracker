package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit,
    onEditTransaction: (Int) -> Unit,
    onNavigateToJournal: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    // Parse the selected month (yyyy-MM)
    val calendar = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        try {
            val parts = selectedMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
        } catch (e: Exception) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        cal
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // Day of the week for day 1 (1 = Sunday, 2 = Monday, etc.)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val monthDisplayName = remember(selectedMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    // Currently selected day in the calendar grid (defaults to today if current month/year, else 1st)
    var selectedDay by remember(selectedMonth) {
        val today = Calendar.getInstance()
        val day = if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month) {
            today.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        mutableIntStateOf(day)
    }

    // Get formatted date string for selected day
    val selectedDateStr = remember(selectedDay, selectedMonth) {
        String.format("%04d-%02d-%02d", year, month + 1, selectedDay)
    }

    // Filter transactions for the selected month to render daily dots/summaries
    val monthTransactions = remember(allTransactions, selectedMonth) {
        allTransactions.filter { it.date.startsWith(selectedMonth) }
    }

    // Filter transactions for the selected day
    val selectedDayTransactions = remember(monthTransactions, selectedDateStr) {
        monthTransactions.filter { it.date == selectedDateStr }
    }

    // Calculate income, expense and net worth summaries for the selected day
    val dailyIncome = remember(selectedDayTransactions) {
        selectedDayTransactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val dailyExpense = remember(selectedDayTransactions) {
        selectedDayTransactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val dailyNet = dailyIncome - dailyExpense

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Cash Flow Calendar", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToJournal) {
                        Icon(Icons.Default.Book, contentDescription = "Journal", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Month Switcher Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { viewModel.selectPreviousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = WhiteText)
                        }
                        Text(
                            text = monthDisplayName,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { viewModel.selectNextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = WhiteText)
                        }
                    }
                }
            }

            // Calendar Month Grid View Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Days of week header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreyText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid math
                        val totalSlots = daysInMonth + (firstDayOfWeek - 1)
                        val numRows = (totalSlots + 6) / 7

                        for (r in 0 until numRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 7) {
                                    val slotIndex = r * 7 + c
                                    val dayNum = slotIndex - (firstDayOfWeek - 2)
                                    val isValidDay = dayNum in 1..daysInMonth

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isValidDay && dayNum == selectedDay) TealPrimary.copy(
                                                    alpha = 0.15f
                                                ) else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isValidDay && dayNum == selectedDay) TealPrimary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = isValidDay) {
                                                if (isValidDay) selectedDay = dayNum
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isValidDay) {
                                            // Compute net flow for this specific day
                                            val dayStr = String.format("%04d-%02d-%02d", year, month + 1, dayNum)
                                            val dayTxs = monthTransactions.filter { it.date == dayStr }
                                            val dayNetFlow = dayTxs.filter { it.type == "income" }.sumOf { it.amount } - 
                                                             dayTxs.filter { it.type == "expense" }.sumOf { it.amount }

                                            val dotColor = when {
                                                dayNetFlow > 0 -> MintIncome
                                                dayNetFlow < 0 -> RubyExpense
                                                else -> Color.Transparent
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    color = if (dayNum == selectedDay) TealPrimary else WhiteText,
                                                    fontWeight = if (dayNum == selectedDay) FontWeight.ExtraBold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                )
                                                if (dotColor != Color.Transparent) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(dotColor, CircleShape)
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

            // Daily Flow Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FLOW SUMMARY · $selectedDateStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreyText,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Day Income", color = GreyText, fontSize = 12.sp)
                                Text(CurrencyFormatter.format(dailyIncome), color = MintIncome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Day Expense", color = GreyText, fontSize = 12.sp)
                                Text(CurrencyFormatter.format(dailyExpense), color = RubyExpense, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Balance", color = GreyText, fontSize = 12.sp)
                                Text(
                                    text = (if (dailyNet >= 0) "+" else "") + CurrencyFormatter.format(dailyNet),
                                    color = if (dailyNet >= 0) MintIncome else RubyExpense,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Transactions Header & List
            item {
                Text(
                    text = "Daily Transactions (${selectedDayTransactions.size})",
                    color = WhiteText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (selectedDayTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No transactions logged on this day", color = GreyText, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(selectedDayTransactions, key = { it.id }) { transaction ->
                    DailyTransactionCardItem(
                        transaction = transaction,
                        onEditClick = { onEditTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTransactionCardItem(
    transaction: Transaction,
    onEditClick: () -> Unit
) {
    val isExpense = transaction.type == "expense"
    val accentColor = if (isExpense) RubyExpense else MintIncome

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (transaction.mood != null) {
                        when (transaction.mood) {
                            "Necessary" -> "😐"
                            "Happy" -> "😊"
                            "Regret" -> "😞"
                            "Impulse" -> "⚡"
                            else -> "💰"
                        }
                    } else if (isExpense) "💸" else "📥",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.SemiBold,
                    color = WhiteText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        color = GreyText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isExpense) "-" else "+") + CurrencyFormatter.format(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 14.sp
                )
                if (!transaction.paymentMethod.isNullOrBlank()) {
                    Text(
                        text = transaction.paymentMethod,
                        color = GreyText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
