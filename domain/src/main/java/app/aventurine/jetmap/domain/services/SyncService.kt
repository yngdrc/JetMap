package app.aventurine.jetmap.domain.services

import kotlinx.coroutines.flow.StateFlow

abstract class SyncService {
    abstract val syncStateFlow: StateFlow<SyncState>
    protected abstract val syncStrategies: List<SyncStrategy>
    abstract suspend fun sync()
}