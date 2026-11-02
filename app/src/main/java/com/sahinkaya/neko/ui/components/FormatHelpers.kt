package com.sahinkaya.neko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahinkaya.neko.model.FormatOption
import com.sahinkaya.neko.model.MediaInfo
import java.util.Locale

private val turkish = Locale.forLanguageTag("tr")

internal fun formatSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        mb >= 1024 -> String.format(turkish, "%.2f GB", mb / 1024)
        else -> String.format(turkish, "%.2f MB", mb)
    }
}

internal fun formatBitrate(kbps: Int?): String? {
    if (kbps == null || kbps <= 0) return null
    return if (kbps >= 1000) String.format(turkish, "%.2f Mbps", kbps / 1000.0) else "$kbps kbps"
}

/** Readable codec names: avc1.640028 → H.264, vp09.00.40.08 → VP9, mp4a.40.2 → AAC. */
internal fun codecName(codec: String?): String? {
    val c = codec?.lowercase()?.takeIf { it.isNotBlank() && it != "none" } ?: return null
    return when {
        c.startsWith("avc") -> "H.264"
        c.startsWith("hev") || c.startsWith("hvc") -> "H.265"
        c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
        c.startsWith("av01") -> "AV1"
        c.startsWith("mp4a") -> "AAC"
        c.startsWith("opus") -> "Opus"
        else -> c.substringBefore('.').uppercase()
    }
}

/** yt-dlp's audio quality notes, in Turkish; other notes (e.g. a dub language) are kept as is. */
internal fun audioNote(note: String?): String? = when (note?.lowercase()) {
    null, "" -> null
    "ultralow" -> "çok düşük"
    "low" -> "düşük"
    "medium" -> "orta"
    "high" -> "yüksek"
    else -> note
}

/** Container to extract an audio stream into without re-encoding it. */
internal fun audioTargetFor(f: FormatOption): String = when {
    f.acodec?.startsWith("opus") == true -> "opus"
    f.acodec?.startsWith("mp4a") == true || f.ext == "m4a" -> "m4a"
    else -> "best"
}

internal fun hasVideo(f: FormatOption) = f.height != null && f.height > 0 && f.vcodec != null && f.vcodec != "none"

internal fun audioStreams(info: MediaInfo) =
    info.formats.filter { it.isAudioOnly }.sortedByDescending { it.bitrateKbps ?: 0 }

internal fun bestAudio(info: MediaInfo, ext: String? = null, codec: String? = null): FormatOption? =
    audioStreams(info).firstOrNull { (ext == null || it.ext == ext) && (codec == null || it.acodec?.startsWith(codec) == true) }

/** Video + audio pair that yt-dlp's automatic choice would download, for size estimates. */
internal data class FormatPair(val video: FormatOption?, val audio: FormatOption?) {
    val sizeBytes: Long?
        get() {
            val v = video?.fileSizeApprox ?: return null
            // Formats that already contain audio don't get a second track
            return if (video.isVideoOnly) v + (audio?.fileSizeApprox ?: 0L) else v
        }
}

private val byQuality = compareBy<FormatOption>({ it.height ?: 0 }, { it.bitrateKbps ?: 0 })

// yt-dlp picks the https stream over its HLS copy, so estimates should too
private fun videoCandidates(info: MediaInfo): List<FormatOption> =
    info.formats.filter(::hasVideo).let { all -> all.filterNot { it.isHls }.ifEmpty { all } }

/** H.264 video with AAC audio in MP4, the automatic "Uyumlu" choice. */
internal fun compatiblePair(info: MediaInfo): FormatPair {
    val candidates = videoCandidates(info)
    val video = candidates.filter { it.vcodec?.startsWith("avc1") == true }.maxWithOrNull(byQuality)
        ?: candidates.filter { it.ext == "mp4" }.maxWithOrNull(byQuality)
    return FormatPair(video, bestAudio(info, ext = "m4a") ?: bestAudio(info))
}

/** Highest resolution and bitrate, whatever the codec: the automatic "En iyi kalite" choice. */
internal fun bestQualityPair(info: MediaInfo): FormatPair =
    FormatPair(videoCandidates(info).maxWithOrNull(byQuality), bestAudio(info))

/** Outlined on/off chip; filled with a check mark when on, like the selected segment. */
@Composable
internal fun ToggleChip(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val shape = MaterialTheme.shapes.small
    val content = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1
        )
    }
}
