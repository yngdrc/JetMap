package app.aventurine.jetmapdemo.ui.modules.main

import android.content.Context
import android.content.res.Resources
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmapdemo.ui.modules.main.providers.MarkerProviderImpl
import app.aventurine.jetmapdemo.ui.modules.main.providers.TileProviderImpl
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.gestureApi
import app.aventurine.jetmap.utils.pathApi
import app.aventurine.jetmapdemo.ui.modules.main.providers.PathProviderImpl
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val resources: Resources,
    private val markerRepository: MarkerRepository,
    private val ladderRepository: LadderRepository,
    private val fileStorage: FileStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val mapConfig: MapConfigEntity =
        savedStateHandle.get<MapConfigEntity>(key = "mapConfig")
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

    val markerProvider: MarkerProvider = MarkerProviderImpl(
        markerRepository = markerRepository,
        ladderRepository = ladderRepository,
        resources = resources
    )

    val tileProvider = TileProviderImpl(
        fileStorage = fileStorage,
        mapConfig = mapConfig
    )

    val pathProvider = PathProviderImpl(
        pathFinder = AStarPathFinder(),
        mapStitcher = MinimapStitcher(
            context = context,
            mapConfig = mapConfig
        ),
        ladderRepository = ladderRepository
    )

    val jetMapController = JetMapController(
        config = JetMapConfig(
            tileSize = mapConfig.tileSize,
            mapSize = IntSize(
                width = mapConfig.width,
                height = mapConfig.height
            ),
        ),
        tileProvider = tileProvider,
        markerProvider = markerProvider,
        pathProvider = pathProvider
    )

    init {
        viewModelScope.launch {
            jetMapController.gestureApi.focusedMarkerFlow
                .collectLatest { markerDescriptor ->
                    if (markerDescriptor == null) {
                        return@collectLatest jetMapController.pathApi.clear()
                    }

                    jetMapController.pathApi.findPath(
                        startingPoint = IntOffset(
                            x = markerDescriptor.x,
                            y = markerDescriptor.y
                        ) to markerDescriptor.z,
                        endingPoint = IntOffset(x = 1587, y = 940) to 8
                    )
                }
        }
    }
}