package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JournalEntry
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val allEntries by viewModel.allJournalEntries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Financial Journal", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Entry", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        if (allEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = GreyText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your Journal is Empty", color = WhiteText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Write down your thoughts on your spending", color = GreyText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(allEntries, key = { it.id }) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onDelete = { viewModel.deleteJournalEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddJournalEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, content ->
                viewModel.addJournalEntry(JournalEntry(date = date, content = content))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = entry.date, color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GreyText, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = entry.content, color = WhiteText, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this journal entry?", color = GreyText) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = RubyExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = GreyText) }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddJournalEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, content: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateStr = datePickerState.selectedDateMillis?.let { 
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) 
    } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val isValid = content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Journal Entry", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
                ) {
                    Text("Date: $dateStr")
                }
                
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    placeholder = { Text("Write your thoughts...", color = GreyText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dateStr, content.trim()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
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
