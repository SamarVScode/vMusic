package com.glass.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.glass
import com.glass.player.design.motion.pressableScale
import com.glass.player.design.motion.rubberBandOverscroll
import com.glass.player.domain.Song
import com.glass.player.ui.components.CloseIcon
import com.glass.player.ui.components.GlassButton
import com.glass.player.ui.components.GlassButtonVariant
import com.glass.player.ui.components.SearchIcon
import com.glass.player.ui.components.SongRow
import com.glass.player.ui.components.ToneFallbackIcon
import dev.chrisbanes.haze.HazeState

@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onScanClick: () -> Unit,
    isScanning: Boolean = false,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            val q = searchQuery.trim().lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Screen Header
            Spacer(modifier = Modifier.height(24.dp))
            GlassText(
                text = "Library",
                style = GlassTheme.typography.largeTitle,
                color = GlassTheme.colors.labelPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar with glass pill styling
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Track count / scan button ("Scan Local Audio")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassText(
                    text = "${filteredSongs.size} Lossless Tracks",
                    style = GlassTheme.typography.subhead.copy(fontWeight = FontWeight.Medium),
                    color = GlassTheme.colors.labelSecondary
                )

                val scanShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .clip(scanShape)
                        .background(GlassTheme.colors.fillSubtle)
                        .border(1.dp, GlassTheme.colors.glassBorder, scanShape)
                        .pressableScale(enabled = !isScanning, onClick = onScanClick)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassText(
                        text = if (isScanning) "Refreshing..." else "Refresh Local Storage",
                        style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = GlassTheme.colors.accentText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Track List or Empty / Permission State
            if (!hasPermission) {
                PermissionRequiredCard(
                    onRequestPermission = onRequestPermission,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                )
            } else if (filteredSongs.isEmpty()) {
                EmptyLibraryCard(
                    searchQuery = searchQuery,
                    onScanClick = onScanClick,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                )
            } else {
                // LazyColumn with rubber-band overscroll and SongRow items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .rubberBandOverscroll(orientation = Orientation.Vertical),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isSelected = currentSong?.id == song.id,
                            isPlaying = isPlaying && (currentSong?.id == song.id),
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val pillShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .glass(
                hazeState = hazeState,
                shape = pillShape,
                blurRadius = 20.dp,
                refractionIndex = 0.035f
            )
            .border(1.dp, GlassTheme.colors.glassBorder, pillShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchIcon(
                modifier = Modifier.size(18.dp),
                tint = GlassTheme.colors.labelSecondary
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    GlassText(
                        text = "Search tracks, artists, albums...",
                        style = GlassTheme.typography.subhead,
                        color = GlassTheme.colors.labelTertiary
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = GlassTheme.typography.subhead.copy(color = GlassTheme.colors.labelPrimary),
                    cursorBrush = SolidColor(GlassTheme.colors.accent)
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    CloseIcon(
                        modifier = Modifier.size(16.dp),
                        tint = GlassTheme.colors.labelSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .glass(
                hazeState = hazeState,
                shape = cardShape,
                blurRadius = 24.dp
            )
            .border(1.dp, GlassTheme.colors.glassBorder, cardShape)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ToneFallbackIcon(
                modifier = Modifier.size(48.dp),
                tint = GlassTheme.colors.accentText
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassText(
                text = "Audio Permission Required",
                style = GlassTheme.typography.title,
                color = GlassTheme.colors.labelPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassText(
                text = "Allow vMusic to access your local audio files to scan and play bit-perfect FLAC audio.",
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassButton(
                text = "Grant Audio Permission",
                variant = GlassButtonVariant.Primary,
                onClick = onRequestPermission
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    searchQuery: String,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .glass(
                hazeState = hazeState,
                shape = cardShape,
                blurRadius = 24.dp
            )
            .border(1.dp, GlassTheme.colors.glassBorder, cardShape)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ToneFallbackIcon(
                modifier = Modifier.size(48.dp),
                tint = GlassTheme.colors.labelSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassText(
                text = if (searchQuery.isNotEmpty()) "No Matching Tracks" else "No Tracks Found",
                style = GlassTheme.typography.title,
                color = GlassTheme.colors.labelPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassText(
                text = if (searchQuery.isNotEmpty()) {
                    "No audio tracks matched \"$searchQuery\"."
                } else {
                    "Tap scan to index local FLAC and lossless files on your device."
                },
                style = GlassTheme.typography.subhead,
                color = GlassTheme.colors.labelSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassButton(
                text = "Scan Local Audio",
                variant = GlassButtonVariant.Secondary,
                onClick = onScanClick
            )
        }
    }
}
