package com.shdarv.yalda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF137FEC),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0F172A),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EDF2),
    background = Color(0xFFF6F7F8),
    onBackground = Color(0xFF0F172A),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFE2E8F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF137FEC),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFF8FAFC),
    onSecondary = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF243244),
    background = Color(0xFF101922),
    onBackground = Color(0xFFF8FAFC),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF334155),
)

@Composable
fun YaldaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
