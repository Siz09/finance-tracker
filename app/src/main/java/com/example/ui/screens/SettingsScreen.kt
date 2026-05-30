package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
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

        // Preferences Tiles
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
            icon = Icons.Default.CloudQueue, // cloud offline look
            iconTint = AmberWarning,
            tag = "tile_settings_backup",
            onClick = onNavigateToBackup
        )

        Spacer(modifier = Modifier.weight(1f))

        // System information footer (100% Client Offline metadata)
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
                text = "v1.0.0.offline — Fully Client-Side Device Sandbox",
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
