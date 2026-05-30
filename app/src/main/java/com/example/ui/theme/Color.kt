package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// NEW: Clean Minimalism Color Palette (HTML Inspired)
// ==========================================
val LightBg = Color(0xFFFEF7FF)              // Very light pale purple-white background
val AppSurface = Color(0xFFFFFFFF)            // Clean white surface
val PaleSurface = Color(0xFFF3EDF7)           // Recent transaction & panel pale-grey background
val LavenderAccentCard = Color(0xFFEADDFF)     // Balance card / main accent background
val TextPrimary = Color(0xFF1D1B20)          // High contrast primary text (charcoal)
val TextSecondary = Color(0xFF49454F)        // Medium contrast secondary text
val ActivePill = Color(0xFFEADDFF)            // Navigation active background
val ActivePillText = Color(0xFF21005D)        // Intense violet active text

// Functional Colors
val PrimaryBrand = Color(0xFF6750A4)          // Brand purple / Chart Segment / Links
val IncomeForestGreen = Color(0xFF1B6F1B)     // Calm deep green for income flow
val ExpenseWarmRed = Color(0xFFB3261E)         // Muted deep red for expense outflow
val WarningAmber = Color(0xFFE65100)          // Warm amber for 80% threshold warnings
val BorderPale = Color(0x4DCAC4D0)            // Fine border lines (#CAC4D0 at 30% alpha)

// ==========================================
// Compatibility Aliases for Existing Screens
// ==========================================
val DarkBg = LightBg                          // Maps dark background style to minimalist light-purple background
val DarkSurface = PaleSurface                 // Maps dark card surfaces to elegant pale grey-lavender surface
val DarkSurfaceElevated = AppSurface          // Maps elevated elements to pristine white cards
val MintIncome = IncomeForestGreen
val RubyExpense = ExpenseWarmRed
val TealPrimary = PrimaryBrand
val AmberWarning = WarningAmber
val GreyText = TextSecondary
val WhiteText = TextPrimary

