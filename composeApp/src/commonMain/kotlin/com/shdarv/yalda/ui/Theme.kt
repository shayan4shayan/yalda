package com.shdarv.yalda.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import yalda.composeapp.generated.resources.Res
import yalda.composeapp.generated.resources.vazirmatn_regular
import yalda.composeapp.generated.resources.vazirmatn_medium
import yalda.composeapp.generated.resources.vazirmatn_semibold
import yalda.composeapp.generated.resources.vazirmatn_bold
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
        typography = appTypography(),
        content = content
    )
}

// Bundle the family for every profile: Persian can appear in translations even
// when the interface language is English. Other scripts use platform fallback.
@Composable
private fun appTypography(): Typography {
    val family = FontFamily(
        Font(Res.font.vazirmatn_regular, FontWeight.Normal),
        Font(Res.font.vazirmatn_medium, FontWeight.Medium),
        Font(Res.font.vazirmatn_semibold, FontWeight.SemiBold),
        Font(Res.font.vazirmatn_bold, FontWeight.Bold)
    )
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = family),
        displayMedium = defaults.displayMedium.copy(fontFamily = family),
        displaySmall = defaults.displaySmall.copy(fontFamily = family),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = family),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = family),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = family),
        titleLarge = defaults.titleLarge.copy(fontFamily = family),
        titleMedium = defaults.titleMedium.copy(fontFamily = family),
        titleSmall = defaults.titleSmall.copy(fontFamily = family),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = family),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = family),
        bodySmall = defaults.bodySmall.copy(fontFamily = family),
        labelLarge = defaults.labelLarge.copy(fontFamily = family),
        labelMedium = defaults.labelMedium.copy(fontFamily = family),
        labelSmall = defaults.labelSmall.copy(fontFamily = family)
    )
}
