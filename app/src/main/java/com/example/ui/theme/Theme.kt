package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MinimalColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = ActivePillText,
    primaryContainer = LavenderAccentCard,
    onPrimaryContainer = TextPrimary,
    secondary = MintIncome,
    onSecondary = AppSurface,
    tertiary = RubyExpense,
    onTertiary = AppSurface,
    background = LightBg,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = PaleSurface,
    onSurfaceVariant = TextSecondary,
    error = RubyExpense,
    onError = AppSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalColorScheme,
        typography = Typography,
        content = content
    )
}

