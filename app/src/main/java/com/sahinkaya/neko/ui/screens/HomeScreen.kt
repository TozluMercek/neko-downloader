package com.sahinkaya.neko.ui.screens

import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahinkaya.neko.ui.MainViewModel
import com.sahinkaya.neko.ui.components.ClipboardDetectBanner
import com.sahinkaya.neko.ui.components.DownloadItemCard
import com.sahinkaya.neko.ui.components.FormatPickerBottomSheet
import com.sahinkaya.neko.ui.theme.NavBarClearance
import com.sahinkaya.neko.ui.theme.bottomHairline
import com.sahinkaya.neko.ui.theme.glass
import com.sahinkaya.neko.ui.theme.nekoGlassStyle

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val urlInput by viewModel.urlInput.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    val mediaInfo by viewModel.currentMediaInfo.collectAsState()
    val detectedClipboardUrl by viewModel.detectedClipboardUrl.collectAsState()
    val showFormatSheet by viewModel.showFormatSheet.collectAsState()
    val downloads by viewModel.downloads.collectAsState()

    val openDownloadSheet = {
        keyboardController?.hide()
        viewModel.openDownloadSheet()
    }
    // Pastes the first link on the clipboard into the field
    val pasteFromClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        val link = Regex("""https?://\S+""").find(clipText)?.value
        if (link != null) {
            viewModel.onUrlChange(link)
        } else {
            viewModel.reportMissingClipboardLink()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Link panel: full-width frosted strip, as in the Windows app
            item {
                LinkPanel(
                    url = urlInput,
                    error = analysisError,
                    onUrlChange = { viewModel.onUrlChange(it) },
                    onClear = { viewModel.onUrlChange("") },
                    onDone = openDownloadSheet
                )
            }

            // Clipboard Detect Banner (Feature 1)
            item {
                ClipboardDetectBanner(
                    detectedUrl = detectedClipboardUrl,
                    onApply = { viewModel.applyClipboardUrl() },
                    onDismiss = { viewModel.dismissClipboardBanner() }
                )
            }

            if (downloads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "Kuyruk boş",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kuyruğa eklediğiniz bağlantılar burada sırayla indirilir. " +
                                "İptal etmek, yeniden denemek veya kaldırmak için satırdaki düğmeleri kullanın.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kuyruk",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        TextButton(onClick = { viewModel.clearCompletedDownloads() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tamamlananları kaldır")
                        }
                    }
                }

                // Active Downloads List (Feature 5)
                items(items = downloads, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item) },
                        onRemove = { viewModel.removeDownload(item.id) }
                    )
                }
            }

            item {
                // Keeps the last card clear of the paste and download buttons
                Spacer(modifier = Modifier.navigationBarsPadding().height(NavBarClearance + 168.dp))
            }
        }

        ActionButtons(
            onPaste = pasteFromClipboard,
            onDownload = openDownloadSheet,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
        )
    }

    // "İndirmeden önce" sheet; the link's formats are only fetched if "Biçimleri seç" asks for them
    if (showFormatSheet) {
        val link = urlInput.trim()
        FormatPickerBottomSheet(
            url = link,
            mediaInfo = mediaInfo?.takeIf { it.url == link },
            error = analysisError,
            onLoadFormats = { viewModel.analyzeUrl(link) },
            onDismiss = { viewModel.dismissFormatSheet() },
            onStartDownload = { options -> viewModel.startDownload(options) }
        )
    }
}

/** Paste and download buttons stacked in the bottom-right corner. */
@Composable
private fun ActionButtons(onPaste: () -> Unit, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        FloatingActionButton(
            onClick = onPaste,
            shape = shape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(imageVector = Icons.Outlined.ContentPaste, contentDescription = "Bağlantıyı panodan yapıştır")
        }
        FloatingActionButton(
            onClick = onDownload,
            shape = shape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = "İndir")
        }
    }
}

/** Big underlined link field; downloading starts from the button in the bottom-right corner. */
@Composable
private fun LinkPanel(
    url: String,
    error: String?,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = RectangleShape, style = nekoGlassStyle(), edge = false)
            .bottomHairline()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
    ) {
        Column {
            LinkField(
                url = url,
                onUrlChange = onUrlChange,
                onClear = onClear,
                onDone = onDone,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "YouTube, Instagram, X, Reddit ve yt-dlp'nin desteklediği diğer siteler",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkField(
    url: String,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val linkStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal, fontSize = 26.sp)

    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = onSurface,
            unfocusedIndicatorColor = onSurface.copy(alpha = 0.35f),
            cursorColor = onSurface
    )

    // BasicTextField + DecorationBox so the text can start flush with the panel edge (no 16dp inset)
    BasicTextField(
        value = url,
        onValueChange = onUrlChange,
        textStyle = linkStyle.copy(color = onSurface),
        cursorBrush = SolidColor(onSurface),
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
        modifier = modifier
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = url,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(
                    text = "Bir bağlantı yapıştırın",
                    style = linkStyle,
                    color = onSurface.copy(alpha = 0.55f),
                    maxLines = 1
                )
            },
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle")
                    }
                }
            },
            colors = colors,
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(start = 0.dp, end = 0.dp)
        )
    }
}

