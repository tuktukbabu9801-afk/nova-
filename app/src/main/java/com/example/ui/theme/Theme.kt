package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaVioletPrimary,
    onPrimary = Color(0xFF0E0F14),
    primaryContainer = NovaVioletContainer,
    onPrimaryContainer = NovaOnVioletContainer,
    secondary = NovaElectricBlue,
    onSecondary = Color(0xFF082032),
    secondaryContainer = NovaBlueContainer,
    onSecondaryContainer = NovaOnBlueContainer,
    tertiary = NovaEmeraldSuccess,
    onTertiary = Color(0xFF062C1E),
    background = NovaCharcoalBg,
    onBackground = NovaTextPrimaryDark,
    surface = NovaDarkSurface,
    onSurface = NovaTextPrimaryDark,
    surfaceVariant = NovaDarkSurfaceElevated,
    onSurfaceVariant = NovaTextSecondaryDark,
    outline = NovaDarkBorder,
    error = NovaCoralHigh,
    onError = Color(0xFF3B0909)
)

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaVioletPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = NovaVioletContainerLight,
    onPrimaryContainer = NovaOnVioletContainerLight,
    secondary = NovaElectricBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = NovaLightBg,
    onBackground = NovaTextPrimaryLight,
    surface = NovaLightSurface,
    onSurface = NovaTextPrimaryLight,
    surfaceVariant = NovaLightSurfaceElevated,
    onSurfaceVariant = NovaTextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val NovaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark mode as the default theme per product requirements
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaDarkColorScheme else NovaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NovaShapes,
        content = content
    )
}
