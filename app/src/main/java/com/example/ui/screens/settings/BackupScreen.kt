package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.ExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val savingsGoal by viewModel.savingsGoal.collectAsState()

    // Storage indicator — computed once on composition (fast IO on main thread for display only)
    val receiptsSizeBytes = remember {
        ExportHelper.getReceiptsDirSizeBytes(context)
    }
    val receiptsSizeLabel = remember(receiptsSizeBytes) {
        ExportHelper.formatBytes(receiptsSizeBytes)
    }

    // Import confirmation dialog state
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportFileName by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    
    // Tax Year Selection
    var selectedTaxYear by remember { mutableStateOf<String?>(null) }
    var showTaxYearDropdown by remember { mutableStateOf(false) }
    
    val availableYears = remember(transactions) {
        transactions.map { it.date.take(4) }.distinct().sortedDescending()
    }
    
    LaunchedEffect(availableYears) {
        if (selectedTaxYear == null && availableYears.isNotEmpty()) {
            selectedTaxYear = availableYears.first()
        }
    }

    // JSON file picker launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonContent = inputStream?.bufferedReader()?.use { it.readText() }
            inputStream?.close()

            if (jsonContent.isNullOrBlank()) {
                Toast.makeText(context, "Selected file is empty.", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            // Show preview dialog before committing
            pendingImportFileName = uri.lastPathSegment ?: "backup.json"
            pendingImportJson = jsonContent
            showImportConfirmDialog = true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not read file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Backup & Data Exports", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_backup")) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual header icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(AmberWarning.copy(alpha = 0.12f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Own Your Financial Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                Text(
                    text = "All logs and budgets reside strictly inside your device. We do not use any servers, clouds, or backends. Generate backups locally in multiple open formats below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreyText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = DarkSurfaceElevated)

            // Dynamic records metadata count summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "AVAILABLE LOCAL RECORDS SUMMARY", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Completed Transaction logs:", fontSize = 13.sp, color = WhiteText)
                        Text(text = "${transactions.size} records", fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Category Budget targets:", fontSize = 13.sp, color = WhiteText)
                        Text(text = "${budgets.size} entries", fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 13.sp)
                    }

                    // Receipt image storage size
                    HorizontalDivider(color = DarkBg.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = GreyText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Receipt images on device:", fontSize = 13.sp, color = WhiteText)
                        }
                        Text(
                            text = receiptsSizeLabel,
                            fontWeight = FontWeight.Bold,
                            color = if (receiptsSizeBytes > 50_000_000) AmberWarning else TealPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons to Export CSV/JSON
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "GENERATE OFFLINE FILE EXPORTS", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            if (transactions.isEmpty()) {
                                Toast.makeText(context, "No transactions recorded to export as CSV!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val uri = ExportHelper.exportToCSV(context, transactions)
                            if (uri != null) {
                                ExportHelper.shareFile(context, uri, "text/csv")
                            } else {
                                Toast.makeText(context, "Export generation failed, please try again", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_export_csv"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export Transactions as CSV (Sheets)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val uri = ExportHelper.exportToJSON(
                                context = context,
                                transactions = transactions,
                                budgets = budgets,
                                savingsGoals = savingsGoal?.let { listOf(it) } ?: emptyList()
                            )
                            if (uri != null) {
                                ExportHelper.shareFile(context, uri, "application/json")
                            } else {
                                Toast.makeText(context, "Export generation failed, please try again", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_export_json"),
                        colors = ButtonDefaults.buttonColors(containerColor = MintIncome, contentColor = DarkBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DataObject, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export Complete Data as JSON", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Tax Year Summary Export ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "TAX YEAR SUMMARY EXPORT", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = "Export a custom date-range summary for tax filing or personal archival.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyText,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Select Year:", color = WhiteText, fontWeight = FontWeight.Medium)
                        
                        Box {
                            Row(
                                modifier = Modifier
                                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable { showTaxYearDropdown = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTaxYear ?: "No data",
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TealPrimary)
                            }
                            
                            DropdownMenu(
                                expanded = showTaxYearDropdown,
                                onDismissRequest = { showTaxYearDropdown = false },
                                modifier = Modifier.background(DarkSurfaceElevated)
                            ) {
                                availableYears.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year, color = WhiteText) },
                                        onClick = {
                                            selectedTaxYear = year
                                            showTaxYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedTaxYear == null) {
                                Toast.makeText(context, "No year selected", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val filteredTxs = transactions.filter { it.date.startsWith(selectedTaxYear!!) }
                            if (filteredTxs.isEmpty()) {
                                Toast.makeText(context, "No transactions found for $selectedTaxYear", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val uri = ExportHelper.exportToCSV(context, filteredTxs)
                            if (uri != null) {
                                ExportHelper.shareFile(context, uri, "text/csv")
                            } else {
                                Toast.makeText(context, "Export generation failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_export_tax_year"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7), contentColor = WhiteText),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Export $selectedTaxYear Summary (CSV)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Import / Restore section ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RESTORE FROM BACKUP",
                        fontSize = 11.sp,
                        color = GreyText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select a previously exported JSON file to restore your transactions, budgets, and savings goals. Duplicate records are automatically skipped — safe to run multiple times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyText,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        enabled = !isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_import_json"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberWarning,
                            contentColor = DarkBg,
                            disabledContainerColor = GreyText.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DarkBg)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Importing…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Import from JSON Backup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirmDialog && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = {
                Text(text = "Restore from Backup?", color = WhiteText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File: $pendingImportFileName",
                        color = TealPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This will import all transactions, budgets, and savings goals from the backup file. Existing records with the same ID will be skipped automatically. This operation cannot be undone.",
                        color = GreyText,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingImportJson ?: return@Button
                        isImporting = true
                        showImportConfirmDialog = false
                        viewModel.importFromJSON(json)
                        pendingImportJson = null
                        isImporting = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DarkBg)
                ) {
                    Text("Import & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    pendingImportJson = null
                }) {
                    Text("Cancel", color = GreyText)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
