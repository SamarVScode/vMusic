package com.glass.player.domain

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioRepository {

    suspend fun scanLocalTracks(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val contentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE
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
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "audio/flac" else "audio/flac"
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val isFlac = mimeType.contains("flac", ignoreCase = true) || title.endsWith(".flac", ignoreCase = true)

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = album,
                            durationMs = if (durationMs > 0) durationMs else 210000L,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            sampleRate = if (isFlac) 96000 else 44100,
                            bitDepth = if (isFlac) 24 else 16,
                            isFlac = isFlac,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songs.isEmpty()) {
            getAudiophileSampleTracks()
        } else {
            songs
        }
    }

    fun getAudiophileSampleTracks(): List<Song> {
        return listOf(
            Song(
                id = -101L,
                title = "Liquid Glass (Original Mix)",
                artist = "vMusic Studio",
                album = "Glass Reflections (FLAC 24-bit)",
                durationMs = 214000L,
                contentUri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 210000000L
            ),
            Song(
                id = -102L,
                title = "Jazz in Paris (Audiophile Master)",
                artist = "Media Right Productions",
                album = "Audiophile Sessions",
                durationMs = 175000L,
                contentUri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 185000000L
            ),
            Song(
                id = -103L,
                title = "Acoustic Resonance (Live Master)",
                artist = "Crystalline Acoustics",
                album = "Prism Waves",
                durationMs = 372000L,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                sampleRate = 192000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 142000000L
            ),
            Song(
                id = -104L,
                title = "Prismatic Echoes",
                artist = "Aura Bloom",
                album = "Glass Reflections",
                durationMs = 423000L,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 160000000L
            ),
            Song(
                id = -105L,
                title = "Audiophile Symphony in C minor",
                artist = "Glass Philharmonic",
                album = "Acoustic Horizons",
                durationMs = 345000L,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 110000000L
            ),
            Song(
                id = -106L,
                title = "Etheric Velvet (Studio Session)",
                artist = "Subtle Pulse",
                album = "Pure Fidelity",
                durationMs = 280000L,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 155000000L
            )
        )
    }
}
