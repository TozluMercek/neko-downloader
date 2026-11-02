package com.sahinkaya.neko.data

import android.content.Context
import android.os.Environment
import com.sahinkaya.neko.model.DownloadItem
import com.sahinkaya.neko.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class LibraryFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val lastModified: Long
)

class DownloadRepository(private val context: Context) {

    companion object {
        private const val APP_FOLDER = "Neko"
        private const val LEGACY_APP_FOLDER = "ExpressiveDownloader"
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "opus", "ogg", "aac", "flac", "wav")
    }

    /** User-chosen folder for all downloads, kept in sync with settings by the Application. */
    @Volatile
    var customDownloadDir: String? = null

    private val json = Json { ignoreUnknownKeys = true }
    private val historyFile = File(context.filesDir, "download_history.json")

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _libraryFiles = MutableStateFlow<List<LibraryFile>>(emptyList())
    val libraryFiles: StateFlow<List<LibraryFile>> = _libraryFiles.asStateFlow()

    init {
        loadHistory()
        refreshLibrary()
    }

    private fun loadHistory() {
        try {
            if (historyFile.exists()) {
                val content = historyFile.readText()
                val list = json.decodeFromString<List<DownloadItem>>(content)
                _downloads.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory() {
        try {
            val content = json.encodeToString(_downloads.value)
            historyFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addDownload(item: DownloadItem) {
        _downloads.value = listOf(item) + _downloads.value.filter { it.id != item.id }
        saveHistory()
    }

    fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) transform(it) else it
        }
        saveHistory()
    }

    fun updateProgress(
        id: String,
        progress: Float,
        speed: String = "",
        eta: String = "",
        status: DownloadStatus = DownloadStatus.DOWNLOADING
    ) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(
                    progress = progress,
                    speed = speed,
                    eta = eta,
                    status = status
                )
            } else it
        }
    }

    fun markCompleted(id: String, filePath: String?) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(
                    progress = 100f,
                    status = DownloadStatus.COMPLETED,
                    localFilePath = filePath,
                    speed = "",
                    eta = ""
                )
            } else it
        }
        saveHistory()
        refreshLibrary()
    }

    fun markError(id: String, error: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(
                    status = DownloadStatus.ERROR,
                    errorMessage = error,
                    speed = "",
                    eta = ""
                )
            } else it
        }
        saveHistory()
    }

    fun markPaused(id: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speed = "", eta = "") else it
        }
        saveHistory()
    }

    fun cancelDownload(id: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(status = DownloadStatus.CANCELLED, speed = "", eta = "")
            } else it
        }
        saveHistory()
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filter { it.id != id }
        saveHistory()
    }

    fun clearCompleted() {
        _downloads.value = _downloads.value.filter { it.status != DownloadStatus.COMPLETED }
        saveHistory()
    }

    fun getDownloadDir(isAudioOnly: Boolean): File {
        customDownloadDir?.let { custom ->
            return File(custom).apply { if (!exists()) mkdirs() }
        }
        val baseDir = if (isAudioOnly) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }
        val appFolder = File(baseDir, APP_FOLDER)
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    /** Where "Küçük resmi kaydet" puts thumbnails: the chosen folder, or Pictures/Neko (Movies only takes video). */
    fun getThumbnailDir(): File {
        val dir = customDownloadDir?.let { File(it) }
            ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_FOLDER)
        return dir.apply { if (!exists()) mkdirs() }
    }

    fun refreshLibrary() {
        val results = mutableListOf<LibraryFile>()
        listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        ).flatMap { rootDir ->
            // Also scan the folder used before the app was renamed to Neko
            listOf(File(rootDir, APP_FOLDER), File(rootDir, LEGACY_APP_FOLDER))
        }.plus(listOfNotNull(customDownloadDir?.let { File(it) })).distinct().forEach { folder ->
            if (folder.exists() && folder.isDirectory) {
                // Depth 2 so playlist downloads (one subfolder per playlist) show up too
                folder.walkTopDown().maxDepth(2).forEach { file ->
                    val ext = file.extension.lowercase()
                    // Only media; saved thumbnails can share a custom folder
                    if (file.isFile && file.length() > 0 && (ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS)) {
                        val isVideo = ext in VIDEO_EXTENSIONS
                        results.add(
                            LibraryFile(
                                name = file.name,
                                path = file.absolutePath,
                                sizeBytes = file.length(),
                                isVideo = isVideo,
                                lastModified = file.lastModified()
                            )
                        )
                    }
                }
            }
        }
        _libraryFiles.value = results.sortedByDescending { it.lastModified }
    }

    fun deleteLibraryFile(path: String): Boolean {
        val file = File(path)
        val deleted = if (file.exists()) file.delete() else false
        if (deleted) {
            refreshLibrary()
        }
        return deleted
    }
}
