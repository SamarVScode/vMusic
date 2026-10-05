package com.glass.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.HazeState
import com.glass.player.design.hazeSource
import com.glass.player.design.glass
import com.glass.player.design.motion.GlassSprings
import com.glass.player.design.motion.pressableScale
import com.glass.player.domain.AudioRepository
import com.glass.player.domain.Song
import com.glass.player.domain.update.AppUpdateManager
import com.glass.player.ui.components.LibraryTabIcon
import com.glass.player.ui.components.MiniPlayer
import com.glass.player.ui.components.NowPlayingSheet
import com.glass.player.ui.components.SettingsTabIcon
import com.glass.player.ui.screens.LibraryScreen
import com.glass.player.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab {
    Library,
    Settings
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val updateManager = AppUpdateManager(
            context = applicationContext,
            currentVersionName = BuildConfig.VERSION_NAME
        )

        setContent {
            GlassTheme {
                MainAppScreen(updateManager = updateManager)
            }
        }
    }
}

@Composable
fun MainAppScreen(updateManager: AppUpdateManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    // Active bottom tab
    var selectedTab by remember { mutableStateOf(AppTab.Library) }

    // Permission state
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Song library state
    var songs by remember { mutableStateOf(AudioRepository.getAudiophileSampleTracks()) }
    var isScanning by remember { mutableStateOf(false) }

    // Function to scan local tracks
    fun scanAudio() {
        coroutineScope.launch {
            isScanning = true
            try {
                val scanned = AudioRepository.scanLocalTracks(context)
                if (scanned.isNotEmpty()) {
                    songs = scanned
                }
            } finally {
                isScanning = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            scanAudio()
        }
    }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            scanAudio()
        }
    }

    // Playback state
    var currentSong by remember { mutableStateOf<Song?>(songs.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Playback progress ticker simulation (fluid tick when playing)
    LaunchedEffect(isPlaying, currentSong) {
        while (isActive && isPlaying && currentSong != null) {
            delay(200L)
            val duration = currentSong?.durationMs ?: 0L
            if (duration > 0) {
                if (currentPositionMs + 200L >= duration) {
                    if (isRepeat) {
                        currentPositionMs = 0L
                    } else {
                        // Advance to next song
                        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }
                        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < songs.size) {
                            currentIndex + 1
                        } else {
                            0
                        }
                        currentSong = songs.getOrNull(nextIndex)
                        currentPositionMs = 0L
                    }
                } else {
                    currentPositionMs += 200L
                }
            }
        }
    }

    fun playTrack(song: Song) {
        if (currentSong?.id == song.id) {
            isPlaying = !isPlaying
        } else {
            currentSong = song
            currentPositionMs = 0L
            isPlaying = true
        }
    }

    fun skipToPrevious() {
        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else songs.size - 1
        currentSong = songs.getOrNull(prevIndex)
        currentPositionMs = 0L
        isPlaying = true
    }

    fun skipToNext() {
        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }
        val nextIndex = if (isShuffle) {
            songs.indices.random()
        } else if (currentIndex != -1 && currentIndex + 1 < songs.size) {
            currentIndex + 1
        } else {
            0
        }
        currentSong = songs.getOrNull(nextIndex)
        currentPositionMs = 0L
        isPlaying = true
    }

    val currentDuration = currentSong?.durationMs ?: 1L
    val progressFraction = (currentPositionMs.toFloat() / currentDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.colors.background)
    ) {
        // Main Screen Content with Haze backdrop blur source
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "main_screen_tab_crossfade"
            ) { tab ->
                when (tab) {
                    AppTab.Library -> {
                        LibraryScreen(
                            songs = songs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onSongClick = { song -> playTrack(song) },
                            onScanClick = { scanAudio() },
                            isScanning = isScanning,
                            hasPermission = hasStoragePermission,
                            onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                            hazeState = hazeState
                        )
                    }

                    AppTab.Settings -> {
                        SettingsScreen(
                            updateManager = updateManager,
                            hazeState = hazeState,
                            onRefreshStorage = { scanAudio() }
                        )
                    }
                }
            }
        }

        // Bottom liquid glass drop (deep ambient gradient scrim behind floating controls)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x44000000),
                            Color(0xAA000000),
                            Color(0xEE000000),
                            Color(0xFA000000)
                        )
                    )
                )
        )

        // Floating MiniPlayer docked right above bottom tab navigation
        AnimatedVisibility(
            visible = currentSong != null && !isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 74.dp, start = 12.dp, end = 12.dp)
        ) {
            currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    progress = progressFraction,
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onExpand = { isNowPlayingExpanded = true },
                    hazeState = hazeState
                )
            }
        }

        // Floating Liquid Glass Tab Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
        ) {
            LiquidGlassTabBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                hazeState = hazeState
            )
        }

        // Modal Fullscreen NowPlayingSheet
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = GlassSprings.snappy()
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = GlassSprings.gentle()
            )
        ) {
            currentSong?.let { song ->
                NowPlayingSheet(
                    song = song,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = song.durationMs,
                    isShuffle = isShuffle,
                    isRepeat = isRepeat,
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onPreviousClick = { skipToPrevious() },
                    onNextClick = { skipToNext() },
                    onShuffleClick = { isShuffle = !isShuffle },
                    onRepeatClick = { isRepeat = !isRepeat },
                    onSeek = { targetMs -> currentPositionMs = targetMs },
                    onDismiss = { isNowPlayingExpanded = false },
                    hazeState = hazeState
                )
            }
        }
    }
}

