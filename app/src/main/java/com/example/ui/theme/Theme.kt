package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF031B26),
    primaryContainer = Color(0xFF0C384D),
    onPrimaryContainer = Color(0xFFB5EAFF),

    secondary = MagentaNeon,
    onSecondary = Color(0xFF380018),
    secondaryContainer = Color(0xFF5B0029),
    onSecondaryContainer = Color(0xFFFFD9E2),

    tertiary = AmberGold,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = Color(0xFF452B00),
    onTertiaryContainer = Color(0xFFFFDF9E),

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,

    outline = CardBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
