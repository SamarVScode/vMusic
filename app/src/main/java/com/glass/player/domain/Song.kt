package com.glass.player.domain

import com.glass.player.domain.model.Track
import java.util.Locale

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val sampleRate: Int = 96000,
    val bitDepth: Int = 24,
    val isFlac: Boolean = true,
    val mimeType: String = "audio/flac",
    val sizeBytes: Long = 0L
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    val hiResBadgeLabel: String
        get() = if (isFlac) {
            val khz = sampleRate / 1000
            "FLAC · ${bitDepth}-bit / ${khz}kHz"
        } else {
            "AAC / MP3"
        }

    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isFlac = isFlac,
        bitDepth = bitDepth,
        sampleRate = sampleRate,
        mimeType = mimeType
    )
}

fun Track.toSong(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    contentUri = contentUri,
    albumArtUri = albumArtUri,
    isFlac = isFlac,
    bitDepth = bitDepth,
    sampleRate = sampleRate,
    mimeType = mimeType
)
