package com.sahinkaya.neko.service

import kotlinx.coroutines.runBlocking
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sahinkaya.neko.DownloaderApplication
import com.sahinkaya.neko.MainActivity
import com.sahinkaya.neko.R
import com.sahinkaya.neko.model.DownloadItem
import com.sahinkaya.neko.model.DownloadStatus
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        const val CHANNEL_ID = "download_channel_v1"
        // Separate channel so finished downloads can make a sound while progress stays silent
        const val CHANNEL_DONE_ID = "download_done_v1"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.sahinkaya.neko.action.START"
        const val ACTION_CANCEL = "com.sahinkaya.neko.action.CANCEL"
        const val ACTION_PAUSE = "com.sahinkaya.neko.action.PAUSE"
        const val EXTRA_ID = "extra_download_id"

        fun startDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_ID, downloadId)
            }
            context.startService(intent)
        }

        /** Stops yt-dlp but keeps the partial file in the cache, so starting again resumes it. */
        fun pauseDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private val activeJobs = mutableMapOf<String, Job>()
    // Downloads the user paused or cancelled; their yt-dlp failure is expected, not an error
    private val stoppedByUser = Collections.synchronizedSet(mutableSetOf<String>())
    private lateinit var notificationManager: NotificationManager

    // Ayarlar → İndirme bildirimleri; kept in sync with the setting while the service runs
    @Volatile
    private var notificationsEnabled = true

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val settings = (application as DownloaderApplication).settingsRepository
        notificationsEnabled = runBlocking { settings.notificationsEnabled.first() }
        serviceScope.launch { settings.notificationsEnabled.collect { notificationsEnabled = it } }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Neko::WakeLock").apply {
            setReferenceCounted(false)
            acquire(3 * 60 * 60 * 1000L) // 3 hours max safety
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val downloadId = intent.getStringExtra(EXTRA_ID) ?: return START_NOT_STICKY
        Log.d(TAG, "onStartCommand $action for $downloadId")

        when (action) {
            ACTION_START -> {
                // Android requires a notification for a foreground service; with notifications off it is a quiet one
                val notification = if (notificationsEnabled) buildNotification("İndirme hazırlanıyor…", 0, "", "") else buildQuietNotification()
                startForeground(NOTIFICATION_ID, notification)
                startDownloadTask(downloadId)
            }
            ACTION_CANCEL -> stopDownloadTask(downloadId, paused = false)
            ACTION_PAUSE -> stopDownloadTask(downloadId, paused = true)
        }

        return START_NOT_STICKY
    }

    private fun startDownloadTask(id: String) {
        if (activeJobs.containsKey(id)) return
        stoppedByUser.remove(id)

        val app = application as DownloaderApplication
        val repo = app.downloadRepository
        val engine = app.youtubeDLEngine
        val settings = app.settingsRepository

        val job = serviceScope.launch {
            val item = repo.downloads.value.find { it.id == id } ?: return@launch
            repo.updateProgress(id, 0f, status = DownloadStatus.DOWNLOADING)

            try {
                // A share-sheet download can start the process; wait for the engine to finish starting
                if (!engine.awaitInitialization()) {
                    throw IllegalStateException(engine.engineUnavailableMessage())
                }
                val startedAt = System.currentTimeMillis()
                val outputDir = repo.getDownloadDir(item.isAudioOnly)
                val template = settings.outputTemplate.first()

                val request = engine.buildDownloadRequest(
                    url = item.url,
                    outputDir = outputDir,
                    outputTemplate = template,
                    formatId = item.formatId,
                    isAudioOnly = item.isAudioOnly,
                    audioFormat = item.audioFormat,
                    startTimeSeconds = item.startTimeSeconds,
                    endTimeSeconds = item.endTimeSeconds,
                    embedSubtitles = item.embedSubtitles,
                    subtitleLang = item.selectedSubtitleLang,
                    embedThumbnail = item.embedThumbnail,
                    preferCompatible = item.preferCompatible,
                    downloadPlaylist = item.downloadPlaylist,
                    saveThumbnail = item.saveThumbnail,
                    thumbnailDir = if (item.saveThumbnail) repo.getThumbnailDir() else null
                )

                YoutubeDL.getInstance().execute(request, id) { progress, etaInSeconds, line ->
                    // Post-processing steps (merge, subtitles, thumbnail, playlist items) for diagnostics
                    if (line.startsWith("[") && !line.startsWith("[download]")) Log.d(TAG, line)
                    val etaStr = if (etaInSeconds > 0) {
                        val m = etaInSeconds / 60
                        val s = etaInSeconds % 60
                        String.format("%02d:%02d", m, s)
                    } else ""

                    // Extract speed from line if available
                    val speedRegex = Regex("(\\d+(\\.\\d+)?\\s*(K|M|G)?i?B/s)")
                    val speedMatch = speedRegex.find(line)?.value ?: ""

                    repo.updateProgress(
                        id = id,
                        progress = progress,
                        speed = speedMatch,
                        eta = etaStr,
                        status = DownloadStatus.DOWNLOADING
                    )

                    updateNotification(item.title, progress.toInt(), speedMatch, etaStr, id)
                }

                // Files written by this run (a playlist adds a subfolder; thumbnails may go elsewhere)
                val written = listOfNotNull(outputDir, if (item.saveThumbnail) repo.getThumbnailDir() else null)
                    .distinct()
                    .flatMap { dir -> dir.walkTopDown().maxDepth(2).filter { it.isFile && it.lastModified() >= startedAt - 2_000 }.toList() }
                if (written.isNotEmpty()) {
                    MediaScannerConnection.scanFile(applicationContext, written.map { it.absolutePath }.toTypedArray(), null) { _, _ -> }
                }
                val targetFile = written.filter { it.parentFile?.let { p -> p.startsWith(outputDir) } == true && it.extension.lowercase() != "jpg" }
                    .maxByOrNull { it.lastModified() }
                Log.d(TAG, "Download $id wrote: ${written.joinToString { it.name }}")

                repo.markCompleted(id, targetFile?.absolutePath)
                showCompletedNotification(id, item.title, targetFile)
            } catch (e: Exception) {
                Log.e(TAG, "Download execution failed for $id", e)
                // Paused or cancelled by the user: the status is already set, nothing failed
                if (!stoppedByUser.remove(id)) {
                    val message = e.localizedMessage ?: "İndirme sırasında bir hata oluştu"
                    repo.markError(id, message)
                    showFailedNotification(id, item.title, message)
                }
            } finally {
                activeJobs.remove(id)
                checkStopSelf()
            }
        }

        activeJobs[id] = job
    }

    private fun stopDownloadTask(id: String, paused: Boolean) {
        Log.d(TAG, if (paused) "Pausing $id" else "Cancelling $id")
        val app = application as DownloaderApplication
        stoppedByUser.add(id)
        app.youtubeDLEngine.stopDownload(id)
        activeJobs.remove(id)?.cancel()
        if (paused) app.downloadRepository.markPaused(id) else app.downloadRepository.cancelDownload(id)
        checkStopSelf()
    }

    private fun checkStopSelf() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "İndirme ilerlemesi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Süren indirmelerin ilerlemesini, hızını ve kalan süresini gösterir"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_DONE_ID, "Biten indirmeler", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "İndirme bittiğinde veya başarısız olduğunda haber verir"
                }
            )
        }
    }

    private fun buildNotification(
        title: String,
        progress: Int,
        speed: String,
        eta: String,
        downloadId: String? = null
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pOpenIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pOpenIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val subText = buildString {
            if (speed.isNotEmpty()) append(speed)
            if (eta.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append("Kalan: $eta")
            }
        }
        if (subText.isNotEmpty()) {
            builder.setContentText(subText)
        } else {
            builder.setContentText("İndiriliyor: %$progress")
        }

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        if (downloadId != null) {
            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_ID, downloadId)
            }
            val pCancelIntent = PendingIntent.getService(
                this, downloadId.hashCode(), cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "İptal Et", pCancelIntent)
        }

        return builder.build()
    }

    private fun updateNotification(
        title: String,
        progress: Int,
        speed: String,
        eta: String,
        downloadId: String
    ) {
        if (!notificationsEnabled) return
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(title, progress, speed, eta, downloadId)
        )
    }

    /** Finished download: tap opens the file, "Paylaş" shares it. Skipped when notifications are off. */
    private fun showCompletedNotification(downloadId: String, title: String, file: File?) {
        if (!notificationsEnabled) return
        val builder = NotificationCompat.Builder(this, CHANNEL_DONE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("İndirme tamamlandı")
            .setContentText(file?.name ?: title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(file?.name ?: title))
            .setAutoCancel(true)

        val uri = file?.let { runCatching { FileProvider.getUriForFile(this, "$packageName.provider", it) }.getOrNull() }
        if (file != null && uri != null) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            val open = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            val share = Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Paylaş"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            builder.setContentIntent(PendingIntent.getActivity(this, downloadId.hashCode(), open, flags))
            builder.addAction(0, "Paylaş", PendingIntent.getActivity(this, downloadId.hashCode() + 1, share, flags))
        }
        notificationManager.notify(downloadId.hashCode(), builder.build())
    }

    private fun showFailedNotification(downloadId: String, title: String, message: String) {
        if (!notificationsEnabled) return
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("İndirilemedi: $title")
            .setContentText(message.lineSequence().firstOrNull { it.isNotBlank() } ?: message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.take(500)))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(downloadId.hashCode(), notification)
    }

    /** The notification Android insists on while downloading, when the user turned notifications off. */
    private fun buildQuietNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Neko indiriyor")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
