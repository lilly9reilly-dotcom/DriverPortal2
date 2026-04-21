package com.driver.portal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DriverPortalColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = White,
    primaryContainer = BrandOrangeDark,
    onPrimaryContainer = White,
    secondary = BrandTeal,
    onSecondary = White,
    tertiary = BrandGold,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceAlt,
    onSurfaceVariant = AppTextMuted,
    error = Red,
    onError = White
)

@Composable
fun DriverPortalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriverPortalColorScheme,
        typography = Typography,
        content = content
    )
}