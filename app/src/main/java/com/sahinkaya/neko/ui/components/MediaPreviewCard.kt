package com.sahinkaya.neko.ui.components

import com.sahinkaya.neko.ui.theme.GlassCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sahinkaya.neko.model.MediaInfo
import com.sahinkaya.neko.model.MediaPlatform
import com.sahinkaya.neko.ui.theme.InstagramPurple
import com.sahinkaya.neko.ui.theme.TikTokCyan
import com.sahinkaya.neko.ui.theme.TwitterBlue
import com.sahinkaya.neko.ui.theme.YouTubeRed

@Composable
fun MediaPreviewCard(
    mediaInfo: MediaInfo?,
    onOpenFormatSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = mediaInfo != null,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        modifier = modifier
    ) {
        if (mediaInfo != null) {
            val platformColor = when (mediaInfo.platform) {
                MediaPlatform.YOUTUBE -> YouTubeRed
                MediaPlatform.INSTAGRAM -> InstagramPurple
                MediaPlatform.TIKTOK -> TikTokCyan
                MediaPlatform.TWITTER -> TwitterBlue
                else -> MaterialTheme.colorScheme.primary
            }

            GlassCard(
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 12.dp,
                    bottomEnd = 28.dp,
                    bottomStart = 12.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Thumbnail container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (!mediaInfo.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = mediaInfo.thumbnailUrl,
                                contentDescription = mediaInfo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Gradient overlay for better badge readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )

                        // Top-left platform badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = platformColor,
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                text = mediaInfo.platform.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Bottom-right duration badge
                        if (mediaInfo.durationSeconds != null && mediaInfo.durationSeconds > 0) {
                            val mins = mediaInfo.durationSeconds / 60
                            val secs = mediaInfo.durationSeconds % 60
                            val durationText = String.format("%02d:%02d", mins, secs)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = mediaInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Uploader / Channel
                    if (!mediaInfo.uploader.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mediaInfo.uploader,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button
                    Button(
                        onClick = onOpenFormatSheet,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Format Seç ve İndir",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
