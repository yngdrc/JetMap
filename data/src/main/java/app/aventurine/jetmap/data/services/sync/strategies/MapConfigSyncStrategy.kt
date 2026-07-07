package app.aventurine.jetmap.data.services.sync.strategies

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.services.SyncStrategy

class MapConfigSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val apiService: JetMapApiService
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            val mapConfig = apiService.getMapConfig()
            dataStoreManager.update(mapConfig = mapConfig.toEntity())
            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}