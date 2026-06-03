package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.NetWorthItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val allItems by viewModel.allNetWorthItems.collectAsState()
    val totalAssets by viewModel.totalAssets.collectAsState()
    val totalLiabilities by viewModel.totalLiabilities.collectAsState()
    val netWorth by viewModel.netWorth.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val assets = remember(allItems) { allItems.filter { it.type == "asset" } }
    val liabilities = remember(allItems) { allItems.filter { it.type == "liability" } }
    val displayedItems = if (selectedTab == 0) assets else liabilities

    val netWorthColor = when {
        netWorth > 0 -> MintIncome
        netWorth < 0 -> RubyExpense
        else -> GreyText
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text("Net Worth Tracker", color = WhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Entry", tint = TealPrimary)
                    }
                }
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "NET WORTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreyText,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(netWorth),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = netWorthColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Assets tile
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MintIncome.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MintIncome, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ASSETS", fontSize = 10.sp, color = MintIncome, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(CurrencyFormatter.format(totalAssets), fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            // Liabilities tile
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = RubyExpense.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RubyExpense, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LIABILITIES", fontSize = 10.sp, color = RubyExpense, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(CurrencyFormatter.format(totalLiabilities), fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Tab Row
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = TealPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TealPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Assets (${assets.size})", color = if (selectedTab == 0) TealPrimary else GreyText, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Liabilities (${liabilities.size})", color = if (selectedTab == 1) TealPrimary else GreyText, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Items list
            if (displayedItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.AccountBalance else Icons.Default.CreditCard,
                                    contentDescription = null, tint = GreyText, modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedTab == 0) "No assets added yet" else "No liabilities added yet",
                                    color = GreyText, fontSize = 14.sp
                                )
                                Text("Tap + to add an entry", color = GreyText.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(displayedItems, key = { it.id }) { item ->
                    NetWorthItemCard(
                        item = item,
                        onDelete = { viewModel.deleteNetWorthItem(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddNetWorthItemDialog(
            initialType = if (selectedTab == 0) "asset" else "liability",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, value, type, category ->
                viewModel.addNetWorthItem(NetWorthItem(name = name, value = value, type = type, category = category))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun NetWorthItemCard(item: NetWorthItem, onDelete: () -> Unit) {
    val accentColor = if (item.type == "asset") MintIncome else RubyExpense
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == "asset") Icons.Default.AccountBalance else Icons.Default.CreditCard,
                    contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, color = WhiteText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.category, color = GreyText, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(item.value), fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GreyText, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${item.name}\" from your net worth tracker?", color = GreyText) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
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
private fun AddNetWorthItemDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: Double, type: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var category by remember { mutableStateOf(if (initialType == "asset") "Cash" else "Loan") }

    val assetCategories = listOf("Cash", "Bank Account", "Property", "Vehicle", "Investments", "Other")
    val liabilityCategories = listOf("Loan", "Mortgage", "Credit Card", "Other Debt")
    val categories = if (type == "asset") assetCategories else liabilityCategories

    val isValid = name.isNotBlank() && valueStr.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Entry", color = WhiteText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("asset", "liability").forEach { t ->
                        val selected = type == t
                        val color = if (t == "asset") MintIncome else RubyExpense
                        FilterChip(
                            selected = selected,
                            onClick = {
                                type = t
                                category = if (t == "asset") "Cash" else "Loan"
                            },
                            label = { Text(t.replaceFirstChar { it.uppercase() }, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color,
                                containerColor = DarkSurfaceElevated,
                                labelColor = GreyText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it }, // 100-char cap (#14)
                    label = { Text("Name (e.g. Savings Account)", color = GreyText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = valueStr, onValueChange = { valueStr = it },
                    label = { Text("Value (Rs.)", color = GreyText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                        focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                // Category chips
                Text("Category", color = GreyText, fontSize = 12.sp)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = TealPrimary,
                                containerColor = DarkSurfaceElevated,
                                labelColor = GreyText
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), valueStr.toDouble(), type, category) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg)
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GreyText) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
