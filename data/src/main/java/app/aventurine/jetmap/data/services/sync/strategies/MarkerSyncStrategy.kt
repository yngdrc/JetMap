package app.aventurine.jetmap.data.services.sync.strategies

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.domain.services.SyncStrategy

class MarkerSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val markerRepository: MarkerRepository,
    private val apiService: JetMapApiService
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            val response = apiService.getMarkers()
            markerRepository.persist(
                entities = response.markers.map { markerRemoteEntity ->
                    markerRemoteEntity.toEntity()
                }
            )

            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}