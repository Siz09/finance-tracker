package com.example.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DebtItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val allDebts by viewModel.allDebtItems.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<DebtItem?>(null) }
    var sortStrategy by remember { mutableStateOf("Avalanche") }

    val totalDebt = allDebts.sumOf { it.totalAmount }
    val totalPaid = allDebts.sumOf { it.paidAmount }

    val sortedDebts = remember(allDebts, sortStrategy) {
        when (sortStrategy) {
            "Avalanche" -> allDebts.sortedByDescending { it.interestRate }
            "Snowball" -> allDebts.sortedBy { it.totalAmount - it.paidAmount }
            else -> allDebts
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Debt Payoff Tracker", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Debt", tint = RubyExpense)
                    }
                }
            )
        },
        containerColor = DarkBg
    ) { padding ->
        if (allDebts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CreditScore, contentDescription = null, tint = GreyText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Debts Tracked", color = WhiteText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap + to add a loan or credit card", color = GreyText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item {
                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TOTAL DEBT REMAINING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreyText, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(CurrencyFormatter.format(totalDebt - totalPaid), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = RubyExpense)
                            Spacer(modifier = Modifier.height(8.dp))
                            val progressRatio = if (totalDebt > 0) (totalPaid / totalDebt).coerceIn(0.0, 1.0) else 0.0
                            val animatedProgress by animateFloatAsState(targetValue = progressRatio.toFloat(), animationSpec = tween(800), label = "total_progress")
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = RubyExpense,
                                trackColor = DarkSurfaceElevated,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${(progressRatio * 100).toInt()}% Paid Off (${CurrencyFormatter.format(totalPaid)})", fontSize = 12.sp, color = GreyText)
                        }
                    }
                }
                item {
                    // Sorting Strategy
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = sortStrategy == "Avalanche",
                            onClick = { sortStrategy = "Avalanche" },
                            label = { Text("Avalanche (High APR First)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RubyExpense.copy(alpha = 0.2f),
                                selectedLabelColor = RubyExpense
                            )
                        )
                        FilterChip(
                            selected = sortStrategy == "Snowball",
                            onClick = { sortStrategy = "Snowball" },
                            label = { Text("Snowball (Lowest Balance First)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintIncome.copy(alpha = 0.2f),
                                selectedLabelColor = MintIncome
                            )
                        )
                    }
                }
                
                items(sortedDebts, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        onAddPayment = { selectedDebtForPayment = debt },
                        onDelete = { showDeleteConfirmDialog = debt },
                        strategy = sortStrategy
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, total, rate, minPay ->
                viewModel.addDebtItem(DebtItem(name = name, totalAmount = total, interestRate = rate, minPayment = minPay))
                showAddDialog = false
            }
        )
    }

    selectedDebtForPayment?.let { debt ->
        AddPaymentDialog(
            debt = debt,
            onDismiss = { selectedDebtForPayment = null },
            onConfirm = { payment ->
                viewModel.updateDebtItem(debt.copy(paidAmount = debt.paidAmount + payment))
                selectedDebtForPayment = null
            }
        )
    }

    showDeleteConfirmDialog?.let { debt ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Debt", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${debt.name}'?", color = GreyText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDebtItem(debt.id)
                    showDeleteConfirmDialog = null
                }) {
                    Text("Delete", color = RubyExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel", color = GreyText) }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun DebtCard(debt: DebtItem, onAddPayment: () -> Unit, onDelete: () -> Unit, strategy: String = "Avalanche") {
    val progressRatio = (debt.paidAmount / debt.totalAmount).coerceIn(0.0, 1.0)
    val animatedProgress by animateFloatAsState(targetValue = progressRatio.toFloat(), animationSpec = tween(800), label = "debt_progress")
    val isPaidOff = progressRatio >= 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(if (isPaidOff) MintIncome.copy(alpha = 0.15f) else RubyExpense.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (isPaidOff) Icons.Default.CheckCircle else Icons.Default.MoneyOff, contentDescription = null, tint = if (isPaidOff) MintIncome else RubyExpense, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = debt.name, fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Min Pay: ${CurrencyFormatter.format(debt.minPayment)}/mo | APR: ${debt.interestRate}%", color = GreyText, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GreyText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = CurrencyFormatter.format(debt.totalAmount - debt.paidAmount), fontWeight = FontWeight.Bold, color = if (isPaidOff) MintIncome else RubyExpense, fontSize = 18.sp)
                Text(text = "Remaining", color = GreyText, fontSize = 14.sp, modifier = Modifier.align(Alignment.Bottom))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isPaidOff) MintIncome else RubyExpense,
                trackColor = DarkSurfaceElevated,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddPayment,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPaidOff,
                colors = ButtonDefaults.buttonColors(containerColor = RubyExpense.copy(alpha = 0.1f), contentColor = RubyExpense)
            ) {
                Text(if (isPaidOff) "Paid in Full!" else "Log Payment", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, total: Double, rate: Double, minPay: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var minPayStr by remember { mutableStateOf("") }

    val isValid = name.isNotBlank() && totalStr.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Debt / Loan", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name (e.g. Car Loan)", color = GreyText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyExpense, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalStr, onValueChange = { totalStr = it },
                    label = { Text("Total Amount (Rs.)", color = GreyText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyExpense, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateStr, onValueChange = { rateStr = it },
                        label = { Text("APR %", color = GreyText) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyExpense, unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                            focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minPayStr, onValueChange = { minPayStr = it },
                        label = { Text("Min Pay", color = GreyText) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyExpense, unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                            focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), totalStr.toDouble(), rateStr.toDoubleOrNull() ?: 0.0, minPayStr.toDoubleOrNull() ?: 0.0) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RubyExpense, contentColor = WhiteText)
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreyText) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AddPaymentDialog(
    debt: DebtItem,
    onDismiss: () -> Unit,
    onConfirm: (payment: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    val maxPayable = debt.totalAmount - debt.paidAmount
    val isValid = amountStr.toDoubleOrNull()?.let { it > 0 && it <= maxPayable } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Payment for ${debt.name}", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Remaining Balance: ${CurrencyFormatter.format(maxPayable)}", color = GreyText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Payment Amount (Rs.)", color = GreyText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyExpense, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountStr.toDouble()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RubyExpense, contentColor = WhiteText)
            ) { Text("Log Payment", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreyText) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
