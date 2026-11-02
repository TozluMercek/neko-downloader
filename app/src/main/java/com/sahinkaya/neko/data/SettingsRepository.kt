package com.sahinkaya.neko.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class BackgroundMode {
    DEFAULT, CUSTOM, OFF
}

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_OUTPUT_TEMPLATE = stringPreferencesKey("output_template")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_AUTO_CLIPBOARD = booleanPreferencesKey("auto_clipboard")
        val KEY_EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        val KEY_BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val KEY_BACKGROUND_IMAGE = stringPreferencesKey("background_image")
        val KEY_BACKGROUND_BLUR = intPreferencesKey("background_blur")
        val KEY_DOWNLOAD_DIR = stringPreferencesKey("download_dir")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("download_notifications")

        const val DEFAULT_BLUR = 12
        const val MAX_BLUR = 60
    }

    val outputTemplate: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_OUTPUT_TEMPLATE] ?: "%(title)s [%(resolution)s].%(ext)s"
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: AppThemeMode.SYSTEM.name
        runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.SYSTEM)
    }

    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: false
    }

    val autoClipboard: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CLIPBOARD] ?: true
    }

    val embedMetadata: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMBED_METADATA] ?: true
    }

    /** Absolute path chosen by the user for all downloads; null keeps Movies/Neko and Music/Neko. */
    val downloadDir: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOAD_DIR]
    }

    /** Progress, finished and failed download notifications. */
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS] ?: true
    }

    val backgroundMode: Flow<BackgroundMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_BACKGROUND_MODE] ?: BackgroundMode.DEFAULT.name
        runCatching { BackgroundMode.valueOf(raw) }.getOrDefault(BackgroundMode.DEFAULT)
    }

    /** Absolute path of the user's own background, copied into app storage. */
    val backgroundImage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKGROUND_IMAGE]
    }

    val backgroundBlur: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKGROUND_BLUR] ?: DEFAULT_BLUR
    }

    suspend fun setOutputTemplate(template: String) {
        context.dataStore.edit { it[KEY_OUTPUT_TEMPLATE] = template }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setAutoClipboard(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CLIPBOARD] = enabled }
    }

    suspend fun setEmbedMetadata(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EMBED_METADATA] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setDownloadDir(path: String?) {
        context.dataStore.edit {
            if (path == null) it.remove(KEY_DOWNLOAD_DIR) else it[KEY_DOWNLOAD_DIR] = path
        }
    }

    suspend fun setBackgroundMode(mode: BackgroundMode) {
        context.dataStore.edit { it[KEY_BACKGROUND_MODE] = mode.name }
    }

    suspend fun setBackgroundImage(path: String) {
        context.dataStore.edit { it[KEY_BACKGROUND_IMAGE] = path }
    }

    suspend fun setBackgroundBlur(blur: Int) {
        context.dataStore.edit { it[KEY_BACKGROUND_BLUR] = blur.coerceIn(0, MAX_BLUR) }
    }
}
