package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.currentMonthTransactions.collectAsState()

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<String?>(null) }
    var inputLimitVal by remember { mutableStateOf("") }



    // Map each expense category to currently computed spent totals inside select month
    val categorySpentMap = remember(transactions) {
        transactions.filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Monthly Budgets", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_budget")) {
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
                .padding(horizontal = 16.dp)
        ) {
            // Context header displaying the active month
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Budgets below are automatically tracked for the current month: ${com.example.ui.screens.getFormattedMonthName(selectedMonth)}",
                        fontSize = 13.sp,
                        color = WhiteText
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(Category.EXPENSES, key = { it.name }) { cat ->
                    val limitBudget = budgets.firstOrNull { it.category.equals(cat.name, true) }
                    val spent = categorySpentMap[cat.name] ?: 0.0
                    val limitVal = limitBudget?.monthlyLimit ?: 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .clickable {
                                selectedCategoryForEdit = cat.name
                                inputLimitVal = if (limitVal > 0.0) limitVal.toString() else ""
                                showEditBudgetDialog = true
                            }
                            .testTag("budget_row_${cat.name}"),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = cat.icon, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = cat.name, fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 15.sp)
                                }

                                if (limitVal > 0.0) {
                                    IconButton(
                                        onClick = {
                                            if (limitBudget != null) {
                                                viewModel.deleteBudget(limitBudget.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("btn_delete_budget_${cat.name}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Limit", tint = RubyExpense, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Amount Spent", style = MaterialTheme.typography.bodySmall, color = GreyText)
                                    Text(text = CurrencyFormatter.format(spent), fontWeight = FontWeight.SemiBold, color = WhiteText)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Budget Limit", style = MaterialTheme.typography.bodySmall, color = GreyText)
                                    Text(
                                        text = if (limitVal > 0.0) CurrencyFormatter.format(limitVal) else "No Limit set",
                                        fontWeight = FontWeight.Bold,
                                        color = if (limitVal > 0) TealPrimary else GreyText
                                    )
                                }
                            }

                            if (limitVal > 0.0) {
                                val progress = (spent / limitVal).toFloat().coerceIn(0f..1f)
                                val progressPercent = (spent / limitVal * 100).toInt()
                                val warningThreshold = spent >= (limitVal * 0.8)

                                val progressAnim by animateFloatAsState(
                                    targetValue = progress,
                                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
                                    label = "budget_limit_progress"
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Progress tracker indicator bar
                                val trackCol = if (spent > limitVal) RubyExpense else if (warningThreshold) AmberWarning else TealPrimary
                                LinearProgressIndicator(
                                    progress = { progressAnim },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = trackCol,
                                    trackColor = DarkSurfaceElevated,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$progressPercent% limit reached",
                                        fontSize = 11.sp,
                                        color = GreyText
                                    )

                                    if (spent > limitVal) {
                                        Text(
                                            text = "EXCEEDED LIMIT!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RubyExpense
                                        )
                                    } else if (warningThreshold) {
                                        Text(
                                            text = "80% threshold exceeded!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberWarning
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

    // Modal popup editing prompting Dialogue
    if (showEditBudgetDialog && selectedCategoryForEdit != null) {
        val editingCategory = selectedCategoryForEdit!!

        AlertDialog(
            onDismissRequest = {
                showEditBudgetDialog = false
                selectedCategoryForEdit = null
            },
            title = {
                Text(text = "Modify $editingCategory Budget", color = WhiteText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Set a monthly spending restriction target limit (Rs. or $):", color = GreyText, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = inputLimitVal,
                        onValueChange = { inputLimitVal = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_budget_limit"),
                        placeholder = { Text("e.g. 1000", color = GreyText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputLimit = inputLimitVal.toDoubleOrNull()
                        if (inputLimit != null && inputLimit >= 0) {
                            viewModel.saveBudget(editingCategory, inputLimit)
                        }
                        showEditBudgetDialog = false
                        selectedCategoryForEdit = null
                    },
                    modifier = Modifier.testTag("btn_confirm_edit_budget"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
                ) {
                    Text("Confirm Limit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditBudgetDialog = false
                        selectedCategoryForEdit = null
                    },
                    modifier = Modifier.testTag("btn_cancel_edit_budget")
                ) {
                    Text("Cancel", color = GreyText)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
