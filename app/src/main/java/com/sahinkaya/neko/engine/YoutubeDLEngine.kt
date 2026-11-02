package com.sahinkaya.neko.engine

import android.content.Context
import android.util.Log
import com.sahinkaya.neko.model.FormatOption
import com.sahinkaya.neko.model.MediaInfo
import com.sahinkaya.neko.model.MediaPlatform
import com.sahinkaya.neko.model.SubtitleOption
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class YoutubeDLEngine(private val context: Context) {

    companion object {
        private const val TAG = "YoutubeDLEngine"
    }

    var isInitialized: Boolean = false
        private set

    var initializationError: String? = null
        private set

    var ffmpegAvailable: Boolean = false
        private set

    // Completed once initialize() has finished, whether it succeeded or not
    private val initDone = CompletableDeferred<Unit>()

    /**
     * Waits for initialize() to finish. The engine starts in the background when the process
     * starts, so a share-sheet download or an early preview can arrive before it is ready.
     */
    suspend fun awaitInitialization(): Boolean {
        initDone.await()
        return isInitialized
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().init(context.applicationContext)
            isInitialized = true
            Log.d(TAG, "YoutubeDL initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e)
            initializationError = e.localizedMessage ?: "Bilinmeyen bir hata oluştu"
            isInitialized = false
        }

        try {
            FFmpeg.getInstance().init(context.applicationContext)
            ffmpegAvailable = true
            Log.d(TAG, "FFmpeg initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "FFmpeg initialization failed or not available", e)
            ffmpegAvailable = false
        }

        initDone.complete(Unit)
        isInitialized
    }

    fun engineUnavailableMessage(): String =
        "İndirme motoru başlatılamadı: ${initializationError ?: "bilinmeyen hata"}"

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        if (!awaitInitialization()) return@withContext "Yüklenmedi"
        try {
            // The library only remembers versions it installed itself; otherwise ask yt-dlp directly
            YoutubeDL.getInstance().version(context)
                ?: YoutubeDL.getInstance().execute(YoutubeDLRequest(emptyList()).addOption("--version"))
                    .out.trim().ifEmpty { "Bilinmiyor" }
        } catch (e: Exception) {
            "Hata: ${e.message}"
        }
    }

    suspend fun updateYoutubeDL(): Result<String> = withContext(Dispatchers.IO) {
        if (!awaitInitialization()) {
            return@withContext Result.failure(IllegalStateException("Motor başlatılmadı"))
        }
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
            val newVer = YoutubeDL.getInstance().version(context) ?: "Güncellendi"
            Result.success("$status ($newVer)")
        } catch (e: Exception) {
            Log.e(TAG, "yt-dlp güncelleme hatası", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMediaInfo(url: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        if (!awaitInitialization()) {
            return@withContext Result.failure(IllegalStateException(engineUnavailableMessage()))
        }
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--dump-single-json")
            }
            // Raw JSON instead of getInfo(): the library's model has no subtitle or codec details
            val json = JSONObject(YoutubeDL.getInstance().execute(request).out)
            val durationSec = json.optDouble("duration", 0.0).toLong()

            val formatsList = mutableListOf<FormatOption>()
            val formats = json.optJSONArray("formats")
            for (i in 0 until (formats?.length() ?: 0)) {
                val f = formats!!.optJSONObject(i) ?: continue
                val fid = f.str("format_id") ?: continue
                val ext = f.str("ext") ?: "mp4"
                // Storyboards (sb0, sb1...) are thumbnail sprites, not playable media
                if (ext == "mhtml" || fid.startsWith("sb")) continue

                val vcodec = f.str("vcodec")
                val acodec = f.str("acodec")
                val hasVideo = vcodec != null && vcodec != "none"
                val hasAudio = acodec != null && acodec != "none"
                if (!hasVideo && !hasAudio) continue

                val height = f.optInt("height", 0)
                val width = f.optInt("width", 0)
                val tbr = f.optDouble("tbr", 0.0).takeIf { !it.isNaN() } ?: 0.0
                val abr = f.optDouble("abr", 0.0).takeIf { !it.isNaN() } ?: 0.0
                val kbps = (if (tbr > 0) tbr else abr).toInt()
                val size = f.optLong("filesize", 0L).takeIf { it > 0 }
                    ?: f.optLong("filesize_approx", 0L).takeIf { it > 0 }
                    // kbps * seconds / 8 = kilobytes
                    ?: if (kbps > 0 && durationSec > 0) kbps * 1000L / 8 * durationSec else null

                formatsList.add(
                    FormatOption(
                        formatId = fid,
                        ext = ext,
                        resolution = if (height > 0) "${height}p" else null,
                        height = height.takeIf { it > 0 },
                        fps = f.optDouble("fps", 0.0).toInt().takeIf { it > 0 },
                        vcodec = vcodec,
                        acodec = acodec,
                        fileSizeApprox = size,
                        bitrateKbps = kbps.takeIf { it > 0 },
                        formatNote = f.str("format_note"),
                        width = width.takeIf { it > 0 },
                        isHls = f.str("protocol")?.contains("m3u8") == true,
                        isAudioOnly = hasAudio && !hasVideo,
                        isVideoOnly = hasVideo && !hasAudio
                    )
                )
            }

            val media = MediaInfo(
                url = url,
                id = json.str("id") ?: url.hashCode().toString(),
                title = json.str("title") ?: "İsimsiz Medya",
                thumbnailUrl = json.str("thumbnail"),
                uploader = json.str("uploader") ?: json.str("channel"),
                durationSeconds = durationSec,
                formats = formatsList,
                subtitles = parseSubtitles(json),
                isPlaylist = false,
                platform = MediaPlatform.fromUrl(url)
            )
            Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "fetchMediaInfo failed for $url", e)
            Result.failure(e)
        }
    }

    /**
     * Uploaded subtitles first, then the automatic captions in the video's own language
     * ("-orig"); YouTube's machine-translated captions would add a hundred more entries.
     */
    private fun parseSubtitles(json: JSONObject): List<SubtitleOption> {
        val result = mutableListOf<SubtitleOption>()
        json.optJSONObject("subtitles")?.let { subs ->
            for (code in subs.keys()) {
                if (code == "live_chat") continue
                val name = subs.optJSONArray(code)?.optJSONObject(0)?.str("name") ?: code
                result.add(SubtitleOption(languageCode = code, languageName = name))
            }
        }
        json.optJSONObject("automatic_captions")?.let { auto ->
            for (code in auto.keys()) {
                if (!code.endsWith("-orig")) continue
                val name = auto.optJSONArray(code)?.optJSONObject(0)?.str("name") ?: code
                result.add(SubtitleOption(languageCode = code, languageName = name, isAutoGenerated = true))
            }
        }
        return result
    }

    private fun JSONObject.str(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    fun buildDownloadRequest(
        url: String,
        outputDir: File,
        outputTemplate: String,
        formatId: String,
        isAudioOnly: Boolean,
        audioFormat: String? = null,
        startTimeSeconds: String? = null,
        endTimeSeconds: String? = null,
        embedSubtitles: Boolean = false,
        subtitleLang: String? = null,
        embedThumbnail: Boolean = true,
        preferCompatible: Boolean = true,
        downloadPlaylist: Boolean = false,
        saveThumbnail: Boolean = false,
        thumbnailDir: File? = null
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)

        // Only the finished file goes to outputDir. Android lets apps write just video files to
        // Movies and audio to Music, so partial downloads, thumbnails and subtitles (which are
        // embedded and then deleted) are kept in the app cache.
        val workDir = File(context.cacheDir, "ytdl-work").apply { mkdirs() }
        request.addOption("-P", "home:${outputDir.absolutePath}")
        request.addOption("-P", "temp:${workDir.absolutePath}")
        // Thumbnails stay in the cache unless the user wants to keep them as files
        val keepThumbnail = saveThumbnail && thumbnailDir != null
        request.addOption("-P", "thumbnail:${(if (keepThumbnail) thumbnailDir!! else workDir).absolutePath}")
        request.addOption("-P", "subtitle:${workDir.absolutePath}")
        // Playlist videos go into a folder named after the playlist
        request.addOption("-o", if (downloadPlaylist) "%(playlist_title&{}/|)s$outputTemplate" else outputTemplate)

        // Format selection: formatId is either "best" (automatic) or a selector built in the sheet
        val audioCodec = audioFormat?.lowercase() ?: "mp3"
        if (isAudioOnly) {
            request.addOption("-f", if (formatId != "best") formatId else "bestaudio/best")
            request.addOption("-x")
            request.addOption("--audio-format", audioCodec)
            // m4a and opus streams are only remuxed; quality 0 matters when re-encoding to mp3
            request.addOption("--audio-quality", "0")
        } else {
            val selector = when {
                formatId != "best" -> formatId
                // H.264 + AAC plays on every phone and TV
                preferCompatible -> "bestvideo[vcodec^=avc1]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                else -> "bestvideo+bestaudio/best"
            }
            request.addOption("-f", selector)
            request.addOption("--merge-output-format", "mp4/mkv")
        }

        // Clip / Trim section (--download-sections)
        if (!startTimeSeconds.isNullOrBlank() && !endTimeSeconds.isNullOrBlank()) {
            request.addOption("--download-sections", "*$startTimeSeconds-$endTimeSeconds")
        }

        // Subtitles (video only); automatic captions are only used when no uploaded ones exist
        if (embedSubtitles && !isAudioOnly) {
            request.addOption("--embed-subs")
            request.addOption("--write-subs")
            request.addOption("--write-auto-subs")
            request.addOption("--sub-langs", subtitleLang?.takeIf { it.isNotBlank() } ?: "all")
        }

        if (keepThumbnail) {
            // Saved as JPG so every gallery can show it; also kept after embedding
            request.addOption("--write-thumbnail")
            request.addOption("--convert-thumbnails", "jpg")
        }

        // Cover art; yt-dlp needs mutagen (not bundled) to embed it into Opus files
        if (embedThumbnail && !(isAudioOnly && audioCodec == "opus")) {
            request.addOption("--embed-thumbnail")
        }
        request.addOption("--add-metadata")

        request.addOption("--no-mtime")
        request.addOption(if (downloadPlaylist) "--yes-playlist" else "--no-playlist")
        request.addOption("--newline")

        return request
    }

    /** Kills the yt-dlp process started for [downloadId]; returns false if none was running. */
    fun stopDownload(downloadId: String): Boolean = try {
        YoutubeDL.getInstance().destroyProcessById(downloadId)
    } catch (e: Exception) {
        Log.w(TAG, "Could not stop download $downloadId", e)
        false
    }
}
