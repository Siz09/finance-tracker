package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("digital") } // "digital" | "bank" | "cash"
    var accountEmoji by remember { mutableStateOf("💳") }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Manage Wallets & Accounts", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = DarkBg
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Wallet, contentDescription = null, tint = TealPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Track Multiple Wallets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Text(
                            text = "Divide your balance into Bank Accounts, digital wallets like eSewa/Khalti, or physical Cash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreyText
                        )
                    }
                }
            }

            Text(
                text = "YOUR WALLETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GreyText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No wallets created yet. Tap + to add one!", color = GreyText)
                }
            } else {
                accounts.forEach { acc ->
                    // Calculate individual wallet balance
                    val walletTxs = transactions.filter { it.accountId == acc.id }
                    val income = walletTxs.filter { it.type == "income" }.sumOf { it.amount }
                    val expense = walletTxs.filter { it.type == "expense" }.sumOf { it.amount }
                    val balance = income - expense

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(DarkBg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = acc.emoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = acc.name, fontWeight = FontWeight.Bold, color = WhiteText)
                                    Text(
                                        text = acc.type.uppercase(),
                                        color = GreyText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("Rs. %.2f", balance),
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance >= 0) MintIncome else RubyExpense,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.deleteAccount(acc.id)
                                        Toast.makeText(context, "Wallet deleted", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RubyExpense)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add wallet popup dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add New Wallet", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Wallet / Account Name", color = GreyText) },
                        placeholder = { Text("e.g. eSewa, Global IME Bank, Cash", color = GreyText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountEmoji,
                        onValueChange = { accountEmoji = it },
                        label = { Text("Emoji Icon", color = GreyText) },
                        placeholder = { Text("e.g. 💳, 💰, 🏦", color = GreyText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = DarkSurfaceElevated,
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type select
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("digital", "bank", "cash").forEach { type ->
                            val isSelected = accountType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) TealPrimary else DarkSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { accountType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    color = if (isSelected) DarkBg else WhiteText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountName.isNotBlank()) {
                            viewModel.addAccount(accountName, accountType, accountEmoji)
                            showAddDialog = false
                            accountName = ""
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
                ) {
                    Text("Create Wallet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = GreyText)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
