package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Int) -> Unit
) {
    // Use currentMonthTransactions so list matches Dashboard's selected month
    val transactions by viewModel.currentMonthTransactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var filterType by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

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

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Header (Back arrow removed for clean top-level tab)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = WhiteText
                    )
                    Text(
                        text = "Detailed list of your financial records",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyText
                    )
                }
            }

            // Collapsible Date Selector Card (Paradigm B - Zero click highlight & spring physics)
            var isExpanded by remember { mutableStateOf(false) }
            val interactionSource = remember { MutableInteractionSource() }
            var yearMenuExpanded by remember { mutableStateOf(false) }
            val yearsList = listOf("2023", "2024", "2025", "2026", "2027", "2028")
            val currentYear = remember(selectedMonth) { selectedMonth.take(4) }
            val currentMonthIndex = remember(selectedMonth) { selectedMonth.substring(5).toIntOrNull()?.minus(1) ?: 0 }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    // Top Row: Info Title & Expansion Toggle (Zero Click Highlight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                isExpanded = !isExpanded
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rotationState by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "caret_rotation"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = getFormattedMonthName(selectedMonth),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WhiteText
                                )
                                Text(
                                    text = if (isExpanded) "Tap to collapse" else "Tap to change month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GreyText
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = rotationState)
                            )
                        }

                        // Year dropdown trigger (Only visible when expanded)
                        if (isExpanded) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                        .clickable { yearMenuExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = currentYear,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Year",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = yearMenuExpanded,
                                    onDismissRequest = { yearMenuExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceElevated)
                                ) {
                                    yearsList.forEach { yr ->
                                        DropdownMenuItem(
                                            text = { Text(text = yr, color = WhiteText, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                yearMenuExpanded = false
                                                val monthStr = String.format("%02d", currentMonthIndex + 1)
                                                viewModel.selectedMonth.value = "$yr-$monthStr"
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Calendar visual icon in collapsed state
                            Box(
                                modifier = Modifier
                                    .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Expand Calendar Selector",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Smooth expandable slide and fade animation for Month Ribbon
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(animationSpec = tween(150, delayMillis = 80)) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(animationSpec = tween(100))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Horizontally scrollable Months Ribbon
                            val monthsList = listOf(
                                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                            )
                            val listState = rememberLazyListState()

                            // Auto-scroll selected month to the center when selectedMonth changes
                            LaunchedEffect(currentMonthIndex) {
                                listState.animateScrollToItem(maxOf(0, currentMonthIndex - 2))
                            }

                            LazyRow(
                                state = listState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(monthsList) { idx, mth ->
                                    val isSelected = idx == currentMonthIndex
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) TealPrimary else DarkSurfaceElevated,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                val monthStr = String.format("%02d", idx + 1)
                                                viewModel.selectedMonth.value = "$currentYear-$monthStr"
                                                isExpanded = false // Collapse card on selection
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mth,
                                            color = if (isSelected) DarkBg else WhiteText,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("input_search_transactions"),
                placeholder = { Text("Search category or note...", color = GreyText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreyText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary, unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = WhiteText, unfocusedTextColor = WhiteText,
                    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Filter chips
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChipItem(label = "All Logs", isSelected = filterType == "all", tag = "chip_filter_all", onClick = { filterType = "all" })
                FilterChipItem(label = "Incomes", isSelected = filterType == "income", tag = "chip_filter_income", onClick = { filterType = "income" })
                FilterChipItem(label = "Expenses", isSelected = filterType == "expense", tag = "chip_filter_expense", onClick = { filterType = "expense" })
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = GreyText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "No transactions this month",
                            color = GreyText, style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredList) { tx ->
                        TransactionCardItem(
                            transaction = tx,
                            onEditClick = { onEditTransactionClick(tx.id) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { onAddTransactionClick() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp).testTag("btn_add_transaction_fab"),
            containerColor = LavenderAccentCard,
            contentColor = ActivePillText
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
fun FilterChipItem(label: String, isSelected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color = if (isSelected) TealPrimary else DarkSurface, shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) DarkBg else WhiteText)
    }
}
