package com.example.memori.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MemoriDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error
)

@Composable
fun MemoriTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MemoriDarkColorScheme,
        typography = MemoriTypography,
        content = content
    )
}