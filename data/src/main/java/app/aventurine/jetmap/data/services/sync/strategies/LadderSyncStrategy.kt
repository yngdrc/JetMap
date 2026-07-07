package app.aventurine.jetmap.data.services.sync.strategies

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.services.SyncStrategy

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
                    ladderRemoteEntity.toEntity()
                }
            )

            Result.success(value = Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }
}