/**
 * 100% Material-free Floating Liquid Glass Bottom Navigation Bar
 */
@Composable
private fun LiquidGlassTabBar(
    selectedTab: AppTab,
    onTabSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val barShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .glass(
                hazeState = hazeState,
                shape = barShape,
                blurRadius = 32.dp,
                refractionIndex = 0.05f,
                saturation = 1.6f
            )
            .border(1.dp, GlassTheme.colors.glassBorder, barShape)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabOffsetFraction by animateFloatAsState(
            targetValue = if (selectedTab == AppTab.Library) 0f else 1f,
            animationSpec = GlassSprings.snappy(),
            label = "tab_pill_offset"
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val pillWidth = maxWidth / 2
            val pillShape = RoundedCornerShape(26.dp)

            // Animated sliding liquid glass pill behind active tab
            Box(
                modifier = Modifier
                    .offset(x = pillWidth * tabOffsetFraction)
                    .width(pillWidth)
                    .fillMaxHeight()
                    .clip(pillShape)
                    .background(GlassTheme.colors.accent.copy(alpha = 0.22f))
                    .border(1.dp, GlassTheme.colors.accent.copy(alpha = 0.45f), pillShape)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Library Tab
                TabBarItem(
                    title = "Library",
                    isSelected = selectedTab == AppTab.Library,
                    icon = { tint -> LibraryTabIcon(modifier = Modifier.size(20.dp), tint = tint) },
                    onClick = { onTabSelect(AppTab.Library) },
                    modifier = Modifier.weight(1f)
                )

                // Settings Tab
                TabBarItem(
                    title = "Settings",
                    isSelected = selectedTab == AppTab.Settings,
                    icon = { tint -> SettingsTabIcon(modifier = Modifier.size(20.dp), tint = tint) },
                    onClick = { onTabSelect(AppTab.Settings) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabBarItem(
    title: String,
    isSelected: Boolean,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemShape = RoundedCornerShape(26.dp)
    val activeColor = GlassTheme.colors.accentText
    val inactiveColor = GlassTheme.colors.labelSecondary
    val tint = if (isSelected) activeColor else inactiveColor

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemShape)
            .pressableScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon(tint)

            GlassText(
                text = title,
                style = GlassTheme.typography.subhead.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = tint
            )
        }
    }
}
