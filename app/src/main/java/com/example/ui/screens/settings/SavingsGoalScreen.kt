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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavingsGoal
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val allGoals by viewModel.allSavingsGoals.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalForProgress by remember { mutableStateOf<SavingsGoal?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = TealPrimary)
                    }
                }
            )
        },
        containerColor = DarkBg
    ) { padding ->
        if (allGoals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalActivity, contentDescription = null, tint = GreyText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Savings Goals Yet", color = WhiteText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap + to add your first goal", color = GreyText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(allGoals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onAddProgress = { selectedGoalForProgress = goal },
                        onDelete = { showDeleteConfirmDialog = goal }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target, deadline, autoCredit ->
                viewModel.addSavingsGoal(SavingsGoal(name = name, target = target, deadline = deadline, autoCreditEnabled = autoCredit))
                showAddDialog = false
            }
        )
    }

    selectedGoalForProgress?.let { goal ->
        AddProgressDialog(
            goal = goal,
            onDismiss = { selectedGoalForProgress = null },
            onConfirm = { addedAmount ->
                viewModel.updateSavingsGoal(goal.copy(savedAmount = goal.savedAmount + addedAmount))
                selectedGoalForProgress = null
            }
        )
    }

    showDeleteConfirmDialog?.let { goal ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Goal", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${goal.name}'?", color = GreyText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSavingsGoal(goal.id)
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
fun SavingsGoalCard(goal: SavingsGoal, onAddProgress: () -> Unit, onDelete: () -> Unit) {
    val progressRatio = (goal.savedAmount / goal.target).coerceIn(0.0, 1.0)
    val animatedProgress by animateFloatAsState(targetValue = progressRatio.toFloat(), animationSpec = tween(800), label = "progress")
    val isCompleted = progressRatio >= 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MintIncome.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Star, contentDescription = null, tint = MintIncome, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = goal.name, fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (goal.deadline != null) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text(text = "Target Date: ${sdf.format(Date(goal.deadline))}", color = GreyText, fontSize = 12.sp)
                        } else {
                            Text(text = "No deadline", color = GreyText, fontSize = 12.sp)
                        }
                        if (goal.autoCreditEnabled) {
                            Text(text = "Pay-yourself-first Active (10% of income)", color = MintIncome, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GreyText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = CurrencyFormatter.format(goal.savedAmount), fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 18.sp)
                Text(text = "of ${CurrencyFormatter.format(goal.target)}", color = GreyText, fontSize = 14.sp, modifier = Modifier.align(Alignment.Bottom))
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) MintIncome else TealPrimary,
                trackColor = DarkSurfaceElevated,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddProgress,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleted,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary.copy(alpha = 0.1f), contentColor = TealPrimary)
            ) {
                Text(if (isCompleted) "Goal Achieved!" else "Add Funds", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, deadline: Long?, autoCredit: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    var autoCreditEnabled by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && targetStr.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it }, // 100-char cap (#14)
                    label = { Text("Goal Name (e.g. Vacation)", color = GreyText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr, onValueChange = { targetStr = it },
                    label = { Text("Target Amount (Rs.)", color = GreyText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
                ) {
                    val dateStr = datePickerState.selectedDateMillis?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Set Deadline (Optional)"
                    Text(dateStr)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pay Yourself First", color = WhiteText, fontWeight = FontWeight.SemiBold)
                        Text("Auto-credit 10% of new income to this goal", color = GreyText, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoCreditEnabled,
                        onCheckedChange = { autoCreditEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealPrimary.copy(alpha = 0.5f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), targetStr.toDouble(), datePickerState.selectedDateMillis, autoCreditEnabled) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreyText) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK", color = TealPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false; datePickerState.selectedDateMillis = null }) { Text("Clear", color = GreyText) }
            },
            colors = DatePickerDefaults.colors(containerColor = DarkSurface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = WhiteText,
                    headlineContentColor = TealPrimary,
                    weekdayContentColor = GreyText,
                    dayContentColor = WhiteText,
                    selectedDayContentColor = DarkBg,
                    selectedDayContainerColor = TealPrimary
                )
            )
        }
    }
}

@Composable
private fun AddProgressDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onConfirm: (addedAmount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    val isValid = amountStr.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to ${goal.name}", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Current: ${CurrencyFormatter.format(goal.savedAmount)} / ${CurrencyFormatter.format(goal.target)}", color = GreyText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Amount to add (Rs.)", color = GreyText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
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
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
            ) { Text("Add Funds", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreyText) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
