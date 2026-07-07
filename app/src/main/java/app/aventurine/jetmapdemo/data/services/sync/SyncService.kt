package app.aventurine.jetmapdemo.data.services.sync

import app.aventurine.jetmapdemo.data.services.sync.strategies.SyncStrategy
import kotlinx.coroutines.flow.StateFlow

abstract class SyncService {
    abstract val syncStateFlow: StateFlow<SyncState>
    protected abstract val syncStrategies: List<SyncStrategy>
    abstract suspend fun sync()
}