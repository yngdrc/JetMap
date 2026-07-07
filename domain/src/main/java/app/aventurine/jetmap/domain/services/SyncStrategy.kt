package app.aventurine.jetmap.domain.services

import androidx.datastore.preferences.core.stringPreferencesKey
import app.aventurine.jetmap.domain.dataStore.DataStoreManager

abstract class SyncStrategy(
    protected val dataStoreManager: DataStoreManager
) {
    abstract val id: String
    protected abstract suspend fun sync(): Result<Unit>

    suspend fun execute(version: String): Result<Unit> {
        if (!needsSync(version = version)) {
            return Result.success(value = Unit)
        }

        return try {
            val result = sync()
            if (!result.isFailure) {
                setSyncData(version = version)
            }

            return result
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }

    suspend fun setSyncData(version: String) {
        dataStoreManager.update(
            key = stringPreferencesKey(name = id),
            value = version
        )
    }

    suspend fun needsSync(version: String): Boolean {
        val lastSyncVersion = dataStoreManager.getOrNull(
            key = stringPreferencesKey(name = id)
        ) ?: return true

        return lastSyncVersion != version
    }
}