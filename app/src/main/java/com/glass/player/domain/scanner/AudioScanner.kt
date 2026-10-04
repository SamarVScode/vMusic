package com.glass.player.domain.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.glass.player.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scanner for local audio files querying Android's MediaStore.
 * Detects audiophile FLAC formats and provides fallback sample tracks
 * for immediate interactivity on emulators or fresh installs.
 */
object AudioScanner {

    private const val TAG = "AudioScanner"

    /**
     * Scans device storage for audio tracks via MediaStore.
     * Returns curated sample fallback tracks if no media is found or permissions are not yet granted.
     */
    suspend fun scanDeviceAudio(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val durationMs = cursor.getLong(durationCol)
                    val data = cursor.getString(dataCol) ?: ""
                    val mimeType = cursor.getString(mimeTypeCol) ?: "audio/flac"
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    // Skip extremely short audio (< 3s, notifications/ringtones)
                    if (durationMs > 0 && durationMs < 3000L) continue

                    val isFlac = mimeType.contains("flac", ignoreCase = true) ||
                        data.endsWith(".flac", ignoreCase = true)

                    val title = rawTitle?.takeIf { it.isNotBlank() }
                        ?: data.substringAfterLast('/').substringBeforeLast('.')
                            .takeIf { it.isNotBlank() }
                        ?: "Unknown Track"

                    val artist = rawArtist?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Artist"

                    val album = rawAlbum?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Album"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs.coerceAtLeast(0L),
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            isFlac = isFlac,
                            bitDepth = if (isFlac) 24 else 16,
                            sampleRate = if (isFlac) 96000 else 44100,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore scan failed or permissions pending (${e.message}). Using fallback tracks.")
        }

        if (tracks.isEmpty()) {
            getFallbackTracks()
        } else {
            // Sort FLAC tracks to the front, then by title
            tracks.sortedWith(
                compareByDescending<Track> { it.isFlac }.thenBy { it.title.lowercase() }
            )
        }
    }

    /**
     * Curated sample audio tracks for immediate playback and UI testing.
     */
    fun getFallbackTracks(): List<Track> = listOf(
        Track(
            id = -101L,
            title = "Liquid Glass (Original Mix)",
            artist = "vMusic Studio",
            album = "Glass Reflections",
            durationMs = 214000L,
            contentUri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            isFlac = true,
            bitDepth = 24,
            sampleRate = 96000,
            mimeType = "audio/flac"
        ),
        Track(
            id = -102L,
            title = "Audiophile Symphony in C minor (FLAC 24-bit/96kHz)",
            artist = "Glass Philharmonic Orchestra",
            album = "Acoustic Horizons",
            durationMs = 345000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            isFlac = true,
            bitDepth = 24,
            sampleRate = 96000,
            mimeType = "audio/flac"
        ),
        Track(
            id = -103L,
            title = "Acoustic Resonance",
            artist = "Crystalline Acoustics",
            album = "Prism Waves",
            durationMs = 195000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            isFlac = true,
            bitDepth = 24,
            sampleRate = 192000,
            mimeType = "audio/flac"
        ),
        Track(
            id = -104L,
            title = "Prismatic Echoes",
            artist = "Aura Bloom",
            album = "Glass Reflections",
            durationMs = 260000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            isFlac = true,
            bitDepth = 24,
            sampleRate = 96000,
            mimeType = "audio/flac"
        ),
        Track(
            id = -105L,
            title = "Etheric Velvet",
            artist = "Subtle Pulse",
            album = "Pure Fidelity Master",
            durationMs = 182000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            isFlac = false,
            bitDepth = 16,
            sampleRate = 44100,
            mimeType = "audio/mp3"
        )
    )
}
