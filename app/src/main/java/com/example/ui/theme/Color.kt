package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Light Mode Scheme Values
val LightBgVal = Color(0xFFFEF7FF)
val LightSurfaceVal = Color(0xFFF3EDF7)
val LightSurfaceElevatedVal = Color(0xFFFFFFFF)
val LightTealPrimaryVal = Color(0xFF6750A4)
val LightWhiteTextVal = Color(0xFF1D1B20)
val LightGreyTextVal = Color(0xFF49454F)
val LightMintIncomeVal = Color(0xFF1B6F1B)
val LightRubyExpenseVal = Color(0xFFB3261E)
val LightBorderPaleVal = Color(0x4DCAC4D0)

// Gorgeous Slate Dark Mode Scheme Values
val DarkBgVal = Color(0xFF0D0E12)
val DarkSurfaceVal = Color(0xFF161820)
val DarkSurfaceElevatedVal = Color(0xFF1F222D)
val DarkTealPrimaryVal = Color(0xFF14FFEC)
val DarkWhiteTextVal = Color(0xFFF5F6F9)
val DarkGreyTextVal = Color(0xFF9095A6)
val DarkMintIncomeVal = Color(0xFF00E676)
val DarkRubyExpenseVal = Color(0xFFFF1744)
val DarkBorderPaleVal = Color(0x2BFFFFFF)

// Dynamic backing properties for zero-refactor dynamic themes
var DarkBg by mutableStateOf(DarkBgVal)
var DarkSurface by mutableStateOf(DarkSurfaceVal)
var DarkSurfaceElevated by mutableStateOf(DarkSurfaceElevatedVal)
var MintIncome by mutableStateOf(DarkMintIncomeVal)
var RubyExpense by mutableStateOf(DarkRubyExpenseVal)
var TealPrimary by mutableStateOf(DarkTealPrimaryVal)
var AmberWarning by mutableStateOf(Color(0xFFE65100))
var GreyText by mutableStateOf(DarkGreyTextVal)
var WhiteText by mutableStateOf(DarkWhiteTextVal)
var BorderPale by mutableStateOf(DarkBorderPaleVal)

// Theme update function called dynamically inside MyApplicationTheme Composable
fun updateThemeColors(darkTheme: Boolean) {
    if (darkTheme) {
        DarkBg = DarkBgVal
        DarkSurface = DarkSurfaceVal
        DarkSurfaceElevated = DarkSurfaceElevatedVal
        MintIncome = DarkMintIncomeVal
        RubyExpense = DarkRubyExpenseVal
        TealPrimary = DarkTealPrimaryVal
        GreyText = DarkGreyTextVal
        WhiteText = DarkWhiteTextVal
        BorderPale = DarkBorderPaleVal
    } else {
        DarkBg = LightBgVal
        DarkSurface = LightSurfaceVal
        DarkSurfaceElevated = LightSurfaceElevatedVal
        MintIncome = LightMintIncomeVal
        RubyExpense = LightRubyExpenseVal
        TealPrimary = LightTealPrimaryVal
        GreyText = LightGreyTextVal
        WhiteText = LightWhiteTextVal
        BorderPale = LightBorderPaleVal
    }
}
