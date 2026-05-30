package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val savingsGoal by viewModel.savingsGoal.collectAsState()
    
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()

    var showEditGoalDialog by remember { mutableStateOf(false) }
    var inputGoalVal by remember { mutableStateOf("") }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val goalValue = savingsGoal?.target ?: 0.0

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Monthly Savings Goals", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_savings")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteText)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target selector banner showing current status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MintIncome.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = MintIncome, modifier = Modifier.size(32.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "CURRENT SAVINGS TARGET", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (goalValue > 0.0) currencyFormatter.format(goalValue) else "Goal not configured yet",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (goalValue > 0.0) MintIncome else GreyText
                        )
                    }

                    Button(
                        onClick = {
                            inputGoalVal = if (goalValue > 0.0) goalValue.toString() else ""
                            showEditGoalDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .testTag("btn_configure_savings_goal"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (goalValue > 0.0) "Modify Savings target" else "Configure Savings Goal", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Calculations panel: Income - Expenses = Net Savings (versus Target)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Savings Progress status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WhiteText)

                    Divider(color = DarkSurfaceElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Income (+) ", color = GreyText)
                        Text(text = currencyFormatter.format(totalIncome), color = MintIncome, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Expense (-) ", color = GreyText)
                        Text(text = currencyFormatter.format(totalExpense), color = RubyExpense, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = DarkSurfaceElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Current Net Balance", fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 15.sp)
                            Text(text = "Auto Calculated Net Savings", fontSize = 11.sp, color = GreyText)
                        }
                        Text(
                            text = currencyFormatter.format(netBalance),
                            color = if (netBalance >= 0.0) TealPrimary else RubyExpense,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    if (goalValue > 0.0) {
                        val progressNormalized = if (netBalance <= 0) 0f else (netBalance / goalValue).toFloat().coerceIn(0f..1f)
                        val progressPercent = if (netBalance <= 0) 0 else (netBalance / goalValue * 100).toInt()

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Achieved: $progressPercent% of savings target!",
                            fontSize = 13.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Bold
                        )

                        LinearProgressIndicator(
                            progress = { progressNormalized },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = MintIncome,
                            trackColor = DarkSurfaceElevated,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        if (netBalance >= goalValue) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MintIncome.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MintIncome, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Nice! You achieved your savings target!", color = MintIncome, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Goal configurator dialogue
    if (showEditGoalDialog) {
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            title = {
                Text(text = "Configure Savings Goal target", color = WhiteText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify a target currency amount that you intend to save this month (Rs. or $):",
                        color = GreyText,
                        fontSize = 13.sp
                    )

                     OutlinedTextField(
                        value = inputGoalVal,
                        onValueChange = { inputGoalVal = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_savings_target"),
                        placeholder = { Text("e.g. 5000", color = GreyText) },
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
                        val inputGoal = inputGoalVal.toDoubleOrNull()
                        if (inputGoal != null && inputGoal >= 0) {
                            viewModel.saveSavingsGoal(inputGoal)
                        }
                        showEditGoalDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_edit_savings"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
                ) {
                    Text("Confirm Target Plan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditGoalDialog = false },
                    modifier = Modifier.testTag("btn_cancel_edit_savings")
                ) {
                    Text("Cancel", color = GreyText)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
