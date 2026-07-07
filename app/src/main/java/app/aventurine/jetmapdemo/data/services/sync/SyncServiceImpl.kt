package app.aventurine.jetmapdemo.data.services.sync

import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.services.sync.strategies.LadderSyncStrategy
import app.aventurine.jetmapdemo.data.services.sync.strategies.MapConfigSyncStrategy
import app.aventurine.jetmapdemo.data.services.sync.strategies.MarkerSyncStrategy
import app.aventurine.jetmapdemo.data.services.sync.strategies.SyncStrategy
import app.aventurine.jetmapdemo.data.services.sync.strategies.TileSyncStrategy
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
            markerRepository = markerRepository,
            apiService = apiService
        ),
        LadderSyncStrategy(
            dataStoreManager = dataStoreManager,
            ladderRepository = ladderRepository,
            apiService = apiService
        )
    )

    override suspend fun sync() {
        _syncStateFlow.emit(value = SyncState.Initial)
        val version = apiService.getVersion()

        _syncStateFlow.emit(SyncState.InProgress(progress = 0f))
        val strategies = syncStrategies.filter { strategy -> strategy.needsSync(version = version) }

        for (index in strategies.indices) {
            val strategy = strategies[index]
            val result = strategy.execute(version = version)

            if (result.isSuccess) {
                val progress = (index + 1f) / strategies.size
                _syncStateFlow.emit(SyncState.InProgress(progress = progress))
            } else {
                val error = result.exceptionOrNull() ?: RuntimeException("Unknown sync error")
                _syncStateFlow.emit(SyncState.Failed(error = error))
                return
            }
        }

        _syncStateFlow.emit(SyncState.Completed)
    }
}