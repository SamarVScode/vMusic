package com.glass.player.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.player.BuildConfig
import com.glass.player.design.GlassQualityTier
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.glass
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.update.AppUpdateManager
import com.glass.player.domain.update.UpdateState
import com.glass.player.ui.components.ChevronRightIcon
import com.glass.player.ui.components.GlassButton
import com.glass.player.ui.components.GlassButtonVariant
import com.glass.player.ui.components.GlassUpdateDialog
import com.glass.player.design.HazeState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    updateManager: AppUpdateManager,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    onRefreshStorage: (() -> Unit)? = null
) {
    val updateState by updateManager.updateState.collectAsState()
    val scope = rememberCoroutineScope()

    var bitPerfectEnabled by remember { mutableStateOf(true) }
    var hwFlacEnabled by remember { mutableStateOf(true) }
    var touchSheenEnabled by remember { mutableStateOf(true) }
    var eqBroadcastEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background)
    ) {
        // Grouped Settings Content scrolling smoothly under pinned header
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 110.dp, bottom = 160.dp)
        ) {
            // SECTION 1: AUDIO ENGINE
            SectionTitle(title = "AUDIO ENGINE (BIT-PERFECT)")

            InsetGroupCard(hazeState = hazeState) {
                // Bit-Perfect Output
                SettingToggleRow(
                    title = "Bit-Perfect Output",
                    subtitle = "Direct DAC HAL bypass for uncompressed stream",
                    checked = bitPerfectEnabled,
                    onCheckedChange = { bitPerfectEnabled = it }
                )

                SettingDivider()

                // Hardware FLAC Decoder
                SettingToggleRow(
                    title = "Hardware FLAC Decoder",
                    subtitle = "DSP offload for lossless 24-bit/192kHz streams",
                    checked = hwFlacEnabled,
                    onCheckedChange = { hwFlacEnabled = it }
                )

                SettingDivider()

                // Buffer 200ms
                SettingValueRow(
                    title = "Playback Buffer",
                    subtitle = "Low-latency jitter prevention",
                    value = "200 ms"
                )

                SettingDivider()

                // Equalizer broadcast note (for Poweramp & System EQ)
                SettingIndicatorRow(
                    title = "Equalizer Broadcast Support",
                    subtitle = "AudioEffect session ID broadcast for system and external equalizers",
                    active = eqBroadcastEnabled
                )

                SettingDivider()

                // Refresh Local Storage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressableScale(onClick = { onRefreshStorage?.invoke() })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        GlassText(
                            text = "Refresh Local Storage",
                            style = GlassTheme.typography.headline,
                            color = GlassTheme.colors.labelPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        GlassText(
                            text = "Rescan device MediaStore for audio files",
                            style = GlassTheme.typography.subhead,
                            color = GlassTheme.colors.labelSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    GlassButton(
                        text = "Rescan",
                        variant = GlassButtonVariant.Primary,
                        onClick = { onRefreshStorage?.invoke() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 2: LIQUID GLASS ENGINE
            SectionTitle(title = "LIQUID GLASS ENGINE")

            InsetGroupCard(hazeState = hazeState) {
                // AGSL Refraction Tier A
                SettingValueRow(
                    title = "AGSL Refraction Tier",
                    subtitle = "Real-time chromatic dispersion shader",
                    value = "Tier A (AGSL)"
                )

                SettingDivider()

                // Dynamic Touch Sheen
                SettingToggleRow(
                    title = "Dynamic Touch Sheen",
                    subtitle = "Interactive specular highlights on gesture",
                    checked = touchSheenEnabled,
                    onCheckedChange = { touchSheenEnabled = it }
                )

                SettingDivider()

                // Backdrop Saturation
                SettingValueRow(
                    title = "Backdrop Vibrance",
                    subtitle = "Luminance-aware saturation multiplier",
                    value = "1.6x Boost"
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SECTION 3: ABOUT & UPDATES
            SectionTitle(title = "ABOUT & UPDATES")

            InsetGroupCard(hazeState = hazeState) {
                // App Version
                SettingValueRow(
                    title = "vMusic Version",
                    subtitle = "Pure Liquid Glass Audiophile Edition",
                    value = "v${BuildConfig.VERSION_NAME}"
                )

                SettingDivider()

                // Check for Updates Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressableScale(onClick = {
                            scope.launch {
                                updateManager.checkForUpdates()
                            }
                        })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        GlassText(
                            text = "Check for Updates",
                            style = GlassTheme.typography.headline,
                            color = GlassTheme.colors.accentText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        GlassText(
                            text = "Queries latest GitHub releases for updates",
                            style = GlassTheme.typography.subhead,
                            color = GlassTheme.colors.labelSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    ChevronRightIcon(
                        modifier = Modifier.size(16.dp),
                        tint = GlassTheme.colors.labelSecondary
                    )
                }
            }
        }

        // Pinned Liquid Glass Top Bar protecting Status Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .glass(
                    hazeState = hazeState,
                    shape = RectangleShape,
                    blurRadius = 28.dp,
                    refractionIndex = 0.04f
                )
                .border(0.5.dp, GlassTheme.colors.glassBorder, RectangleShape)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            GlassText(
                text = "Settings",
                style = GlassTheme.typography.largeTitle,
                color = GlassTheme.colors.labelPrimary
            )
        }

        // GitHub Release Update Dialog
        when (val state = updateState) {
            is UpdateState.Available -> {
                GlassUpdateDialog(
                    state = state,
                    onStartDownload = { url ->
                        scope.launch {
                            updateManager.downloadUpdate(url)
                        }
                    },
                    onInstall = { uri ->
                        updateManager.launchInstallation(uri)
                    },
                    onDismiss = {}
                )
            }
            is UpdateState.Downloading -> {
                GlassUpdateDialog(
                    state = state,
                    onStartDownload = {},
                    onInstall = {},
                    onDismiss = {}
                )
            }
            is UpdateState.ReadyToInstall -> {
                GlassUpdateDialog(
                    state = state,
                    onStartDownload = {},
                    onInstall = { uri ->
                        updateManager.launchInstallation(uri)
                    },
                    onDismiss = {}
                )
            }
            is UpdateState.Error -> {
                GlassUpdateDialog(
                    state = state,
                    onStartDownload = {},
                    onInstall = {},
                    onDismiss = {}
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    GlassText(
        text = title,
        style = GlassTheme.typography.caption.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        ),
        color = GlassTheme.colors.labelSecondary,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun InsetGroupCard(
    hazeState: HazeState?,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(
                hazeState = hazeState,
                shape = cardShape,
                blurRadius = 24.dp,
                refractionIndex = 0.04f
            )
            .border(1.dp, GlassTheme.colors.glassBorder, cardShape)
            .clip(cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GlassText(
                text = title,
                style = GlassTheme.typography.headline,
                color = GlassTheme.colors.labelPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            GlassText(
                text = subtitle,
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Custom iOS-style Liquid Switch
        val switchWidth = 50.dp
        val switchHeight = 30.dp
        val thumbSize = 26.dp
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 21.dp else 3.dp,
            animationSpec = GlassSprings.bouncy(),
            label = "switch_thumb"
        )

        Box(
            modifier = Modifier
                .size(width = switchWidth, height = switchHeight)
                .clip(CircleShape)
                .background(
                    if (checked) GlassTheme.colors.accent else GlassTheme.colors.fillSubtle
                )
                .border(
                    width = 1.dp,
                    color = if (checked) GlassTheme.colors.accent else GlassTheme.colors.glassBorderStart,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GlassText(
                text = title,
                style = GlassTheme.typography.headline,
                color = GlassTheme.colors.labelPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            GlassText(
                text = subtitle,
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        val valuePillShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .clip(valuePillShape)
                .background(GlassTheme.colors.fillSubtle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassText(
                text = value,
                style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = GlassTheme.colors.accentText
            )
        }
    }
}

@Composable
private fun SettingIndicatorRow(
    title: String,
    subtitle: String,
    active: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GlassText(
                text = title,
                style = GlassTheme.typography.headline,
                color = GlassTheme.colors.labelPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            GlassText(
                text = subtitle,
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        val indicatorPillShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .clip(indicatorPillShape)
                .background(if (active) GlassTheme.colors.accent.copy(alpha = 0.16f) else GlassTheme.colors.fillSubtle)
                .border(0.8.dp, if (active) GlassTheme.colors.accent else GlassTheme.colors.glassBorderStart, indicatorPillShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassText(
                text = if (active) "ACTIVE" else "INACTIVE",
                style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = if (active) GlassTheme.colors.accentText else GlassTheme.colors.labelSecondary
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.6.dp)
            .background(GlassTheme.colors.separator)
    )
}
