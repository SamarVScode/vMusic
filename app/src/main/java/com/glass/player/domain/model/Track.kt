package com.glass.player.domain.model

import java.util.Locale

/**
 * Domain model representing an audio track in the Glass FLAC Music Player (vMusic).
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val isFlac: Boolean = false,
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val mimeType: String = "audio/flac"
) {
    /**
     * Formatted track duration string (e.g. "3:15" or "1:02:40").
     */
    val formattedDuration: String
        get() = formatDuration(durationMs)

    /**
     * Audiophile quality badge descriptor (e.g. "FLAC 24-bit / 96 kHz" or "MP3 16-bit / 44.1 kHz").
     */
    val qualityBadge: String
        get() {
            val formatStr = if (isFlac) "FLAC" else mimeType.substringAfter("/").uppercase(Locale.US)
            val sampleRateKhz = if (sampleRate % 1000 == 0) {
                "${sampleRate / 1000} kHz"
            } else {
                String.format(Locale.US, "%.1f kHz", sampleRate / 1000.0)
            }
            return "$formatStr ${bitDepth}-bit / $sampleRateKhz"
        }
}

/**
 * Formats a duration in milliseconds to a human-readable mm:ss or hh:mm:ss string.
 * Example: 195000 -> "3:15"
 */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * Helper extension on Long duration.
 */
fun Long.formatDuration(): String = formatDuration(this)
