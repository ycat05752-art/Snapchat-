package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SnapYellow,
    onPrimary = ObsidianBlack,
    primaryContainer = SnapGold.copy(alpha = 0.22f),
    onPrimaryContainer = SnapYellow,
    secondary = NeonCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = CharcoalElevated,
    onSecondaryContainer = PureWhite,
    tertiary = SnapCoral,
    onTertiary = PureWhite,
    background = ObsidianBlack,
    onBackground = DarkTextPrimary,
    surface = CharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = MutedSlate,
    outline = CharcoalBorder
)

private val ColorSurfaceLightVariant = androidx.compose.ui.graphics.Color(0xFFEEEEF5)

private val LightColorScheme = lightColorScheme(
    primary = ObsidianBlack,
    onPrimary = SnapYellow,
    primaryContainer = SnapYellow,
    onPrimaryContainer = ObsidianBlack,
    secondary = ElectricViolet,
    onSecondary = PureWhite,
    secondaryContainer = SoftSnow,
    onSecondaryContainer = LightTextPrimary,
    tertiary = SnapCoral,
    onTertiary = PureWhite,
    background = SoftSnow,
    onBackground = LightTextPrimary,
    surface = LightCard,
    onSurface = LightTextPrimary,
    surfaceVariant = ColorSurfaceLightVariant,
    onSurfaceVariant = MutedSlate,
    outline = LightBorder
)

val NileshShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NileshSnapchatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NileshShapes,
        content = content
    )
}
