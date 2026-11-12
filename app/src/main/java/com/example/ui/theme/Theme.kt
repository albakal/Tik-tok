package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val VyoraDarkColorScheme = darkColorScheme(
    primary = VyoraCrimson,
    onPrimary = VyoraWhite,
    primaryContainer = VyoraCrimson.copy(alpha = 0.2f),
    onPrimaryContainer = VyoraCrimson,
    secondary = VyoraCyan,
    onSecondary = VyoraBlack,
    secondaryContainer = VyoraCyan.copy(alpha = 0.2f),
    onSecondaryContainer = VyoraCyan,
    background = VyoraBlack,
    onBackground = VyoraWhite,
    surface = VyoraDarkSurface,
    onSurface = VyoraWhite,
    surfaceVariant = VyoraElevated,
    onSurfaceVariant = VyoraGrey,
    outline = VyoraCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Set Arabic RTL as standard layout direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = VyoraDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
