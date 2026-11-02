package com.sahinkaya.neko.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import com.sahinkaya.neko.ui.theme.NavBarClearance
import com.sahinkaya.neko.ui.theme.bottomHairline
import com.sahinkaya.neko.ui.theme.glass
import com.sahinkaya.neko.ui.theme.nekoGlassStyle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.sahinkaya.neko.ui.theme.GlassCard
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sahinkaya.neko.data.LibraryFile
import com.sahinkaya.neko.ui.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val libraryFiles by viewModel.libraryFiles.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<LibraryFile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshLibrary()
    }

    val trimmedQuery = query.trim()
    val filteredFiles = if (trimmedQuery.isEmpty()) {
        libraryFiles
    } else {
        libraryFiles.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ArchiveHeader(
            count = libraryFiles.size,
            query = query,
            onQueryChange = { query = it }
        )

        if (filteredFiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = if (libraryFiles.isEmpty()) "Arşiv boş" else "Sonuç yok",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (libraryFiles.isEmpty()) {
                        "Biten indirmeler burada listelenir. Her satırdan dosyayı oynatabilir veya paylaşabilirsiniz."
                    } else {
                        "\"$trimmedQuery\" adını içeren dosya yok."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = filteredFiles, key = { it.path }) { file ->
                    LibraryFileItem(
                        file = file,
                        onPlay = { openFile(context, file) },
                        onShare = { shareFile(context, file) },
                        onDelete = { fileToDelete = file }
                    )
                }

                item {
                    Spacer(modifier = Modifier.navigationBarsPadding().height(NavBarClearance))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Dosyayı Sil") },
            text = { Text("\"${target.name}\" cihazınızdan kalıcı olarak silinsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLibraryFile(target.path)
                        fileToDelete = null
                        Toast.makeText(context, "Dosya silindi", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun LibraryFileItem(
    file: LibraryFile,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeText = formatFileSize(file.sizeBytes)
    val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastModified))

    GlassCard(
        shape = MaterialTheme.shapes.large,
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (file.isVideo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (file.isVideo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Paylaş",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    return when {
        gb >= 1f -> String.format("%.2f GB", gb)
        mb >= 1f -> String.format("%.1f MB", mb)
        kb >= 1f -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

private fun openFile(context: Context, file: LibraryFile) {
    try {
        val f = File(file.path)
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
        } catch (e: Exception) {
            Uri.fromFile(f)
        }

        val mimeType = if (file.isVideo) "video/*" else "audio/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Dosya oynatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, file: LibraryFile) {
    try {
        val f = File(file.path)
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
        } catch (e: Exception) {
            Uri.fromFile(f)
        }

        val mimeType = if (file.isVideo) "video/*" else "audio/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Dosyayı Paylaş"))
    } catch (e: Exception) {
        Toast.makeText(context, "Paylaşılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Frosted strip at the top of the archive, as on Windows: a large completed-downloads count
 * with a hint, and an underlined search field (beside it on wide screens, below it on phones).
 */
@Composable
private fun ArchiveHeader(
    count: Int,
    query: String,
    onQueryChange: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = RectangleShape, style = nekoGlassStyle(), edge = false)
            .bottomHairline()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    ) {
        val wide = maxWidth >= 600.dp

        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArchiveCount(count = count, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(24.dp))
                ArchiveSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.width(320.dp))
            }
        } else {
            Column {
                ArchiveCount(count = count)
                Spacer(modifier = Modifier.height(12.dp))
                ArchiveSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ArchiveCount(count: Int, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "tamamlanan indirme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Oynatmak için satıra dokunun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArchiveSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = onSurface),
        cursorBrush = SolidColor(onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        modifier = modifier
    ) { innerTextField ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .bottomHairline()
                .padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Arşivde ara",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Aramayı temizle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}
