package app.aventurine.jetmapdemo.ui.modules.main

import android.content.res.AssetManager
import android.content.res.Resources
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import app.aventurine.jetmapdemo.data.providers.TileProvider
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val assetManager: AssetManager,
    private val resources: Resources,
    private val markerRepository: MarkerRepository,
    private val fileStorage: FileStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mapConfig: MapConfigLocalEntity.Default =
        savedStateHandle.get<MapConfigLocalEntity.Default>(key = "mapConfig")
            ?: throw IllegalArgumentException("MapConfig must be provided in SavedStateHandle")

    private val _queryStateFlow: MutableStateFlow<String> = MutableStateFlow("")
    val queryStateFlow: StateFlow<String> = _queryStateFlow.asStateFlow()

    val searchResultsFlow: StateFlow<List<MarkerEntity>> = queryStateFlow
        .debounce(300.milliseconds)
        .map { query ->
            markerRepository.search(query = query)
        }.stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun onQueryChange(query: String) = _queryStateFlow.update { query }

    val markerProvider: app.aventurine.jetmapdemo.data.providers.MarkerProvider =
        app.aventurine.jetmapdemo.data.providers.MarkerProvider(
            markerRepository = markerRepository,
            resources = resources
        )

    val jetMapController = JetMapController(
        config = JetMapConfig(
            tileSize = mapConfig.tileSize,
            mapSize = IntSize(
                width = mapConfig.width,
                height = mapConfig.height
            ),
        ),
        tileProvider = TileProvider(fileStorage = fileStorage, mapConfig = mapConfig),
        markerProvider = markerProvider,
        assetManager = assetManager
    )
}