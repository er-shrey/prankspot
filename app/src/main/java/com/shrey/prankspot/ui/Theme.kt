package com.shrey.prankspot.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shrey.prankspot.data.ThemeChoice

val Grape = Color(0xFF6C3BFF)
val Bubblegum = Color(0xFFFF4FA3)
val Sunny = Color(0xFFFFC93C)
val Mint = Color(0xFF2ED3A1)

private val Light = lightColorScheme(
    primary = Grape, onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DDFF), onPrimaryContainer = Color(0xFF22005D),
    secondary = Bubblegum, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E8), onSecondaryContainer = Color(0xFF3E0021),
    tertiary = Color(0xFF8A6100), tertiaryContainer = Color(0xFFFFE8B0), onTertiaryContainer = Color(0xFF2B1D00),
    background = Color(0xFFFFF8F0), surface = Color(0xFFFFF8F0),
    surfaceContainer = Color(0xFFF7EEF6), surfaceContainerHigh = Color(0xFFF1E6F1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCBBEFF), onPrimary = Color(0xFF370096),
    primaryContainer = Color(0xFF5020D8), onPrimaryContainer = Color(0xFFE6DDFF),
    secondary = Color(0xFFFFB0CF), onSecondary = Color(0xFF5F0038),
    secondaryContainer = Color(0xFF85104F), onSecondaryContainer = Color(0xFFFFD9E8),
    tertiary = Sunny, tertiaryContainer = Color(0xFF5E4300), onTertiaryContainer = Color(0xFFFFE8B0),
    background = Color(0xFF15121F), surface = Color(0xFF15121F),
    surfaceContainer = Color(0xFF221D2E), surfaceContainerHigh = Color(0xFF2C2638),
)

@Composable
fun PrankSpotTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
