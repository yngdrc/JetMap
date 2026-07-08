package app.aventurine.jetmap.data.services.sync.strategies

import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.services.SyncStrategy

class LadderSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val ladderRepository: LadderRepository
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            ladderRepository.getAllOnline(returnOnPersistError = true).fold(
                onSuccess = { _ ->
                    Result.success(value = Unit)
                },
                onFailure = { error ->
                    Result.failure(exception = error)
                }
            )
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}