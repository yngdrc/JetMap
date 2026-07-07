package app.aventurine.jetmapdemo.data.services.sync

sealed class SyncState(
    val text: String,
    val progress: Float?
) {
    data object Initial : SyncState(
        text = "Starting sync",
        progress = 0f
    )

    class InProgress(
        progress: Float
    ) : SyncState(
        text = "Sync in progress",
        progress = progress
    )

    data object Completed : SyncState(
        text = "Sync completed",
        progress = 100f
    )

    data class Failed(
        val error: Throwable
    ) : SyncState(
        text = error.message ?: "Unexpected error",
        progress = null
    )
}