package app.aventurine.jetmapdemo.ui.modules.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmapdemo.data.services.sync.SyncService
import app.aventurine.jetmapdemo.data.services.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncService: SyncService,
    dataStoreManager: DataStoreManager
) : ViewModel() {
    val syncStateFlow: StateFlow<SyncState> = syncService.syncStateFlow
    val mapConfigFlow: Flow<MapConfigLocalEntity> = dataStoreManager.mapConfigFlow

    init {
        sync()
    }

    fun sync() = viewModelScope.launch {
        syncService.sync()
    }
}