package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val allBills by viewModel.allBills.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Bill?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Bills & Subscriptions", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Bill", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        if (allBills.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = GreyText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Bills Yet", color = WhiteText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap + to add your first bill or subscription", color = GreyText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(allBills, key = { it.id }) { bill ->
                    BillCard(
                        bill = bill,
                        onTogglePaid = {
                            viewModel.updateBill(bill.copy(isPaid = !bill.isPaid))
                        },
                        onDelete = { showDeleteConfirmDialog = bill }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBillDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, date ->
                viewModel.addBill(Bill(name = name, amount = amount, dueDate = date))
                showAddDialog = false
            }
        )
    }

    showDeleteConfirmDialog?.let { bill ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Bill", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${bill.name}'?", color = GreyText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBill(bill.id)
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
fun BillCard(bill: Bill, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bill.isPaid,
                        onCheckedChange = { onTogglePaid() },
                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary, uncheckedColor = GreyText, checkmarkColor = DarkBg)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = bill.name,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.isPaid) GreyText else WhiteText,
                            fontSize = 16.sp,
                            textDecoration = if (bill.isPaid) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        )
                        Text(text = "Due: ${bill.dueDate}", color = if (bill.isPaid) GreyText else AmberWarning, fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyFormatter.format(bill.amount),
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isPaid) GreyText else TealPrimary,
                        fontSize = 16.sp,
                        textDecoration = if (bill.isPaid) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GreyText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, dueDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Default to today if no date is picked
    val dateStr = datePickerState.selectedDateMillis?.let { 
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) 
    } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val isValid = name.isNotBlank() && amountStr.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Bill / Subscription", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Bill Name (e.g. Netflix)", color = GreyText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Amount (Rs.)", color = GreyText) },
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
                    Text("Due Date: $dateStr")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), amountStr.toDouble(), dateStr) },
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
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = GreyText) }
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
