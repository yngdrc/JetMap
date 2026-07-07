package app.aventurine.jetmapdemo.data.services.sync.strategies

import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository

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
                    markerRemoteEntity.toLocalEntity()
                }
            )

            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}