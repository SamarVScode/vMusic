package com.glass.player.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.update.UpdateState
import java.util.Locale

@Composable
fun GlassUpdateDialog(
    state: UpdateState,
    onStartDownload: (downloadUrl: String) -> Unit,
    onInstall: (fileUri: Uri) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    if (state is UpdateState.Idle || state is UpdateState.Checking) return

    val canDismiss = state !is UpdateState.Downloading

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(GlassSprings.gentle()) + scaleIn(GlassSprings.gentle(), initialScale = 0.92f),
            exit = fadeOut(GlassSprings.stiff()) + scaleOut(GlassSprings.stiff(), targetScale = 0.95f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val dialogShape = RoundedCornerShape(24.dp)
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 24.dp,
                            shape = dialogShape,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(dialogShape)
                        .background(GlassTheme.colors.background)
                        .border(
                            width = 1.dp,
                            brush = GlassTheme.colors.glassBorder,
                            shape = dialogShape
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        is UpdateState.Available -> AvailableContent(
                            state = state,
                            onUpdate = { onStartDownload(state.downloadUrl) },
                            onDismiss = onDismiss
                        )
                        is UpdateState.Downloading -> DownloadingContent(
                            state = state,
                            onCancel = onDismiss
                        )
                        is UpdateState.ReadyToInstall -> ReadyToInstallContent(
                            state = state,
                            onInstall = { onInstall(state.fileUri) },
                            onDismiss = onDismiss
                        )
                        is UpdateState.UpToDate -> UpToDateContent(onDismiss = onDismiss)
                        is UpdateState.Error -> ErrorContent(
                            state = state,
                            onRetry = onRetry,
                            onDismiss = onDismiss
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableContent(
    state: UpdateState.Available,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassText(
        text = "Update Available",
        style = GlassTheme.typography.headline,
        color = GlassTheme.colors.labelPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    val sizeStr = if (state.fileSizeMb > 0) String.format(Locale.US, " · %.1f MB", state.fileSizeMb) else ""
    GlassText(
        text = "Version ${state.versionName}$sizeStr",
        style = GlassTheme.typography.subhead,
        color = GlassTheme.colors.accentText,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    val notesShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(notesShape)
            .background(GlassTheme.colors.fillSubtle)
            .border(0.5.dp, GlassTheme.colors.separator, notesShape)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GlassText(
            text = state.changelog.ifBlank { "Performance improvements and bug fixes." },
            style = GlassTheme.typography.subhead,
            color = GlassTheme.colors.labelSecondary
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassButton(
            text = "Later",
            variant = GlassButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            onClick = onDismiss
        )
        GlassButton(
            text = "Update Now",
            variant = GlassButtonVariant.Primary,
            modifier = Modifier.weight(1f),
            onClick = onUpdate
        )
    }
}

@Composable
private fun DownloadingContent(
    state: UpdateState.Downloading,
    onCancel: () -> Unit
) {
    GlassText(
        text = "Downloading Update",
        style = GlassTheme.typography.headline,
        color = GlassTheme.colors.labelPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    val percentage = (state.progress.coerceIn(0f, 1f) * 100).toInt()
    val progressLabel = if (state.totalMb > 0) {
        String.format(Locale.US, "%.1f MB / %.1f MB · %d%%", state.downloadedMb, state.totalMb, percentage)
    } else {
        "$percentage%"
    }

    GlassText(
        text = progressLabel,
        style = GlassTheme.typography.tabularDigits,
        color = GlassTheme.colors.labelSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(22.dp))

    GlassProgressBar(
        progress = state.progress,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    GlassButton(
        text = "Cancel",
        variant = GlassButtonVariant.Secondary,
        modifier = Modifier.fillMaxWidth(),
        onClick = onCancel
    )
}

@Composable
private fun ReadyToInstallContent(
    state: UpdateState.ReadyToInstall,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassText(
        text = "Ready to Install",
        style = GlassTheme.typography.headline,
        color = GlassTheme.colors.labelPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassText(
        text = "vMusic update package is verified and ready to install.",
        style = GlassTheme.typography.subhead,
        color = GlassTheme.colors.labelSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassButton(
            text = "Later",
            variant = GlassButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            onClick = onDismiss
        )
        GlassButton(
            text = "Install Now",
            variant = GlassButtonVariant.Primary,
            modifier = Modifier.weight(1f),
            onClick = onInstall
        )
    }
}

@Composable
private fun UpToDateContent(onDismiss: () -> Unit) {
    GlassText(
        text = "vMusic is Up to Date",
        style = GlassTheme.typography.headline,
        color = GlassTheme.colors.labelPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassText(
        text = "You are running the latest version.",
        style = GlassTheme.typography.subhead,
        color = GlassTheme.colors.labelSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    GlassButton(
        text = "Done",
        variant = GlassButtonVariant.Secondary,
        modifier = Modifier.fillMaxWidth(),
        onClick = onDismiss
    )
}

@Composable
private fun ErrorContent(
    state: UpdateState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassText(
        text = "Update Check Failed",
        style = GlassTheme.typography.headline,
        color = GlassTheme.colors.accentText,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassText(
        text = state.message,
        style = GlassTheme.typography.subhead,
        color = GlassTheme.colors.labelSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassButton(
            text = "Dismiss",
            variant = GlassButtonVariant.Secondary,
            modifier = Modifier.weight(1f),
            onClick = onDismiss
        )
        GlassButton(
            text = "Retry",
            variant = GlassButtonVariant.Primary,
            modifier = Modifier.weight(1f),
            onClick = onRetry
        )
    }
}

@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = GlassSprings.snappy(),
        label = "glass_progress"
    )

    val trackColor = GlassTheme.colors.fillSubtle
    val accentColor = GlassTheme.colors.accent
    val glowColor = GlassTheme.colors.accentGlow

    Canvas(
        modifier = modifier
            .height(8.dp)
            .clip(CircleShape)
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)

        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = corner
        )

        val progressWidth = size.width * animatedProgress

        if (progressWidth > 0f) {
            drawRoundRect(
                color = glowColor,
                topLeft = Offset(0f, 0f),
                size = Size(progressWidth, size.height),
                cornerRadius = corner
            )

            drawRoundRect(
                color = accentColor,
                topLeft = Offset(0f, 0f),
                size = Size(progressWidth, size.height),
                cornerRadius = corner
            )
        }
    }
}

enum class GlassButtonVariant {
    Primary,
    Secondary
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    variant: GlassButtonVariant = GlassButtonVariant.Secondary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonShape = RoundedCornerShape(12.dp)
    val backgroundColor = when (variant) {
        GlassButtonVariant.Primary -> GlassTheme.colors.accent
        GlassButtonVariant.Secondary -> GlassTheme.colors.fillSubtle
    }
    val textColor = when (variant) {
        GlassButtonVariant.Primary -> Color.White
        GlassButtonVariant.Secondary -> GlassTheme.colors.labelPrimary
    }

    Box(
        modifier = modifier
            .pressableScale(enabled = enabled, onClick = onClick)
            .clip(buttonShape)
            .background(backgroundColor)
            .then(
                if (variant == GlassButtonVariant.Secondary) {
                    Modifier.border(1.dp, GlassTheme.colors.glassBorder, buttonShape)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassText(
            text = text,
            style = GlassTheme.typography.headline,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
