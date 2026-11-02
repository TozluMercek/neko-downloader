package com.sahinkaya.neko.share

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.sahinkaya.neko.DownloaderApplication
import com.sahinkaya.neko.model.DownloadItem
import com.sahinkaya.neko.model.DownloadStatus
import com.sahinkaya.neko.model.MediaPlatform
import com.sahinkaya.neko.service.DownloadService

/**
 * "Hızlı İndir" share target. Has no UI: queues the shared link with the default settings
 * (best quality video), confirms with a toast and returns the user to the app they shared from.
 */
class QuickDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = SharedLink.from(intent)
        if (url == null) {
            Toast.makeText(this, "Paylaşılan içerikte bağlantı bulunamadı.", Toast.LENGTH_LONG).show()
        } else {
            val app = application as DownloaderApplication
            // Title is unknown until yt-dlp reads the page, so show the link itself in the queue
            val item = DownloadItem(
                url = url,
                title = url.substringAfter("://").removePrefix("www."),
                platform = MediaPlatform.fromUrl(url).displayName,
                formatId = "best",
                ext = "mp4",
                isAudioOnly = false,
                status = DownloadStatus.QUEUED
            )
            app.downloadRepository.addDownload(item)
            DownloadService.startDownload(app, item.id)
            Toast.makeText(this, "Neko indirmeye başladı.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
