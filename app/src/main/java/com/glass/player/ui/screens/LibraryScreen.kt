package com.glass.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.RectangleShape
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

enum class LibraryFilter {
    All,
    HiRes,
    Flac
}

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
    var selectedFilter by remember { mutableStateOf(LibraryFilter.All) }

    val filteredSongs = remember(songs, searchQuery, selectedFilter) {
        var result = songs
        when (selectedFilter) {
            LibraryFilter.All -> {}
            LibraryFilter.HiRes -> {
                result = result.filter { it.bitDepth >= 24 || it.sampleRate >= 48000 }
            }
            LibraryFilter.Flac -> {
                result = result.filter { it.isFlac }
            }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
        result
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background)
    ) {
        // Track list scrolling behind the pinned header
        if (!hasPermission) {
            PermissionRequiredCard(
                onRequestPermission = onRequestPermission,
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 230.dp)
            )
        } else if (filteredSongs.isEmpty()) {
            EmptyLibraryCard(
                searchQuery = searchQuery,
                onScanClick = onScanClick,
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 230.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .rubberBandOverscroll(orientation = Orientation.Vertical),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 210.dp,
                    bottom = 160.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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

        // Pinned Liquid Glass Header protecting Status Bar
        PinnedGlassHeader(
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            onRefreshClick = onScanClick,
            isScanning = isScanning,
            trackCount = filteredSongs.size,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PinnedGlassHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit,
    onRefreshClick: () -> Unit,
    isScanning: Boolean,
    trackCount: Int,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass(
                hazeState = hazeState,
                shape = RectangleShape,
                blurRadius = 28.dp,
                refractionIndex = 0.04f
            )
            .border(0.5.dp, GlassTheme.colors.glassBorder, RectangleShape)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassText(
                    text = "Library",
                    style = GlassTheme.typography.largeTitle,
                    color = GlassTheme.colors.labelPrimary
                )

                GlassText(
                    text = "$trackCount Lossless",
                    style = GlassTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = GlassTheme.colors.labelSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    label = "All",
                    selected = selectedFilter == LibraryFilter.All,
                    onClick = { onFilterChange(LibraryFilter.All) }
                )

                FilterChip(
                    label = "24-Bit Hi-Res",
                    selected = selectedFilter == LibraryFilter.HiRes,
                    onClick = { onFilterChange(LibraryFilter.HiRes) }
                )

                FilterChip(
                    label = "FLAC",
                    selected = selectedFilter == LibraryFilter.Flac,
                    onClick = { onFilterChange(LibraryFilter.Flac) }
                )

                FilterChip(
                    label = if (isScanning) "Refreshing..." else "Refresh Storage",
                    selected = false,
                    isAction = true,
                    onClick = onRefreshClick
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isAction: Boolean = false
) {
    val chipShape = RoundedCornerShape(14.dp)
    val bgColor = when {
        selected -> GlassTheme.colors.accent.copy(alpha = 0.22f)
        isAction -> GlassTheme.colors.fillSubtle
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val borderColor = when {
        selected -> GlassTheme.colors.accent
        else -> GlassTheme.colors.glassBorderStart
    }
    val textColor = when {
        selected -> GlassTheme.colors.accentText
        isAction -> GlassTheme.colors.accentText
        else -> GlassTheme.colors.labelSecondary
    }

    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(bgColor)
            .border(0.8.dp, borderColor, chipShape)
            .pressableScale(pressedScale = 0.92f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassText(
            text = label,
            style = GlassTheme.typography.caption.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val pillShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .height(44.dp)
            .glass(
                hazeState = hazeState,
                shape = pillShape,
                blurRadius = 16.dp,
                refractionIndex = 0.03f
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
                modifier = Modifier.size(16.dp),
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
                        modifier = Modifier.size(14.dp),
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
