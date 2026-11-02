package com.sahinkaya.neko.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sahinkaya.neko.R
import com.sahinkaya.neko.data.BackgroundMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.io.File

/** Backdrop that glass surfaces blur. Provided by MainScreen; null falls back to a frosted tint. */
val LocalBackdropHaze = staticCompositionLocalOf<HazeState?> { null }

/** Space kept below the last item of a scrolling screen. */
val NavBarClearance: Dp = 24.dp

@Composable
fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Frosted-glass style: blur, a translucent tint and a little grain. */
@Composable
fun nekoGlassStyle(
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    tintAlpha: Float = if (isDarkSurface()) 0.72f else 0.78f,
    blurRadius: Dp = 24.dp
): HazeStyle = HazeStyle(
    backgroundColor = MaterialTheme.colorScheme.background,
    tint = HazeTint(tint.copy(alpha = tintAlpha)),
    blurRadius = blurRadius,
    noiseFactor = 0.04f,
    // Pre-Android 12 has no RenderEffect blur, so the glass becomes a near-opaque frost
    fallbackTint = HazeTint(tint.copy(alpha = 0.94f))
)

/** Hairline used for glass edges and dividers. */
@Composable
fun glassEdge(): Color =
    if (isDarkSurface()) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)

/** Clips to [shape] and blurs whatever [state] records behind it. */
@Composable
fun Modifier.glass(
    shape: Shape,
    state: HazeState? = LocalBackdropHaze.current,
    style: HazeStyle = nekoGlassStyle(),
    edge: Boolean = true
): Modifier {
    val edgeColor = glassEdge()
    val surface = if (state != null) {
        Modifier.hazeEffect(state = state, style = style)
    } else {
        Modifier.background(style.fallbackTint.color)
    }
    return this
        .clip(shape)
        .then(surface)
        .then(if (edge) Modifier.border(1.dp, edgeColor, shape) else Modifier)
}

/** A 1dp line along the bottom edge, like the Windows header panel. */
@Composable
fun Modifier.bottomHairline(): Modifier {
    val color = glassEdge()
    return drawBehind {
        drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
    }
}

/** Card-shaped glass panel. Use in place of a Card when content sits over the Neko backdrop. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glass(shape = shape, style = nekoGlassStyle(tint = tint))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Full-screen background: Neko's black cat (or the user's own image), blurred by [blur] and
 * washed with the theme colour so text stays readable. [BackgroundMode.OFF] leaves a flat colour.
 * Modifier.blur needs Android 12; older devices show the image unblurred under the wash.
 */
@Composable
fun NekoBackdrop(
    mode: BackgroundMode,
    customImagePath: String?,
    blur: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isDarkSurface()

    Box(modifier = modifier.background(scheme.background)) {
        if (mode != BackgroundMode.OFF) {
            val model: Any = if (mode == BackgroundMode.CUSTOM && customImagePath != null) {
                File(customImagePath)
            } else {
                R.drawable.neko_backdrop
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blur > 0) Modifier.blur(blur.dp) else Modifier)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background.copy(alpha = if (dark) 0.30f else 0.45f))
            )
        }
    }
}
