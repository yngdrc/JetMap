package app.aventurine.jetmapdemo.ui.modules.sync

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import app.aventurine.jetmapdemo.data.dataStore.DataStoreManager
import app.aventurine.jetmapdemo.data.dataStore.PreferencesKey
import app.aventurine.jetmapdemo.data.network.JetMapApiService
import app.aventurine.jetmapdemo.data.services.sync.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    val syncService: SyncService
) : ViewModel() {
}