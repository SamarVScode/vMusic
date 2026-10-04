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
                id = 101L,
                title = "So What",
                artist = "Miles Davis",
                album = "Kind of Blue (Master Edition)",
                durationMs = 562000L,
                contentUri = "",
                sampleRate = 192000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 210000000L
            ),
            Song(
                id = 102L,
                title = "Giorgio by Moroder",
                artist = "Daft Punk",
                album = "Random Access Memories",
                durationMs = 544000L,
                contentUri = "",
                sampleRate = 88200,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 185000000L
            ),
            Song(
                id = 103L,
                title = "Comfortably Numb",
                artist = "Pink Floyd",
                album = "The Wall (Audiophile Remaster)",
                durationMs = 382000L,
                contentUri = "",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 142000000L
            ),
            Song(
                id = 104L,
                title = "Aja",
                artist = "Steely Dan",
                album = "Aja",
                durationMs = 477000L,
                contentUri = "",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 160000000L
            ),
            Song(
                id = 105L,
                title = "Nocturne in E-flat Major, Op. 9 No. 2",
                artist = "Frédéric Chopin",
                album = "The Complete Nocturnes",
                durationMs = 272000L,
                contentUri = "",
                sampleRate = 192000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 110000000L
            ),
            Song(
                id = 106L,
                title = "Hotel California (Live Acoustic)",
                artist = "Eagles",
                album = "Hell Freezes Over",
                durationMs = 432000L,
                contentUri = "",
                sampleRate = 96000,
                bitDepth = 24,
                isFlac = true,
                sizeBytes = 155000000L
            )
        )
    }
}
