package com.sahinkaya.neko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.sahinkaya.neko.model.DownloadOptions
import com.sahinkaya.neko.model.FormatOption
import com.sahinkaya.neko.model.MediaInfo
import com.sahinkaya.neko.ui.theme.glassEdge
import java.util.Locale

private const val COLLAPSED_COUNT = 6

/**
 * Full-screen list of every stream yt-dlp found. Pick one video and/or one audio stream;
 * picking both merges them into one file. In audio mode only audio streams are listed.
 */
@Composable
fun FormatSelectionScreen(
    mediaInfo: MediaInfo,
    audioOnly: Boolean,
    initialSubtitleLang: String?,
    onClose: () -> Unit,
    onConfirm: (DownloadOptions) -> Unit
) {
    val audio = remember(mediaInfo) { audioStreams(mediaInfo) }
    val videoOnly = remember(mediaInfo) {
        mediaInfo.formats.filter { hasVideo(it) && it.isVideoOnly }
            .sortedWith(compareByDescending<FormatOption> { it.height ?: 0 }.thenByDescending { it.bitrateKbps ?: 0 })
    }
    val videoWithAudio = remember(mediaInfo) {
        mediaInfo.formats.filter { hasVideo(it) && !it.isVideoOnly }.sortedByDescending { it.height ?: 0 }
    }
    val recommended = remember(mediaInfo) {
        if (audioOnly) FormatPair(null, bestAudio(mediaInfo, ext = "m4a") ?: bestAudio(mediaInfo))
        else compatiblePair(mediaInfo).takeIf { it.video != null } ?: bestQualityPair(mediaInfo)
    }

    var selectedVideo by remember { mutableStateOf(recommended.video) }
    var selectedAudio by remember { mutableStateOf(recommended.audio) }
    var subtitleLang by remember { mutableStateOf(initialSubtitleLang) }
    val canConfirm = selectedVideo != null || selectedAudio != null

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Kapat")
                    }
                    Text(
                        text = "Biçim seçimi",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onConfirm(buildOptions(selectedVideo, selectedAudio, subtitleLang)) },
                        enabled = canConfirm
                    ) {
                        Text("İndir", fontWeight = FontWeight.SemiBold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { MediaHeader(mediaInfo) }

                    if (!audioOnly && mediaInfo.subtitles.isNotEmpty()) {
                        item { SectionTitle("Altyazı dili") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mediaInfo.subtitles, key = { it.languageCode }) { sub ->
                                    ToggleChip(
                                        label = subtitleLabel(sub.languageName, sub.isAutoGenerated),
                                        checked = subtitleLang == sub.languageCode
                                    ) { checked -> subtitleLang = if (checked) sub.languageCode else null }
                                }
                            }
                        }
                        item { Note("Seçtiğiniz altyazı videoya gömülür. Seçim yoksa altyazı eklenmez.") }
                    }

                    if (recommended.video != null || recommended.audio != null) {
                        item { SectionTitle("Önerilen") }
                        item {
                            RecommendedCard(
                                pair = recommended,
                                selected = selectedVideo == recommended.video && selectedAudio == recommended.audio,
                                onClick = {
                                    selectedVideo = recommended.video
                                    selectedAudio = recommended.audio
                                }
                            )
                        }
                    }

                    formatSection(
                        title = "Ses",
                        formats = audio,
                        selected = selectedAudio,
                        onToggle = { f -> selectedAudio = if (selectedAudio == f) null else f }
                    )
                    if (!audioOnly) {
                        formatSection(
                            title = "Video (ses yok)",
                            formats = videoOnly,
                            selected = selectedVideo,
                            onToggle = { f -> selectedVideo = if (selectedVideo == f) null else f }
                        )
                        formatSection(
                            title = "Video (sesli)",
                            formats = videoWithAudio,
                            selected = selectedVideo,
                            onToggle = { f -> selectedVideo = if (selectedVideo == f) null else f }
                        )
                    }

                    item {
                        Row(modifier = Modifier.padding(top = 16.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Çoğu platform sesi ve görüntüyü ayrı sunar. Bir ses ve bir görüntü biçimi seçerseniz " +
                                    "Neko ikisini tek dosyada birleştirir. Sessiz bir görüntüyü tek başına seçerseniz " +
                                    "en iyi ses otomatik eklenir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Summary and download
                HorizontalDivider(color = glassEdge())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectionSummary(selectedVideo, selectedAudio),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatSize(selectionSize(selectedVideo, selectedAudio, mediaInfo))
                                ?.let { "Tahmini boyut: $it" } ?: "Boyut bilinmiyor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(buildOptions(selectedVideo, selectedAudio, subtitleLang)) },
                        enabled = canConfirm,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("İndir", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Section title, a two-column grid of cards, and a show-all toggle beyond six entries. */
private fun LazyListScope.formatSection(
    title: String,
    formats: List<FormatOption>,
    selected: FormatOption?,
    onToggle: (FormatOption) -> Unit
) {
    if (formats.isEmpty()) return
    item(key = "title-$title") {
        SectionTitle(title)
    }
    item(key = "grid-$title") {
        var expanded by remember { mutableStateOf(false) }
        val visible = if (expanded) formats else formats.take(COLLAPSED_COUNT)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { f ->
                        FormatCard(
                            format = f,
                            selected = f == selected,
                            onClick = { onToggle(f) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (formats.size > COLLAPSED_COUNT) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "Daha az göster" else "${formats.size} biçimin tümünü göster",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaHeader(info: MediaInfo) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(
            modifier = Modifier
                .width(144.dp)
                .height(81.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            info.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                Text(
                    text = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            info.uploader?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Outlined card; filled when selected, like the selected segment elsewhere in Neko. */
@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val container = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, if (selected) container else MaterialTheme.colorScheme.outline, shape)
            .background(container)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(12.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}

@Composable
private fun FormatCard(format: FormatOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    SelectableCard(selected = selected, onClick = onClick, modifier = modifier) {
        val secondary = LocalContentColor.current.copy(alpha = 0.72f)
        Column {
            Text(
                text = formatTitle(format),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listOfNotNull(formatSize(format.fileSizeApprox) ?: "Boyut bilinmiyor", formatBitrate(format.bitrateKbps))
                    .joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = secondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = containerLabel(format),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StreamIcons(video = hasVideo(format), audio = !format.isVideoOnly)
            }
        }
    }
}

@Composable
private fun RecommendedCard(pair: FormatPair, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected = selected, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        val secondary = LocalContentColor.current.copy(alpha = 0.72f)
        Column {
            Text(
                text = selectionSummary(pair.video, pair.audio),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            val totalKbps = (pair.video?.bitrateKbps ?: 0) + (if (pair.video?.isVideoOnly != false) pair.audio?.bitrateKbps ?: 0 else 0)
            Text(
                text = listOfNotNull(
                    formatSize(pair.sizeBytes ?: pair.audio?.fileSizeApprox) ?: "Boyut bilinmiyor",
                    formatBitrate(totalKbps)
                ).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = secondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val codecs = listOfNotNull(codecName(pair.video?.vcodec), codecName(pair.audio?.acodec)).joinToString(", ")
                Text(
                    text = "${(pair.video?.ext ?: pair.audio?.ext ?: "").uppercase()} ($codecs)",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    modifier = Modifier.weight(1f)
                )
                StreamIcons(video = pair.video != null, audio = pair.audio != null)
            }
        }
    }
}

@Composable
private fun StreamIcons(video: Boolean, audio: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (video) Icon(Icons.Outlined.Videocam, contentDescription = "Görüntü", modifier = Modifier.size(16.dp))
        if (audio) Icon(Icons.Outlined.MusicNote, contentDescription = "Ses", modifier = Modifier.size(16.dp))
    }
}

private fun formatTitle(f: FormatOption): String = if (hasVideo(f)) {
    val fps = f.fps?.takeIf { it > 30 }?.let { " $it fps" }.orEmpty()
    "${f.height}p$fps"
} else {
    listOfNotNull(codecName(f.acodec), audioNote(f.formatNote)).joinToString(", ")
        .ifEmpty { f.ext.uppercase() }
}

private fun containerLabel(f: FormatOption): String {
    val codecs = if (hasVideo(f)) {
        listOfNotNull(codecName(f.vcodec), if (!f.isVideoOnly) codecName(f.acodec) else null)
    } else {
        listOfNotNull(codecName(f.acodec))
    }
    val container = if (f.isHls) "${f.ext.uppercase()} HLS" else f.ext.uppercase()
    return if (codecs.isEmpty()) container else "$container (${codecs.joinToString(", ")})"
}

private fun selectionSummary(video: FormatOption?, audio: FormatOption?): String {
    val v = video?.let { "${it.height}p ${codecName(it.vcodec).orEmpty()}".trim() }
    val a = audio?.let { listOfNotNull(codecName(it.acodec), formatBitrate(it.bitrateKbps)).joinToString(" ") }
    return when {
        v != null && a != null -> "$v + $a"
        v != null -> if (video.isVideoOnly) "$v + en iyi ses" else v
        a != null -> "Yalnızca ses: $a"
        else -> "Bir biçim seçin"
    }
}

private fun selectionSize(video: FormatOption?, audio: FormatOption?, info: MediaInfo): Long? = when {
    video == null -> audio?.fileSizeApprox
    // A silent video picked alone gets the best audio added
    video.isVideoOnly -> FormatPair(video, audio ?: bestAudio(info, ext = "m4a") ?: bestAudio(info)).sizeBytes
    else -> video.fileSizeApprox
}

/** Turns the picked streams into a yt-dlp format selector. */
private fun buildOptions(video: FormatOption?, audio: FormatOption?, subtitleLang: String?): DownloadOptions {
    if (video == null) {
        val a = requireNotNull(audio)
        val target = audioTargetFor(a)
        return DownloadOptions(
            isAudioOnly = true,
            formatSelector = a.formatId,
            audioFormat = target,
            ext = if (target == "best") a.ext else target,
            label = formatBitrate(a.bitrateKbps)
        )
    }
    val selector = when {
        audio != null -> "${video.formatId}+${audio.formatId}"
        // Prefer AAC so the merged file can stay MP4
        video.isVideoOnly -> "${video.formatId}+bestaudio[ext=m4a]/${video.formatId}+bestaudio"
        else -> video.formatId
    }
    val mp4 = video.ext == "mp4" && (audio == null || audio.ext == "m4a")
    return DownloadOptions(
        isAudioOnly = false,
        formatSelector = selector,
        ext = if (mp4) "mp4" else "mkv",
        label = "${video.height}p",
        embedSubtitles = subtitleLang != null,
        subtitleLang = subtitleLang
    )
}
