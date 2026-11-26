package com.example.deepsource.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeepsourceColors = darkColorScheme(
    primary = Violet,
    onPrimary = Ink,
    primaryContainer = Action,
    onPrimaryContainer = Paper,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = Elevated,
    onSecondaryContainer = Mint,
    tertiary = Pink80,
    onTertiary = Ink,
    tertiaryContainer = Pink40,
    onTertiaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = Panel,
    onSurface = Paper,
    surfaceVariant = Elevated,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger,
    onError = Ink,
    errorContainer = Pink40,
    onErrorContainer = Paper
)

@Composable
fun DeepsourceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeepsourceColors,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp)
        ),
        content = content
    )
}
