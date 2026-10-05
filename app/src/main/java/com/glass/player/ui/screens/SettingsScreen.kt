package com.glass.player.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.player.BuildConfig
import com.glass.player.design.GlassQualityTier
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.HazeState
import com.glass.player.design.hazeSource
import com.glass.player.design.glass
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.update.AppUpdateManager
import com.glass.player.ui.components.ChevronRightIcon
import com.glass.player.ui.components.GlassButton
import com.glass.player.ui.components.GlassButtonVariant
import com.glass.player.ui.components.GlassUpdateDialog
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Screen Header
            GlassText(
                text = "Settings",
                style = GlassTheme.typography.largeTitle,
                color = GlassTheme.colors.labelPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1: AUDIO ENGINE
            SectionTitle(title = "AUDIO ENGINE")

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
                    title = "Equalizer Support",
                    subtitle = "Poweramp & System EQ broadcast enabled (AudioEffect session ID)",
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
                            text = "Fetch verified releases from GitHub",
                            style = GlassTheme.typography.subhead,
                            color = GlassTheme.colors.labelSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    GlassButton(
                        text = "Check Now",
                        variant = GlassButtonVariant.Primary,
                        onClick = {
                            scope.launch {
                                updateManager.checkForUpdates()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(160.dp)) // Clearance for bottom tabs & floating miniplayer
        }

        // Display GlassUpdateDialog on top when update is active
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

@Composable
private fun SectionTitle(title: String) {
    GlassText(
        text = title,
        style = GlassTheme.typography.caption.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = GlassTheme.colors.labelTertiary,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )
}

@Composable
private fun InsetGroupCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass(
                hazeState = hazeState,
                shape = cardShape,
                blurRadius = 20.dp,
                refractionIndex = 0.035f
            )
            .border(1.dp, GlassTheme.colors.glassBorder, cardShape)
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

        Spacer(modifier = Modifier.width(16.dp))

        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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

        Spacer(modifier = Modifier.width(14.dp))

        val pillShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(GlassTheme.colors.fillSubtle)
                .border(0.5.dp, GlassTheme.colors.glassBorderStart, pillShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            GlassText(
                text = value,
                style = GlassTheme.typography.tabularDigits.copy(fontWeight = FontWeight.Medium),
                color = GlassTheme.colors.labelPrimary
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glowing status indicator dot
                val dotColor = if (active) Color(0xFF34C759) else GlassTheme.colors.labelTertiary
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                GlassText(
                    text = title,
                    style = GlassTheme.typography.headline,
                    color = GlassTheme.colors.labelPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            GlassText(
                text = subtitle,
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary
            )
        }

        val badgeShape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .clip(badgeShape)
                .background(Color(0xFF34C759).copy(alpha = 0.15f))
                .border(0.5.dp, Color(0xFF34C759), badgeShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            GlassText(
                text = "ACTIVE",
                style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF34C759)
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
            .height(0.5.dp)
            .background(GlassTheme.colors.separator)
    )
}

/**
 * 100% Material-free custom Liquid Glass toggle switch
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val switchWidth = 48.dp
    val switchHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    val trackColor = if (checked) GlassTheme.colors.accent else GlassTheme.colors.fillSubtle
    val borderColor = if (checked) GlassTheme.colors.accent else GlassTheme.colors.separator

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) switchWidth - thumbSize - padding else padding,
        animationSpec = GlassSprings.snappy(),
        label = "switch_thumb"
    )

    val capsuleShape = RoundedCornerShape(switchHeight / 2)

    Box(
        modifier = modifier
            .size(width = switchWidth, height = switchHeight)
            .clip(capsuleShape)
            .background(trackColor)
            .border(1.dp, borderColor, capsuleShape)
            .pressableScale(onClick = { onCheckedChange(!checked) }),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb circle
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
