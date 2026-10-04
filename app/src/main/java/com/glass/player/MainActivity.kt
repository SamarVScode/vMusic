package com.glass.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.domain.update.AppUpdateManager
import com.glass.player.domain.update.UpdateState
import com.glass.player.ui.components.GlassButton
import com.glass.player.ui.components.GlassButtonVariant
import com.glass.player.ui.components.GlassUpdateDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val updateManager = AppUpdateManager(
            context = applicationContext,
            currentVersionName = BuildConfig.VERSION_NAME
        )

        setContent {
            GlassTheme {
                MainScreen(updateManager = updateManager)
            }
        }
    }
}

@Composable
fun MainScreen(updateManager: AppUpdateManager) {
    val updateState by updateManager.updateState.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        GlassText(
            text = "vMusic",
            style = GlassTheme.typography.largeTitle,
            color = GlassTheme.colors.labelPrimary
        )

        GlassButton(
            text = "Check for Updates",
            variant = GlassButtonVariant.Primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            onClick = {
                scope.launch {
                    updateManager.checkForUpdates()
                }
            }
        )

        GlassUpdateDialog(
            state = updateState,
            onStartDownload = { url ->
                scope.launch {
                    updateManager.downloadUpdate(url)
                }
            },
            onInstall = { uri ->
                updateManager.launchInstallation(uri)
            },
            onDismiss = {
                // Update dialog dismissed
            }
        )
    }
}
