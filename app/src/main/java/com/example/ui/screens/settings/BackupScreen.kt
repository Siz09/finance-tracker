package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Backup & Data Exports", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_backup")) {
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Divider(color = DarkSurfaceElevated)

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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        Text(text = "Export Complete SQLite as JSON", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
