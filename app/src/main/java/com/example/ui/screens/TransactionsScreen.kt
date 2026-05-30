package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Int) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    
    var filterType by remember { mutableStateOf("all") } // "all" | "income" | "expense"
    var searchQuery by remember { mutableStateOf("") }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    val filteredList = remember(transactions, filterType, searchQuery) {
        transactions.filter { tx ->
            val matchesType = when (filterType) {
                "income" -> tx.type == "income"
                "expense" -> tx.type == "expense"
                else -> true
            }
            val matchesQuery = tx.category.contains(searchQuery, ignoreCase = true) || 
                             (tx.note?.contains(searchQuery, ignoreCase = true) ?: false)
            matchesType && matchesQuery
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Title Header with Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = WhiteText
                )
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("input_search_transactions"),
                placeholder = { Text("Search category or note...", color = GreyText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Categorical Filter pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChipItem(
                    label = "All Logs",
                    isSelected = filterType == "all",
                    tag = "chip_filter_all",
                    onClick = { filterType = "all" }
                )
                FilterChipItem(
                    label = "Incomes",
                    isSelected = filterType == "income",
                    tag = "chip_filter_income",
                    onClick = { filterType = "income" }
                )
                FilterChipItem(
                    label = "Expenses",
                    isSelected = filterType == "expense",
                    tag = "chip_filter_expense",
                    onClick = { filterType = "expense" }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = GreyText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "No transactions matching selected category type",
                            color = GreyText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredList) { tx ->
                        TransactionCardItem(
                            transaction = tx,
                            onEditClick = { onEditTransactionClick(tx.id) },
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
            }
        }

        // Add Transaction Screen Floating Action Button
        FloatingActionButton(
            onClick = { onAddTransactionClick() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("btn_add_transaction_fab"),
            containerColor = LavenderAccentCard,
            contentColor = ActivePillText
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) TealPrimary else DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DarkBg else WhiteText
        )
    }
}
