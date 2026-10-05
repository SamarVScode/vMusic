package com.glass.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.HazeState
import com.glass.player.design.hazeSource
import com.glass.player.design.glass
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.Song
import java.util.Locale

@Composable
fun NowPlayingSheet(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    // Spring scale for album art: 0.88 when paused -> 1.0 when playing
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = GlassSprings.gentle(),
        label = "art_spring_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background.copy(alpha = 0.85f))
            .glass(
                hazeState = hazeState,
                shape = sheetShape,
                blurRadius = 36.dp,
                refractionIndex = 0.05f
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Drag Handle Pill to dismiss/collapse
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.colors.separator)
                        .pressableScale(onClick = onDismiss)
                )

                Spacer(modifier = Modifier.height(14.dp))

                GlassText(
                    text = "NOW PLAYING",
                    style = GlassTheme.typography.caption.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GlassTheme.colors.labelTertiary,
                    textAlign = TextAlign.Center
                )
            }

            // Large Album Art with spring scale (0.88 paused -> 1.0 playing)
            val artCorner = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .clip(artCorner)
                    .background(GlassTheme.colors.fillSubtle)
                    .border(1.dp, GlassTheme.colors.glassBorderStart, artCorner),
                contentAlignment = Alignment.Center
            ) {
                if (!song.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ToneFallbackIcon(
                        modifier = Modifier.size(96.dp),
                        tint = GlassTheme.colors.accentText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track title & Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassText(
                    text = song.title,
                    style = GlassTheme.typography.title,
                    color = GlassTheme.colors.labelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassText(
                    text = song.artist,
                    style = GlassTheme.typography.headline,
                    color = GlassTheme.colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bit-perfect Hi-Res badge: "FLAC · 24-bit / 96kHz · Bit-Perfect"
                val badgeShape = RoundedCornerShape(12.dp)
                val khz = song.sampleRate / 1000
                val badgeText = if (song.isFlac) {
                    "FLAC · ${song.bitDepth}-bit / ${khz}kHz · Bit-Perfect"
                } else {
                    "Lossless · 16-bit / 44.1kHz"
                }

                Box(
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(GlassTheme.colors.accent.copy(alpha = 0.15f))
                        .border(1.dp, GlassTheme.colors.accent.copy(alpha = 0.5f), badgeShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    GlassText(
                        text = badgeText,
                        style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = GlassTheme.colors.accentText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Fluid Seek Bar & Time Labels
            FluidProgressBar(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transport controls: Shuffle, Previous, Play/Pause (64dp blood red), Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .pressableScale(onClick = onShuffleClick),
                    contentAlignment = Alignment.Center
                ) {
                    ShuffleIcon(
                        modifier = Modifier.size(24.dp),
                        tint = if (isShuffle) GlassTheme.colors.accentText else GlassTheme.colors.labelSecondary,
                        isActive = isShuffle
                    )
                }

                // Previous button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .pressableScale(onClick = onPreviousClick),
                    contentAlignment = Alignment.Center
                ) {
                    SkipPreviousIcon(
                        modifier = Modifier.size(28.dp),
                        tint = GlassTheme.colors.labelPrimary
                    )
                }

                // Play / Pause (Large 64dp Blood Red Spring Button)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GlassTheme.colors.accent)
                        .border(1.dp, GlassTheme.colors.glassBorderStart, CircleShape)
                        .pressableScale(pressedScale = 0.90f, onClick = onPlayPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        PauseIcon(
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    } else {
                        PlayIcon(
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }

                // Next button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .pressableScale(onClick = onNextClick),
                    contentAlignment = Alignment.Center
                ) {
                    SkipNextIcon(
                        modifier = Modifier.size(28.dp),
                        tint = GlassTheme.colors.labelPrimary
                    )
                }

                // Repeat button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .pressableScale(onClick = onRepeatClick),
                    contentAlignment = Alignment.Center
                ) {
                    RepeatIcon(
                        modifier = Modifier.size(24.dp),
                        tint = if (isRepeat) GlassTheme.colors.accentText else GlassTheme.colors.labelSecondary,
                        isActive = isRepeat
                    )
                }
            }
        }
    }
}

@Composable
private fun FluidProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragProgressFraction else actualFraction

    val animatedFraction by animateFloatAsState(
        targetValue = displayFraction,
        animationSpec = GlassSprings.snappy(),
        label = "progress_fraction"
    )

    val elapsedMs = (displayFraction * safeDuration).toLong()
    val remainingMs = (safeDuration - elapsedMs).coerceAtLeast(0L)

    Column(modifier = modifier) {
        // Fluid interactive bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((newFraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgressFraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragProgressFraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgressFraction * safeDuration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val trackColor = GlassTheme.colors.fillSubtle
            val accentColor = GlassTheme.colors.accent
            val glowColor = GlassTheme.colors.accentGlow

            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                val h = size.height
                val corner = CornerRadius(h / 2f, h / 2f)

                // Background track
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = corner
                )

                val filledWidth = size.width * animatedFraction

                if (filledWidth > 0f) {
                    // Glow
                    drawRoundRect(
                        color = glowColor,
                        topLeft = Offset(0f, -1.dp.toPx()),
                        size = Size(filledWidth, h + 2.dp.toPx()),
                        cornerRadius = corner
                    )
                    // Accent fill
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(filledWidth, h),
                        cornerRadius = corner
                    )
                }

                // Fluid thumb indicator
                val thumbRadius = if (isDragging) 8.dp.toPx() else 6.dp.toPx()
                val thumbCenter = Offset(filledWidth.coerceIn(thumbRadius, size.width - thumbRadius), h / 2f)

                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = thumbCenter
                )
                drawCircle(
                    color = accentColor,
                    radius = thumbRadius * 0.65f,
                    center = thumbCenter
                )
            }
        }

        // Time labels: elapsed & remaining
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassText(
                text = formatDuration(elapsedMs),
                style = GlassTheme.typography.tabularDigits,
                color = GlassTheme.colors.labelSecondary
            )

            GlassText(
                text = "-${formatDuration(remainingMs)}",
                style = GlassTheme.typography.tabularDigits,
                color = GlassTheme.colors.labelTertiary
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
