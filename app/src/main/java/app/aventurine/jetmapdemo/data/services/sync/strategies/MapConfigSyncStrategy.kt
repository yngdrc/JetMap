package app.aventurine.jetmapdemo.data.services.sync.strategies

import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import kotlinx.coroutines.Dispatchers

class MapConfigSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val apiService: JetMapApiService
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            val mapConfig = apiService.getMapConfig()
            dataStoreManager.update(mapConfig = mapConfig.toLocalEntity())
            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}