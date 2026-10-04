package com.glass.player.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.Song

@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerShape = RoundedCornerShape(14.dp)
    val rowBackground = when {
        isSelected -> GlassTheme.colors.fillSubtle
        else -> GlassTheme.colors.background.copy(alpha = 0f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cornerShape)
            .background(rowBackground)
            .pressableScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail (48x48dp rounded 12dp) with fallback tone icon
        val artShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(artShape)
                .background(GlassTheme.colors.fillSubtle)
                .border(0.5.dp, GlassTheme.colors.glassBorderStart, artShape),
            contentAlignment = Alignment.Center
        ) {
            if (!song.albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(artShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                ToneFallbackIcon(
                    modifier = Modifier.size(24.dp),
                    tint = if (isPlaying) GlassTheme.colors.accentText else GlassTheme.colors.labelSecondary
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(GlassTheme.colors.background.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(
                        isPlaying = true,
                        modifier = Modifier.size(20.dp),
                        tint = GlassTheme.colors.accentText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassText(
                    text = song.title,
                    style = GlassTheme.typography.headline,
                    color = if (isPlaying) GlassTheme.colors.accentText else GlassTheme.colors.labelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Hi-Res / "FLAC" badge pill in blood red outline/fill
                if (song.isFlac) {
                    val badgeShape = RoundedCornerShape(4.dp)
                    Box(
                        modifier = Modifier
                            .clip(badgeShape)
                            .background(GlassTheme.colors.accent.copy(alpha = 0.16f))
                            .border(0.8.dp, GlassTheme.colors.accent, badgeShape)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        GlassText(
                            text = "FLAC",
                            style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = GlassTheme.colors.accentText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            GlassText(
                text = song.artist,
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Duration in tabular caption
        GlassText(
            text = song.formattedDuration,
            style = GlassTheme.typography.tabularDigits,
            color = GlassTheme.colors.labelTertiary
        )
    }
}
