package com.sahinkaya.neko.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val NekoDarkColorScheme = darkColorScheme(
    primary = NekoDarkPrimary,
    onPrimary = NekoDarkOnPrimary,
    primaryContainer = NekoDarkPrimaryContainer,
    onPrimaryContainer = NekoDarkOnPrimaryContainer,
    secondary = NekoDarkSecondary,
    onSecondary = NekoDarkOnSecondary,
    secondaryContainer = NekoDarkSecondaryContainer,
    onSecondaryContainer = NekoDarkOnSecondaryContainer,
    tertiary = NekoDarkTertiary,
    onTertiary = NekoDarkOnTertiary,
    tertiaryContainer = NekoDarkTertiaryContainer,
    onTertiaryContainer = NekoDarkOnTertiaryContainer,
    background = NekoFur,
    onBackground = Color(0xFFE8EAED),
    surface = NekoFur,
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF3A3F47),
    onSurfaceVariant = Color(0xFFB4B9C0),
    surfaceContainerLowest = Color(0xFF16181C),
    surfaceContainerLow = Color(0xFF23262B),
    surfaceContainer = Color(0xFF2A2E35),
    surfaceContainerHigh = Color(0xFF33373E),
    surfaceContainerHighest = Color(0xFF3D4148),
    outline = Color(0xFF8A9099),
    outlineVariant = Color(0xFF4A4F57)
)

private val NekoLightColorScheme = lightColorScheme(
    primary = NekoPrimary,
    onPrimary = NekoOnPrimary,
    primaryContainer = NekoPrimaryContainer,
    onPrimaryContainer = NekoOnPrimaryContainer,
    secondary = NekoSecondary,
    onSecondary = NekoOnSecondary,
    secondaryContainer = NekoSecondaryContainer,
    onSecondaryContainer = NekoOnSecondaryContainer,
    tertiary = NekoTertiary,
    onTertiary = NekoOnTertiary,
    tertiaryContainer = NekoTertiaryContainer,
    onTertiaryContainer = NekoOnTertiaryContainer,
    background = NekoMist,
    onBackground = NekoFur,
    surface = NekoMist,
    onSurface = NekoFur,
    surfaceVariant = Color(0xFFDDE1E7),
    onSurfaceVariant = Color(0xFF4A4F57),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F6F9),
    surfaceContainer = Color(0xFFE9EDF3),
    surfaceContainerHigh = Color(0xFFE2E6EC),
    surfaceContainerHighest = Color(0xFFDADEE5),
    outline = Color(0xFF6B7078),
    outlineVariant = Color(0xFFC3C8CF)
)

// Tight Windows-style corners: controls at 4dp, panels at 6-8dp
private val NekoShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NekoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so Neko keeps its neutral palette instead of the wallpaper's
    dynamicColor: Boolean = false,
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        seedColor != null && seedColor != Color.Unspecified -> {
            generateColorSchemeFromSeed(seedColor, darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NekoDarkColorScheme
        else -> NekoLightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = NekoShapes,
        typography = Typography,
        content = content
    )
}
