package com.sahinkaya.neko.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.sahinkaya.neko.DownloaderApplication
import com.sahinkaya.neko.data.AppThemeMode
import com.sahinkaya.neko.data.BackgroundMode
import com.sahinkaya.neko.data.DownloadRepository
import com.sahinkaya.neko.data.LibraryFile
import com.sahinkaya.neko.data.SettingsRepository
import com.sahinkaya.neko.engine.YoutubeDLEngine
import com.sahinkaya.neko.model.DownloadItem
import com.sahinkaya.neko.model.DownloadOptions
import com.sahinkaya.neko.model.DownloadStatus
import com.sahinkaya.neko.model.FormatOption
import com.sahinkaya.neko.model.MediaInfo
import com.sahinkaya.neko.model.MediaPlatform
import com.sahinkaya.neko.service.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EngineStatusUiState(
    val version: String = "Yükleniyor...",
    val isFfmpegAvailable: Boolean = false,
    val isUpdating: Boolean = false,
    val updateMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DownloaderApplication
    val downloadRepository: DownloadRepository = app.downloadRepository
    val settingsRepository: SettingsRepository = app.settingsRepository
    val youtubeDLEngine: YoutubeDLEngine = app.youtubeDLEngine

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _currentMediaInfo = MutableStateFlow<MediaInfo?>(null)
    val currentMediaInfo: StateFlow<MediaInfo?> = _currentMediaInfo.asStateFlow()

    private val _dominantColor = MutableStateFlow<Color?>(null)
    val dominantColor: StateFlow<Color?> = _dominantColor.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private val _showFormatSheet = MutableStateFlow(false)
    val showFormatSheet: StateFlow<Boolean> = _showFormatSheet.asStateFlow()

    private val _engineStatus = MutableStateFlow(EngineStatusUiState())
    val engineStatus: StateFlow<EngineStatusUiState> = _engineStatus.asStateFlow()

    val downloads: StateFlow<List<DownloadItem>> = downloadRepository.downloads
    val libraryFiles: StateFlow<List<LibraryFile>> = downloadRepository.libraryFiles

    val themeMode: StateFlow<AppThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val downloadDir: StateFlow<String?> = settingsRepository.downloadDir
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _downloadDirError = MutableStateFlow<String?>(null)
    val downloadDirError: StateFlow<String?> = _downloadDirError.asStateFlow()

    val backgroundMode: StateFlow<BackgroundMode> = settingsRepository.backgroundMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundMode.DEFAULT)

    val backgroundImage: StateFlow<String?> = settingsRepository.backgroundImage
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val backgroundBlur: StateFlow<Int> = settingsRepository.backgroundBlur
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_BLUR)

    init {
        loadEngineStatus()
    }

    // Bumped each time a link arrives from the share sheet, so the UI can jump to the link field
    private val _shareRequests = MutableStateFlow(0)
    val shareRequests: StateFlow<Int> = _shareRequests.asStateFlow()

    fun onSharedLink(url: String) {
        onUrlChange(url)
        _shareRequests.value += 1
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        _analysisError.value = null
    }

    fun loadEngineStatus() {
        viewModelScope.launch {
            val ver = youtubeDLEngine.getVersion()
            _engineStatus.value = _engineStatus.value.copy(
                version = ver,
                isFfmpegAvailable = youtubeDLEngine.ffmpegAvailable
            )
        }
    }

    fun checkClipboard(text: String?) {
        if (text.isNullOrBlank()) return
        val trimmed = text.trim()
        val isValidUrl = (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
        if (isValidUrl && trimmed != _detectedClipboardUrl.value && trimmed != _urlInput.value) {
            _detectedClipboardUrl.value = trimmed
        }
    }

    fun dismissClipboardBanner() {
        _detectedClipboardUrl.value = null
    }

    fun applyClipboardUrl() {
        _detectedClipboardUrl.value?.let {
            _urlInput.value = it
            _detectedClipboardUrl.value = null
            analyzeUrl(it)
        }
    }

    // Link currently being fetched, so repeated taps don't start the same request twice
    private var analyzingUrl: String? = null

    /** Fetches formats, sizes and subtitles; only done when the user asks for the format list. */
    fun analyzeUrl(targetUrl: String? = null) {
        val url = (targetUrl ?: _urlInput.value).trim()
        if (url.isEmpty() || url == analyzingUrl || _currentMediaInfo.value?.url == url) return

        analyzingUrl = url
        _isAnalyzing.value = true
        _analysisError.value = null

        viewModelScope.launch {
            val result = youtubeDLEngine.fetchMediaInfo(url)
            // A newer link replaced this one while it was loading
            if (analyzingUrl != url) return@launch
            analyzingUrl = null
            _isAnalyzing.value = false

            result.onSuccess { info ->
                _currentMediaInfo.value = info
                extractDominantColor(info.thumbnailUrl)
            }.onFailure { err ->
                _analysisError.value = err.localizedMessage ?: "Medya bilgisi alınamadı."
            }
        }
    }

    fun reportMissingClipboardLink() {
        _analysisError.value = "Panoda bir bağlantı bulunamadı."
    }

    /** The download button: opens the options sheet without fetching anything. */
    fun openDownloadSheet() {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) {
            _analysisError.value = "Önce bir bağlantı yapıştırın veya yazın."
            return
        }
        _analysisError.value = null
        _showFormatSheet.value = true
    }

    private fun extractDominantColor(imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(app)
                val request = ImageRequest.Builder(app)
                    .data(imageUrl)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    val palette = Palette.from(bitmap).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.mutedSwatch
                    swatch?.let {
                        _dominantColor.value = Color(it.rgb)
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Could not extract palette", e)
            }
        }
    }

    /**
     * Closing the sheet or the format list cancels this download: drop the fetched details
     * (and any fetch still running) so no preview of the video is left on screen.
     */
    fun dismissFormatSheet() {
        _showFormatSheet.value = false
        analyzingUrl = null
        _isAnalyzing.value = false
        _currentMediaInfo.value = null
    }

    fun startDownload(options: DownloadOptions) {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) return
        // Automatic downloads can start before the details have loaded; the link stands in for the title
        val info = _currentMediaInfo.value?.takeIf { it.url == url }
        val downloadItem = DownloadItem(
            url = url,
            title = info?.title ?: url.substringAfter("://").removePrefix("www."),
            thumbnailUrl = info?.thumbnailUrl,
            platform = info?.platform?.displayName ?: MediaPlatform.fromUrl(url).displayName,
            formatId = options.formatSelector,
            ext = options.ext,
            isAudioOnly = options.isAudioOnly,
            audioFormat = options.audioFormat,
            targetResolution = options.label,
            startTimeSeconds = options.startTime,
            endTimeSeconds = options.endTime,
            embedSubtitles = options.embedSubtitles,
            selectedSubtitleLang = options.subtitleLang,
            embedThumbnail = options.embedThumbnail,
            preferCompatible = options.preferCompatible,
            downloadPlaylist = options.downloadPlaylist,
            saveThumbnail = options.saveThumbnail,
            status = DownloadStatus.QUEUED
        )

        downloadRepository.addDownload(downloadItem)
        dismissFormatSheet()

        DownloadService.startDownload(app, downloadItem.id)
    }

    fun cancelDownload(id: String) {
        DownloadService.cancelDownload(app, id)
    }

    fun pauseDownload(id: String) {
        DownloadService.pauseDownload(app, id)
    }

    /** Starts a paused, cancelled or failed download again; yt-dlp picks up its partial file. */
    fun resumeDownload(item: DownloadItem) {
        downloadRepository.updateItem(item.id) { it.copy(status = DownloadStatus.QUEUED, errorMessage = null) }
        DownloadService.startDownload(app, item.id)
    }

    /** Takes the entry off the list, stopping it first if it is still running. */
    fun removeDownload(id: String) {
        val item = downloads.value.find { it.id == id }
        if (item != null && item.status in RUNNING_STATUSES) {
            DownloadService.cancelDownload(app, id)
        }
        downloadRepository.removeDownload(id)
    }

    fun retryDownload(item: DownloadItem) = resumeDownload(item)

    fun clearCompletedDownloads() {
        downloadRepository.clearCompleted()
    }

    fun deleteLibraryFile(path: String) {
        downloadRepository.deleteLibraryFile(path)
    }

    fun refreshLibrary() {
        downloadRepository.refreshLibrary()
    }

    fun updateEngine() {
        _engineStatus.value = _engineStatus.value.copy(isUpdating = true, updateMessage = null)
        viewModelScope.launch {
            val result = youtubeDLEngine.updateYoutubeDL()
            result.onSuccess { msg ->
                _engineStatus.value = _engineStatus.value.copy(
                    isUpdating = false,
                    updateMessage = "Başarılı: $msg",
                    version = youtubeDLEngine.getVersion()
                )
            }.onFailure { err ->
                _engineStatus.value = _engineStatus.value.copy(
                    isUpdating = false,
                    updateMessage = "Hata: ${err.localizedMessage}"
                )
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        viewModelScope.launch {
            settingsRepository.setBackgroundMode(mode)
        }
    }

    fun setBackgroundBlur(blur: Int) {
        viewModelScope.launch {
            settingsRepository.setBackgroundBlur(blur)
        }
    }

    /**
     * Copies the picked image into app storage so it survives the picker's temporary
     * permission, then switches the background to it. A new file name per pick keeps
     * Coil from showing the previous image from its cache.
     */
    fun setCustomBackground(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(app.filesDir, "backgrounds").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val target = File(dir, "background_${System.currentTimeMillis()}")
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    } ?: return@runCatching null
                    target.absolutePath
                }.onFailure { Log.w("MainViewModel", "Could not copy background", it) }.getOrNull()
            }
            if (path != null) {
                settingsRepository.setBackgroundImage(path)
                settingsRepository.setBackgroundMode(BackgroundMode.CUSTOM)
            }
        }
    }

    /**
     * Turns a folder picked with the system picker into a file path yt-dlp can write to.
     * Android only lets apps write arbitrary files by path inside Download and Documents
     * on internal storage, so anything else is rejected with a message.
     */
    fun setDownloadDirFromTree(treeUri: Uri) {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        val relative = docId?.takeIf { it.startsWith("primary:") }?.removePrefix("primary:")
        val topFolder = relative?.substringBefore('/')
        if (relative == null || topFolder !in WRITABLE_ROOTS) {
            _downloadDirError.value = "Bu klasöre indirme yapılamıyor. Dahili depolamada Download veya Documents içinden bir klasör seçin."
            return
        }
        _downloadDirError.value = null
        val path = File(Environment.getExternalStorageDirectory(), relative).absolutePath
        viewModelScope.launch {
            settingsRepository.setDownloadDir(path)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun resetDownloadDir() {
        _downloadDirError.value = null
        viewModelScope.launch {
            settingsRepository.setDownloadDir(null)
        }
    }

    private companion object {
        val RUNNING_STATUSES = setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.INITIALIZING,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PROCESSING_FFMPEG
        )
        val WRITABLE_ROOTS = setOf(Environment.DIRECTORY_DOWNLOADS, Environment.DIRECTORY_DOCUMENTS)
    }
}
