package com.sahinkaya.neko.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Neko palette: neutral greys like the Windows app, so the blurred cat backdrop carries the colour
val NekoFur = Color(0xFF1F2227)
val NekoMist = Color(0xFFEEF1F5)
val NekoInk = Color(0xFF1B1B1B)
val NekoPaper = Color(0xFFF0F0F0)

// Light scheme: black ink on frosted white
val NekoPrimary = NekoInk
val NekoOnPrimary = Color(0xFFFFFFFF)
val NekoPrimaryContainer = Color(0xFFDDE1E7)
val NekoOnPrimaryContainer = NekoInk

val NekoSecondary = Color(0xFF4A4F57)
val NekoOnSecondary = Color(0xFFFFFFFF)
val NekoSecondaryContainer = Color(0xFFE2E5EA)
val NekoOnSecondaryContainer = NekoInk

val NekoTertiary = Color(0xFF5B6168)
val NekoOnTertiary = Color(0xFFFFFFFF)
val NekoTertiaryContainer = Color(0xFFE6E8EC)
val NekoOnTertiaryContainer = NekoInk

// Dark scheme: paper-white controls on smoked glass
val NekoDarkPrimary = NekoPaper
val NekoDarkOnPrimary = NekoInk
val NekoDarkPrimaryContainer = Color(0xFF3A3F47)
val NekoDarkOnPrimaryContainer = Color(0xFFE8EAED)

val NekoDarkSecondary = Color(0xFFC4C8CE)
val NekoDarkOnSecondary = Color(0xFF26292E)
val NekoDarkSecondaryContainer = Color(0xFF353A41)
val NekoDarkOnSecondaryContainer = Color(0xFFE8EAED)

val NekoDarkTertiary = Color(0xFFB4B9C0)
val NekoDarkOnTertiary = Color(0xFF26292E)
val NekoDarkTertiaryContainer = Color(0xFF30343A)
val NekoDarkOnTertiaryContainer = Color(0xFFE8EAED)

// Platform Branding Accents
val YouTubeRed = Color(0xFFFF0000)
val TikTokCyan = Color(0xFF00F2FE)
val InstagramPurple = Color(0xFFE1306C)
val TwitterBlue = Color(0xFF1DA1F2)

/**
 * Derives a dynamic Material 3 ColorScheme from a seed color (e.g. from Palette).
 */
fun generateColorSchemeFromSeed(seed: Color, isDark: Boolean): ColorScheme {
    val seedArgb = seed.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedArgb, hsl)

    val primaryHue = hsl[0]

    val primaryArgb = ColorUtils.HSLToColor(
        floatArrayOf(primaryHue, (hsl[1] * 0.9f).coerceIn(0.4f, 0.9f), if (isDark) 0.75f else 0.45f)
    )
    val primaryContainerArgb = ColorUtils.HSLToColor(
        floatArrayOf(primaryHue, (hsl[1] * 0.7f).coerceIn(0.3f, 0.7f), if (isDark) 0.25f else 0.90f)
    )

    val secondaryHue = (primaryHue + 30f) % 360f
    val secondaryArgb = ColorUtils.HSLToColor(
        floatArrayOf(secondaryHue, 0.5f, if (isDark) 0.70f else 0.40f)
    )
    val secondaryContainerArgb = ColorUtils.HSLToColor(
        floatArrayOf(secondaryHue, 0.4f, if (isDark) 0.22f else 0.88f)
    )

    val tertiaryHue = (primaryHue + 180f) % 360f
    val tertiaryArgb = ColorUtils.HSLToColor(
        floatArrayOf(tertiaryHue, 0.6f, if (isDark) 0.75f else 0.45f)
    )

    return if (isDark) {
        darkColorScheme(
            primary = Color(primaryArgb),
            primaryContainer = Color(primaryContainerArgb),
            secondary = Color(secondaryArgb),
            secondaryContainer = Color(secondaryContainerArgb),
            tertiary = Color(tertiaryArgb),
            background = NekoFur,
            surface = NekoFur
        )
    } else {
        lightColorScheme(
            primary = Color(primaryArgb),
            primaryContainer = Color(primaryContainerArgb),
            secondary = Color(secondaryArgb),
            secondaryContainer = Color(secondaryContainerArgb),
            tertiary = Color(tertiaryArgb),
            background = NekoMist,
            surface = NekoMist
        )
    }
}
