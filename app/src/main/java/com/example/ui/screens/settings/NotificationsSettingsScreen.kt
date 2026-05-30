package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    viewModel: FinanceViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val savedTimeStr by viewModel.reminderTime.collectAsState()

    // Internal picker states, synced with DB value when received
    var hour by remember { mutableStateOf(20) }
    var minute by remember { mutableStateOf(0) }

    LaunchedEffect(savedTimeStr) {
        val parts = savedTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()
        val m = parts.getOrNull(1)?.toIntOrNull()
        if (h != null && m != null) {
            hour = h
            minute = m
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = { Text(text = "Daily Reminders schedule", color = WhiteText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_notifications")) {
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
            // Icon layout header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Daily Expense Tracker Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                Text(
                    text = "We will schedule a daily notification alert on your system at this configured hour to remind you to log today's financial transitions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreyText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Sub Status Box showing currently configured reminder time
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Current Alert schedule: ${formatHourMinute(hour, minute)}",
                        fontWeight = FontWeight.Bold,
                        color = WhiteText,
                        fontSize = 14.sp
                    )
                }
            }

            // Custom Hour and Minute spinner controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "SELECT REMINDER SCHEDULE TIME", fontSize = 11.sp, color = GreyText, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increment Hour", tint = TealPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", hour),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WhiteText,
                                modifier = Modifier.testTag("text_hour_pick")
                            )
                            IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrement Hour", tint = TealPrimary)
                            }
                            Text(text = "Hour (24h)", style = MaterialTheme.typography.labelSmall, color = GreyText)
                        }

                        Text(text = ":", fontSize = 36.sp, color = WhiteText, fontWeight = FontWeight.ExtraBold)

                        // Minute spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increment Minute", tint = TealPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", minute),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WhiteText,
                                modifier = Modifier.testTag("text_minute_pick")
                            )
                            IconButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrement Minute", tint = TealPrimary)
                            }
                            Text(text = "Minute", style = MaterialTheme.typography.labelSmall, color = GreyText)
                        }
                    }
                }
            }

            // Save alarm settings action button
            Button(
                onClick = {
                    viewModel.saveReminderTime(context, hour, minute)
                    Toast.makeText(context, "Daily alert scheduled at ${formatHourMinute(hour, minute)}!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_reminder_schedule"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DarkBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.AlarmOn, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Save Reminder Alert Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

private fun formatHourMinute(h: Int, m: Int): String {
    val suffix = if (h >= 12) "PM" else "AM"
    val adjustedHour = if (h == 0) 12 else if (h > 12) h - 12 else h
    return String.format(Locale.getDefault(), "%02d:%02d %s", adjustedHour, m, suffix)
}
