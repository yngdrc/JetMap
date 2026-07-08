package app.aventurine.jetmap.data.services.sync.strategies

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.domain.services.SyncStrategy

class MarkerSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val markerRepository: MarkerRepository
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            markerRepository.getAllOnline(returnOnPersistError = true).fold(
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