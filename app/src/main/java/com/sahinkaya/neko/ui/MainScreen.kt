package com.sahinkaya.neko.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sahinkaya.neko.R
import com.sahinkaya.neko.data.AppThemeMode
import com.sahinkaya.neko.ui.screens.HomeScreen
import com.sahinkaya.neko.ui.screens.LibraryScreen
import com.sahinkaya.neko.ui.screens.SettingsScreen
import com.sahinkaya.neko.ui.theme.LocalBackdropHaze
import com.sahinkaya.neko.ui.theme.NekoBackdrop
import com.sahinkaya.neko.ui.theme.bottomHairline
import com.sahinkaya.neko.ui.theme.glass
import com.sahinkaya.neko.ui.theme.isDarkSurface
import com.sahinkaya.neko.ui.theme.nekoGlassStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val downloads by viewModel.downloads.collectAsState()
    val libraryFiles by viewModel.libraryFiles.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val backgroundImage by viewModel.backgroundImage.collectAsState()
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()

    // Observe lifecycle ON_RESUME for clipboard auto-detect (Feature 1)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    if (clipboard.hasPrimaryClip()) {
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        viewModel.checkClipboard(text)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    // A link shared to Neko lands in the Kuyruk link field, so make sure that is what's on screen
    val shareRequests by viewModel.shareRequests.collectAsState()
    var handledShareRequests by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(shareRequests) {
        // Skip requests already handled before a rotation or process restore
        if (shareRequests > handledShareRequests) {
            handledShareRequests = shareRequests
            selectedIndex = 0
            showSettings = false
        }
    }

    val backdropHaze = rememberHazeState()
    val dark = isDarkSurface()

    Box(modifier = modifier.fillMaxSize()) {
        NekoBackdrop(
            mode = backgroundMode,
            customImagePath = backgroundImage,
            blur = backgroundBlur,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(backdropHaze)
        )

        // No Scaffold or Surface here, so set the default text and icon colour for the theme
        CompositionLocalProvider(
            LocalBackdropHaze provides backdropHaze,
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NekoTopBar(
                    selectedIndex = selectedIndex,
                    queueCount = downloads.size,
                    archiveCount = libraryFiles.size,
                    isDark = dark,
                    onSelect = { selectedIndex = it },
                    onToggleTheme = {
                        viewModel.setThemeMode(if (dark) AppThemeMode.LIGHT else AppThemeMode.DARK)
                    },
                    onOpenSettings = { showSettings = true }
                )

                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "screenTransition"
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(viewModel = viewModel)
                        1 -> LibraryScreen(viewModel = viewModel)
                    }
                }
            }

            // Settings slides over everything as a frosted panel, like the Windows side panel
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glass(
                            shape = RectangleShape,
                            style = nekoGlassStyle(
                                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                                tintAlpha = 0.9f
                            ),
                            edge = false
                        )
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onClose = { showSettings = false },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun NekoTopBar(
    selectedIndex: Int,
    queueCount: Int,
    archiveCount: Int,
    isDark: Boolean,
    onSelect: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(
                shape = RectangleShape,
                style = nekoGlassStyle(tint = MaterialTheme.colorScheme.surfaceContainerHighest),
                edge = false
            )
            .bottomHairline()
            .statusBarsPadding()
            .height(52.dp)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_cat_icon),
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Neko",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(20.dp))

        TopBarTab(label = "Kuyruk", count = queueCount, selected = selectedIndex == 0, onClick = { onSelect(0) })
        Spacer(modifier = Modifier.width(16.dp))
        TopBarTab(label = "Arşiv", count = archiveCount, selected = selectedIndex == 1, onClick = { onSelect(1) })

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = if (isDark) "Açık temaya geç" else "Koyu temaya geç",
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Ayarlar",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Text tab with a count, underlined when selected (Windows "Kuyruk 0 / Arşiv 0"). */
@Composable
private fun TopBarTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textColor = if (selected) onSurface else onSurface.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(role = Role.Tab, onClick = onClick)
            // Underline drawn rather than laid out, so it can never widen the tab
            .drawBehind {
                if (selected) {
                    val stroke = 2.dp.toPx()
                    drawRect(
                        color = onSurface,
                        topLeft = Offset(0f, size.height - stroke),
                        size = Size(size.width, stroke)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                color = textColor.copy(alpha = textColor.alpha * 0.6f)
            )
        }
    }
}
