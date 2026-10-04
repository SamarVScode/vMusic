package com.glass.player.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.glass.player.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Singleton player controller managing Media3 ExoPlayer state and audiophile playback.
 * Provides real-time reactive StateFlows for Compose UI and system audio effect integration
 * (e.g., Poweramp Equalizer / third-party EQ action broadcasts).
 */
object PlayerController {

    private const val TAG = "PlayerController"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private var progressJob: Job? = null

    // State flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            handleAudioSessionId(audioSessionId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressPolling()
            } else {
                stopProgressPolling()
                exoPlayer?.let { _currentPositionMs.value = it.currentPosition.coerceAtLeast(0L) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    val dur = exoPlayer?.duration ?: 0L
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }
                    exoPlayer?.audioSessionId?.let { handleAudioSessionId(it) }
                }
                Player.STATE_ENDED -> {
                    skipNext()
                }
                Player.STATE_BUFFERING,
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            val trackId = mediaItem.mediaId.toLongOrNull()
            val matchedTrack = _playlist.value.find { it.id == trackId }
            if (matchedTrack != null) {
                _currentTrack.value = matchedTrack
                _durationMs.value = matchedTrack.durationMs
                _currentPositionMs.value = 0L
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPositionMs.value = newPosition.positionMs.coerceAtLeast(0L)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer error [${error.errorCode}]: ${error.message}", error)
        }
    }

    /**
     * Initializes the player controller with application context and configures audiophile ExoPlayer attributes.
     */
    @Synchronized
    fun initialize(context: Context): PlayerController {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val player = ExoPlayer.Builder(context.applicationContext)
                .setLooper(Looper.getMainLooper())
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .build()

            setPlayer(player)
        }
        return this
    }

    /**
     * Sets or attaches an existing ExoPlayer (e.g. from PlaybackService).
     */
    @Synchronized
    fun setPlayer(player: ExoPlayer) {
        if (exoPlayer === player) return
        exoPlayer?.removeListener(playerListener)
        exoPlayer = player
        player.addListener(playerListener)

        val initialSessionId = player.audioSessionId
        if (initialSessionId > 0 && initialSessionId != C.AUDIO_SESSION_ID_UNSET) {
            handleAudioSessionId(initialSessionId)
        }

        _isPlaying.value = player.isPlaying
        if (player.isPlaying) {
            startProgressPolling()
        }
    }

    /**
     * Retrieves the underlying ExoPlayer instance, initializing it if context is provided.
     */
    fun getPlayer(context: Context? = null): ExoPlayer? {
        if (exoPlayer == null && context != null) {
            initialize(context)
        }
        return exoPlayer
    }

    /**
     * Plays a track, optionally setting a new playlist.
     */
    fun playTrack(track: Track, newPlaylist: List<Track>? = null) {
        runOnMain {
            val player = exoPlayer ?: run {
                Log.w(TAG, "playTrack called before initialize(context)")
                return@runOnMain
            }

            val targetPlaylist = newPlaylist ?: _playlist.value.let { currentList ->
                if (currentList.any { it.id == track.id }) currentList
                else currentList + track
            }

            _playlist.value = targetPlaylist
            _currentTrack.value = track
            _durationMs.value = track.durationMs
            _currentPositionMs.value = 0L

            val targetIndex = targetPlaylist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

            val count = player.mediaItemCount
            val isMatching = count == targetPlaylist.size && (0 until count).all { i ->
                player.getMediaItemAt(i).mediaId == targetPlaylist[i].id.toString()
            }

            if (!isMatching) {
                val mediaItems = targetPlaylist.map { it.toMediaItem() }
                player.setMediaItems(mediaItems, targetIndex, 0L)
            } else {
                if (player.currentMediaItemIndex != targetIndex) {
                    player.seekTo(targetIndex, 0L)
                }
            }

            player.prepare()
            player.play()
        }
    }

    /**
     * Toggles between playing and paused states.
     */
    fun togglePlayPause() {
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0, 0L)
                }
                player.play()
            }
        }
    }

    /**
     * Skips to the next track or loops to the beginning of the playlist.
     */
    fun skipNext() {
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            val currentList = _playlist.value
            if (currentList.isEmpty()) return@runOnMain

            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.play()
            } else {
                val firstIndex = 0
                if (player.mediaItemCount > 0) {
                    player.seekTo(firstIndex, 0L)
                    player.play()
                } else {
                    currentList.firstOrNull()?.let { playTrack(it, currentList) }
                }
            }
        }
    }

    /**
     * Skips to the previous track or restarts the current track if > 3 seconds in.
     */
    fun skipPrevious() {
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            val currentList = _playlist.value
            if (currentList.isEmpty()) return@runOnMain

            if (player.currentPosition > 3000L) {
                seekTo(0L)
                player.play()
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.play()
            } else {
                val lastIndex = currentList.size - 1
                if (lastIndex >= 0 && player.mediaItemCount > lastIndex) {
                    player.seekTo(lastIndex, 0L)
                    player.play()
                }
            }
        }
    }

    /**
     * Seeks to the specified position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        runOnMain {
            val player = exoPlayer ?: return@runOnMain
            _currentPositionMs.value = positionMs.coerceAtLeast(0L)
            player.seekTo(positionMs)
        }
    }

    /**
     * Releases player resources and notifies system audio session closure.
     */
    fun release() {
        runOnMain {
            stopProgressPolling()
            if (currentAudioSessionId > 0 && currentAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                broadcastCloseAudioSession(currentAudioSessionId)
                currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET
            }
            exoPlayer?.removeListener(playerListener)
            exoPlayer?.release()
            exoPlayer = null
            _isPlaying.value = false
            _currentTrack.value = null
            _currentPositionMs.value = 0L
            _durationMs.value = 0L
        }
    }

    // --- Internal Helpers ---

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            scope.launch { block() }
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0L && dur != _durationMs.value) {
                        _durationMs.value = dur
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handleAudioSessionId(newSessionId: Int) {
        if (newSessionId <= 0 || newSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (currentAudioSessionId != newSessionId) {
            if (currentAudioSessionId > 0 && currentAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                broadcastCloseAudioSession(currentAudioSessionId)
            }
            currentAudioSessionId = newSessionId
            broadcastOpenAudioSession(newSessionId)
        }
    }

    private fun broadcastOpenAudioSession(sessionId: Int) {
        val ctx = appContext ?: return
        try {
            val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            ctx.sendBroadcast(openIntent)
            Log.d(TAG, "AudioEffect OPEN session broadcast sent for session ID $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast AudioEffect open session: ${e.message}")
        }
    }

    private fun broadcastCloseAudioSession(sessionId: Int) {
        val ctx = appContext ?: return
        try {
            val closeIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
            }
            ctx.sendBroadcast(closeIntent)
            Log.d(TAG, "AudioEffect CLOSE session broadcast sent for session ID $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast AudioEffect close session: ${e.message}")
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply {
                albumArtUri?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadata)
            .setMimeType(mimeType)
            .build()
    }
}
