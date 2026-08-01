package com.vinnovateit.latch.core.updater

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState

    /**
     * Checked, and this build is current. Distinct from [Idle] ("never checked"),
     * which is what lets Settings show "You are up to date" instead of appearing
     * to have done nothing.
     */
    data object UpToDate : UpdateState
    data class UpdateAvailable(
        val version: String,
        val downloadUrl: String,
        val releaseNotes: String,
    ) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Downloaded(val filePath: String) : UpdateState
    data class Error(val message: String) : UpdateState
}
