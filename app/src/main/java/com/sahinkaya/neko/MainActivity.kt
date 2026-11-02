package com.sahinkaya.neko

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import com.sahinkaya.neko.ui.components.NekoIntro
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sahinkaya.neko.data.AppThemeMode
import com.sahinkaya.neko.ui.MainScreen
import com.sahinkaya.neko.ui.MainViewModel
import com.sahinkaya.neko.ui.theme.NekoTheme
import com.sahinkaya.neko.share.SharedLink

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Short intro after the system splash: same cat, with the name added underneath
            var showIntro by rememberSaveable { mutableStateOf(savedInstanceState == null) }
            LaunchedEffect(Unit) {
                delay(1100)
                showIntro = false
            }

            // No thumbnail seed colour: Neko stays neutral and lets the backdrop carry the colour
            NekoTheme(darkTheme = isDark) {
                Box {
                    MainScreen(viewModel = viewModel)
                    AnimatedVisibility(
                        visible = showIntro,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(durationMillis = 400))
                    ) {
                        NekoIntro()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    // "Neko" in the share sheet: open the app with the shared link pasted, ready to preview
    private fun handleIncomingIntent(intent: Intent?) {
        SharedLink.from(intent)?.let { viewModel.onSharedLink(it) }
    }
}
