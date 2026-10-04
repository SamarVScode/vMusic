package com.glass.player.domain.update

import android.net.Uri

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()

    data class Available(
        val versionName: String,
        val changelog: String,
        val downloadUrl: String,
        val fileSizeMb: Double
    ) : UpdateState()

    data class Downloading(
        val progress: Float,
        val downloadedMb: Double,
        val totalMb: Double
    ) : UpdateState()

    data class ReadyToInstall(
        val fileUri: Uri
    ) : UpdateState()

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UpdateState()
}
