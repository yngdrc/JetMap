package app.aventurine.jetmap.data.services.sync

import app.aventurine.jetmap.data.network.api.JetMapApiService
import app.aventurine.jetmap.data.services.sync.strategies.LadderSyncStrategy
import app.aventurine.jetmap.data.services.sync.strategies.MapConfigSyncStrategy
import app.aventurine.jetmap.data.services.sync.strategies.MarkerSyncStrategy
import app.aventurine.jetmap.data.services.sync.strategies.TileSyncStrategy
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.domain.services.SyncService
import app.aventurine.jetmap.domain.services.SyncState
import app.aventurine.jetmap.domain.services.SyncStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SyncServiceImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val apiService: JetMapApiService,
    private val fileStorage: FileStorage,
    private val markerRepository: MarkerRepository,
    private val ladderRepository: LadderRepository
) : SyncService() {
    private val _syncStateFlow: MutableStateFlow<SyncState> = MutableStateFlow(
        value = SyncState.Initial
    )

    override val syncStateFlow: StateFlow<SyncState> = _syncStateFlow.asStateFlow()

    override val syncStrategies: List<SyncStrategy> = listOf(
        MapConfigSyncStrategy(
            dataStoreManager = dataStoreManager,
            apiService = apiService
        ),
        TileSyncStrategy(
            dataStoreManager = dataStoreManager,
            apiService = apiService,
            fileStorage = fileStorage
        ),
        MarkerSyncStrategy(
            dataStoreManager = dataStoreManager,
            markerRepository = markerRepository
        ),
        LadderSyncStrategy(
            dataStoreManager = dataStoreManager,
            ladderRepository = ladderRepository
        )
    )

    override suspend fun sync() {
        _syncStateFlow.emit(value = SyncState.Initial)
        val version = try {
            apiService.getVersion()
        } catch (e: Exception) {
            val allSyncData = syncStrategies.mapNotNull { syncStrategy ->
                syncStrategy.getSyncData()
            }.filter { syncData ->
                syncData.isNotEmpty()
            }

            if (allSyncData.size == syncStrategies.size && allSyncData.distinct().size == 1) {
                return _syncStateFlow.emit(value = SyncState.Completed)
            }

            return _syncStateFlow.emit(value = SyncState.Failed(error = e))
        }

        _syncStateFlow.emit(value = SyncState.InProgress(progress = 0f))
        val strategies = syncStrategies.filter { strategy -> strategy.needsSync(version = version) }

        for (index in strategies.indices) {
            val strategy = strategies[index]
            val result = strategy.execute(version = version)

            if (result.isSuccess) {
                val progress = (index + 1f) / strategies.size
                _syncStateFlow.emit(value = SyncState.InProgress(progress = progress))
            } else {
                val error = result.exceptionOrNull() ?: RuntimeException("Unknown sync error")
                _syncStateFlow.emit(value =SyncState.Failed(error = error))
                return
            }
        }

        _syncStateFlow.emit(value =SyncState.Completed)
    }
}