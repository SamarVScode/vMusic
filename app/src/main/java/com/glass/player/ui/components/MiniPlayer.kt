package com.glass.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.glass
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.Song
import dev.chrisbanes.haze.HazeState

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val capsuleShape = RoundedCornerShape(24.dp)

    // Art spring scale: 0.88 when paused -> 1.0 when playing
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = GlassSprings.gentle(),
        label = "mini_art_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .glass(
                hazeState = hazeState,
                shape = capsuleShape,
                blurRadius = 24.dp,
                refractionIndex = 0.045f,
                saturation = 1.6f
            )
            .pressableScale(onClick = onExpand)
    ) {
        // Main content row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail with spring scale
            val artShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .clip(artShape)
                    .background(GlassTheme.colors.fillSubtle)
                    .border(0.5.dp, GlassTheme.colors.glassBorderStart, artShape),
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
                        modifier = Modifier.size(24.dp),
                        tint = GlassTheme.colors.accentText
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                GlassText(
                    text = song.title,
                    style = GlassTheme.typography.headline,
                    color = GlassTheme.colors.labelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                GlassText(
                    text = "${song.artist} · ${if (song.isFlac) "FLAC" else "AAC"}",
                    style = GlassTheme.typography.subhead,
                    color = GlassTheme.colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play/Pause circular button in blood red (#C0111F)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GlassTheme.colors.accent)
                    .pressableScale(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    PauseIcon(modifier = Modifier.size(18.dp), tint = Color.White)
                } else {
                    PlayIcon(modifier = Modifier.size(18.dp), tint = Color.White)
                }
            }
        }

        // Bottom progress line in blood red
        val accentColor = GlassTheme.colors.accent
        val glowColor = GlassTheme.colors.accentGlow
        val trackBgColor = GlassTheme.colors.fillSubtle

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            val progressWidth = (size.width * progress.coerceIn(0f, 1f))

            // Track background
            drawRect(
                color = trackBgColor,
                size = size
            )

            // Progress fill with subtle glow
            if (progressWidth > 0f) {
                drawRect(
                    color = glowColor,
                    size = Size(progressWidth, size.height)
                )
                drawRect(
                    color = accentColor,
                    size = Size(progressWidth, size.height)
                )
            }
        }
    }
}
