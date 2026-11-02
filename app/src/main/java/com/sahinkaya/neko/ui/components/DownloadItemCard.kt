package com.sahinkaya.neko.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sahinkaya.neko.model.DownloadItem
import com.sahinkaya.neko.model.DownloadStatus
import com.sahinkaya.neko.ui.theme.GlassCard

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // yt-dlp reports -1 until it knows the file size
    val progressKnown = item.progress >= 0f
    val animatedProgress by animateFloatAsState(
        targetValue = (item.progress / 100f).coerceIn(0f, 1f),
        label = "progressAnim"
    )
    val isActive = item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.INITIALIZING ||
        item.status == DownloadStatus.PROCESSING_FFMPEG
    val isPaused = item.status == DownloadStatus.PAUSED

    GlassCard(
        shape = CurvedScreenShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Extra room at the bottom so the bowed edge never cuts into the content
        Column(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp + CurveDepth)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail / Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (item.isAudioOnly) Icons.Default.MusicNote else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Badge (Pill)
                        StatusBadge(status = item.status)

                        // Format Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = item.ext.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Actions: pause or resume depending on the state, remove always
                when (item.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.INITIALIZING,
                    DownloadStatus.PROCESSING_FFMPEG, DownloadStatus.QUEUED -> {
                        IconButton(onClick = onPause) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Durdur")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Devam ettir")
                        }
                    }
                    DownloadStatus.ERROR, DownloadStatus.CANCELLED -> {
                        IconButton(onClick = onResume) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yeniden dene")
                        }
                    }
                    DownloadStatus.COMPLETED -> Unit
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Listeden kaldır",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Bar & Indicators (for active downloads)
            if (isActive || isPaused) {
                Spacer(modifier = Modifier.height(12.dp))

                // A paused download keeps its bar at the point where it stopped
                if (progressKnown || isPaused) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.speed.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.speed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Percentage
                    if (progressKnown) {
                        Text(
                            text = "%${item.progress.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // ETA
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.eta.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.eta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Message (if any)
            val errorMessage = item.errorMessage
            if (item.status == DownloadStatus.ERROR && !errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = readableError(errorMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (bgColor, textColor, label, icon) = when (status) {
        DownloadStatus.DOWNLOADING -> Quad(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "İndiriliyor",
            null
        )
        DownloadStatus.INITIALIZING -> Quad(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Hazırlanıyor",
            null
        )
        DownloadStatus.COMPLETED -> Quad(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Tamamlandı",
            Icons.Default.CheckCircle
        )
        DownloadStatus.ERROR -> Quad(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Hata",
            Icons.Default.Error
        )
        DownloadStatus.CANCELLED -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "İptal Edildi",
            null
        )
        DownloadStatus.QUEUED -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Kuyrukta",
            null
        )
        DownloadStatus.PAUSED -> Quad(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Duraklatıldı",
            null
        )
        DownloadStatus.PROCESSING_FFMPEG -> Quad(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "İşleniyor (FFmpeg)",
            null
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * yt-dlp failures arrive as long multi-line output (often a Python traceback); show the line that
 * says what went wrong, e.g. "ERROR: ... Video unavailable", instead of the first two lines.
 */
private fun readableError(message: String): String {
    val lines = message.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return lines.lastOrNull { it.startsWith("ERROR:") }?.removePrefix("ERROR:")?.trim()
        ?: lines.lastOrNull { it.contains("Error:") || it.contains("Exception:") }
        ?: lines.firstOrNull()
        ?: message
}

private val CurveDepth = 8.dp
private val CurveCorner = 18.dp

/**
 * A curved TV screen seen from slightly above: a flat top with rounded corners, and a bottom edge
 * that bows up towards the middle by [CurveDepth], so the bottom corners hang lower than the centre.
 */
private object CurvedScreenShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { CurveCorner.toPx() }.coerceAtMost(size.minDimension / 2)
        val d = with(density) { CurveDepth.toPx() }
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(w - r, 0f)
            quadraticTo(w, 0f, w, r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            // A quadratic curve peaks halfway to its control point, so 2d lifts the centre by d
            quadraticTo(w / 2, h - 2 * d, r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        return Outline.Generic(path)
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
