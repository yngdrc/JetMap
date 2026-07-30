package app.aventurine.jetmapdemo.ui.modules.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aventurine.jetmap.domain.dataStore.DataStoreManager
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.domain.services.SyncService
import app.aventurine.jetmap.domain.services.SyncState
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
    val mapConfigFlow: Flow<MapConfigEntity?> = dataStoreManager.mapConfigFlow

    init {
        sync()
    }

    fun sync() = viewModelScope.launch {
        syncService.sync()
    }
}