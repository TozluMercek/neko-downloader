package com.sahinkaya.neko

import android.app.Application
import com.sahinkaya.neko.data.DownloadRepository
import com.sahinkaya.neko.data.SettingsRepository
import com.sahinkaya.neko.engine.YoutubeDLEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloaderApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var youtubeDLEngine: YoutubeDLEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        downloadRepository = DownloadRepository(this)
        youtubeDLEngine = YoutubeDLEngine(this)

        applicationScope.launch(Dispatchers.IO) {
            youtubeDLEngine.initialize()
        }

        // Keep the repository on the chosen download folder and rescan when it changes
        applicationScope.launch(Dispatchers.IO) {
            settingsRepository.downloadDir.collect { dir ->
                downloadRepository.customDownloadDir = dir
                downloadRepository.refreshLibrary()
            }
        }
    }

    companion object {
        lateinit var instance: DownloaderApplication
            private set
    }
}
