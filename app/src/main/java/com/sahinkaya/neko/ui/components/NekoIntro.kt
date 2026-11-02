package com.sahinkaya.neko.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sahinkaya.neko.R
import com.sahinkaya.neko.ui.theme.NekoFur

/**
 * Picks up where the system splash screen ends: the same cat at the same size and spot
 * (splash_logo is drawn on the 288dp splash canvas), then "Neko" fades in underneath.
 */
@Composable
fun NekoIntro(modifier: Modifier = Modifier) {
    val nameAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        nameAlpha.animateTo(1f, tween(durationMillis = 350))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NekoFur),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = null,
            modifier = Modifier.size(288.dp)
        )
        Text(
            text = "Neko Downloader",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFE8EAED),
            // The visible icon is 148dp tall, so its bottom edge sits 74dp below centre
            modifier = Modifier
                .offset(y = 108.dp)
                .graphicsLayer { alpha = nameAlpha.value }
        )
    }
}
