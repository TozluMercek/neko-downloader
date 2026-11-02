package com.sahinkaya.neko.ui.screens

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import android.os.Environment
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahinkaya.neko.data.AppThemeMode
import com.sahinkaya.neko.data.BackgroundMode
import com.sahinkaya.neko.data.SettingsRepository
import com.sahinkaya.neko.ui.MainViewModel
import com.sahinkaya.neko.ui.components.Segment
import com.sahinkaya.neko.ui.components.SegmentedRow
import com.sahinkaya.neko.ui.theme.NavBarClearance
import com.sahinkaya.neko.ui.theme.bottomHairline
import com.sahinkaya.neko.ui.theme.glassEdge
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engineStatus by viewModel.engineStatus.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val downloadDir by viewModel.downloadDir.collectAsState()
    val downloadDirError by viewModel.downloadDirError.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val context = LocalContext.current
    fun hasNotificationPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission()) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }
    // Turning notifications on asks for Android's permission when it is missing (Android 13+)
    fun setNotifications(enabled: Boolean) {
        viewModel.setNotificationsEnabled(enabled)
        if (enabled && !hasNotificationPermission()) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val backgroundImage by viewModel.backgroundImage.collectAsState()
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()

    // Local slider value so dragging is smooth; saved when the drag ends
    var blurDraft by remember(backgroundBlur) { mutableFloatStateOf(backgroundBlur.toFloat()) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCustomBackground(uri)
    }
    val launchPicker = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.setDownloadDirFromTree(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Kapat")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Görünüm
        SectionLabel("Görünüm")
        SegmentedRow(
            options = listOf(
                Segment(AppThemeMode.SYSTEM, "Sistem", Icons.Outlined.DesktopWindows),
                Segment(AppThemeMode.LIGHT, "Açık", Icons.Outlined.LightMode),
                Segment(AppThemeMode.DARK, "Koyu", Icons.Outlined.DarkMode)
            ),
            selected = themeMode,
            onSelect = { viewModel.setThemeMode(it) }
        )
        Hint("Sistem seçeneği Android'in renk modunu takip eder.")

        Spacer(modifier = Modifier.height(20.dp))

        // Arka plan
        SectionLabel("Arka plan")
        SegmentedRow(
            options = listOf(
                Segment(BackgroundMode.DEFAULT, "Varsayılan", Icons.Outlined.Image),
                Segment(BackgroundMode.CUSTOM, "Kendi görselim", Icons.Outlined.Image),
                Segment(BackgroundMode.OFF, "Kapalı", Icons.Outlined.Close)
            ),
            selected = backgroundMode,
            onSelect = { mode ->
                // Choosing "Kendi görselim" before any image was picked opens the picker
                if (mode == BackgroundMode.CUSTOM && backgroundImage == null) {
                    launchPicker()
                } else {
                    viewModel.setBackgroundMode(mode)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (backgroundMode) {
                    BackgroundMode.DEFAULT -> "Neko'nun kara kedi görseli"
                    BackgroundMode.CUSTOM -> "Seçtiğiniz görsel"
                    BackgroundMode.OFF -> "Düz renk arka plan"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = launchPicker,
                shape = MaterialTheme.shapes.small
            ) {
                Text("Görsel seç…")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bulanıklık
        val blurEnabled = backgroundMode != BackgroundMode.OFF
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Bulanıklık", modifier = Modifier.weight(1f))
            Text(
                text = "${blurDraft.roundToInt()}",
                style = MaterialTheme.typography.titleSmall
            )
        }
        ThinSlider(
            value = blurDraft,
            onValueChange = { blurDraft = it },
            onValueChangeFinished = { viewModel.setBackgroundBlur(blurDraft.roundToInt()) },
            valueRange = 0f..SettingsRepository.MAX_BLUR.toFloat(),
            enabled = blurEnabled
        )
        Hint("0 görseli net gösterir; değer arttıkça görsel yumuşar ve yazılar daha kolay okunur.")

        SectionDivider()

        // Kaydetme konumu
        SectionLabel("Kaydetme konumu")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = downloadDir?.let { displayPath(it) } ?: "Movies/Neko ve Music/Neko",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .bottomHairline()
                    .padding(vertical = 10.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(
                onClick = { pickFolder.launch(null) },
                shape = MaterialTheme.shapes.small
            ) {
                Text("Değiştir…")
            }
        }
        Hint(
            if (downloadDir == null) {
                "Videolar Movies/Neko, müzikler Music/Neko klasörüne kaydedilir. " +
                    "Hepsini tek klasörde toplamak için Download veya Documents içinden bir klasör seçin."
            } else {
                "Videolar ve müzikler bu klasöre kaydedilir."
            }
        )
        downloadDirError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (downloadDir != null) {
            TextButton(
                onClick = { viewModel.resetDownloadDir() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Varsayılan klasörlere dön")
            }
        }

        SectionDivider()

        // Bildirimler
        SectionLabel("Bildirimler")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = notificationsEnabled, role = Role.Switch, onValueChange = ::setNotifications)
        ) {
            Text(
                text = "İndirme bildirimleri",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            BoxToggle(checked = notificationsEnabled)
        }
        Hint(
            if (notificationsEnabled) {
                "İndirme sürerken ilerleme, hız ve kalan süre; bittiğinde dosya bildirilir. " +
                    "Bildirime dokunarak dosyayı açabilir veya paylaşabilirsiniz."
            } else {
                "Kapalıyken yalnızca Android'in indirme sürerken zorunlu tuttuğu sessiz bildirim görünür."
            }
        )
        if (notificationsEnabled && !permissionGranted) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Neko'nun bildirim izni kapalı, bu yüzden bildirimler görünmüyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("İzin ver", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        SectionDivider()

        // İndirme motoru
        SectionLabel("İndirme motoru")
        Text(
            text = "yt-dlp ${engineStatus.version}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (engineStatus.isFfmpegAvailable) {
                "FFmpeg hazır: ses ve görüntü birleştirme, MP3 dönüştürme kullanılabilir."
            } else {
                "FFmpeg başlatılamadı. Birleştirme ve dönüştürme gerektiren biçimler indirilemez."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (engineStatus.isFfmpegAvailable) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.updateEngine() },
            enabled = !engineStatus.isUpdating,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (engineStatus.isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("yt-dlp güncelleniyor…")
            } else {
                Text("yt-dlp'yi güncelle")
            }
        }
        AnimatedVisibility(visible = engineStatus.updateMessage != null) {
            Hint(engineStatus.updateMessage.orEmpty())
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(NavBarClearance))
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = glassEdge(),
        modifier = Modifier.padding(vertical = 20.dp)
    )
}

/** Windows-style slider: hairline track, filled up to a small upright thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean
) {
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    val state = remember(valueRange) { SliderState(value = value, trackRange = valueRange) }
    // The caller owns the value; mirror it into the state on every recomposition
    state.value = value

    Slider(
        state = state,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        thumb = {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .background(ink, RoundedCornerShape(2.dp))
            )
        },
        track = { sliderState ->
            val range = sliderState.trackRange
            val span = range.endInclusive - range.start
            val fraction = if (span > 0f) ((sliderState.value - range.start) / span).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(ink.copy(alpha = ink.alpha * 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .background(ink)
                )
            }
        }
    )
}

/** Shows a storage path the way the Files app does, e.g. "Download/Neko" instead of /storage/emulated/0/... */
private fun displayPath(path: String): String {
    val root = Environment.getExternalStorageDirectory().absolutePath
    return path.removePrefix(root).trimStart('/').ifEmpty { path }
}

/**
 * Square-cornered on/off toggle in Neko's flat style: an outlined box with a small square knob
 * when off, filled with the knob on the right when on. The row around it handles the click.
 */
@Composable
private fun BoxToggle(checked: Boolean) {
    val shape = MaterialTheme.shapes.small
    val knobOffset by animateDpAsState(targetValue = if (checked) 22.dp else 4.dp, label = "knob")
    val track = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    val knob = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(shape)
            .border(1.dp, if (checked) track else MaterialTheme.colorScheme.outline, shape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobOffset)
                .size(18.dp)
                .background(knob, MaterialTheme.shapes.extraSmall)
        )
    }
}
