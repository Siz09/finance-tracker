package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Title Header with Back button
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
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = WhiteText
            )
        }

        HorizontalDivider(color = BorderPale, thickness = 1.dp)

        // New Feature 1: Reports screen shortcut
        SettingsMenuItem(
            title = "Spending Analytics & Reports",
            subtitle = "See deep interactive charts and MoM comparisons",
            icon = Icons.Default.Assessment,
            iconTint = TealPrimary,
            tag = "tile_settings_reports",
            onClick = onNavigateToReports
        )

        // New Feature 2: Manage Accounts / Wallets
        SettingsMenuItem(
            title = "Wallets & Multi-Accounts",
            subtitle = "Divide assets into Cash, eSewa, Bank, etc.",
            icon = Icons.Default.Wallet,
            iconTint = MintIncome,
            tag = "tile_settings_accounts",
            onClick = onNavigateToAccounts
        )

        SettingsMenuItem(
            title = "Monthly Category Budgets",
            subtitle = "Set limits and configure 80% warnings per category",
            icon = Icons.Default.TrendingDown,
            iconTint = RubyExpense,
            tag = "tile_settings_budget",
            onClick = onNavigateToBudget
        )

        SettingsMenuItem(
            title = "Savings Goals Plan",
            subtitle = "Set your savings target and track achievements",
            icon = Icons.Default.LocalActivity,
            iconTint = MintIncome,
            tag = "tile_settings_savings",
            onClick = onNavigateToSavings
        )

        SettingsMenuItem(
            title = "Daily Reminders schedule",
            subtitle = "Schedules local offline alarms to log logs",
            icon = Icons.Default.NotificationsActive,
            iconTint = TealPrimary,
            tag = "tile_settings_notifications",
            onClick = onNavigateToNotifications
        )

        SettingsMenuItem(
            title = "Backup & Data Exports",
            subtitle = "Export database files safely to local JSON or CSV formats",
            icon = Icons.Default.CloudQueue,
            iconTint = AmberWarning,
            tag = "tile_settings_backup",
            onClick = onNavigateToBackup
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System information footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Personal Finance Tracker",
                fontWeight = FontWeight.Bold,
                color = GreyText,
                fontSize = 14.sp
            )
            Text(
                text = "v1.1.0.offline — Multi-Wallet, Recurring, Biometric Security",
                color = GreyText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DarkBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, color = WhiteText, fontSize = 15.sp)
                    Text(text = subtitle, color = GreyText, fontSize = 12.sp)
                }
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = GreyText)
        }
    }
}
