package app.aventurine.jetmapdemo.data.services.sync.strategies

import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository

class LadderSyncStrategy(
    dataStoreManager: DataStoreManager,
    private val ladderRepository: LadderRepository,
    private val apiService: JetMapApiService
) : SyncStrategy(dataStoreManager = dataStoreManager) {
    override val id: String = this::class.java.simpleName

    override suspend fun sync(): Result<Unit> {
        return try {
            val response = apiService.getLadders()
            ladderRepository.persist(
                entities = response.ladders.map { ladderRemoteEntity ->
                    ladderRemoteEntity.toLocalEntity()
                }
            )

            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}