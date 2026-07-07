package app.aventurine.jetmapdemo.ui.modules.main

import android.content.res.AssetManager
import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.provider.AssetTileProvider
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import app.aventurine.jetmapdemo.data.repositories.ladder.LadderRepository
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import app.aventurine.jetmapdemo.utils.MarkerExtractor
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
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val assetManager: AssetManager,
    private val resources: Resources,
    private val markerRepository: MarkerRepository,
    private val ladderRepository: LadderRepository
) : ViewModel() {
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

    val markerProvider = object : MarkerProvider {
        override suspend fun getMarkerInputStream(
            markerDescriptor: MarkerDescriptor
        ): InputStream {
            return resources.openRawResource(markerDescriptor.iconId)
        }

        override suspend fun getMarker(x: Int, y: Int, z: Int): MarkerDescriptor {
            val markerEntity = markerRepository.get(x = x, y = y, z = z)
            return MarkerDescriptor(
                x = markerEntity?.x ?: x,
                y = markerEntity?.y ?: y,
                z = markerEntity?.floorId ?: z,
                iconId = markerEntity?.iconDrawableRes ?: R.drawable.ic_map_player,
                description = markerEntity?.description ?: "$x, $y"
            )
        }

        override suspend fun getMarkers(visibleAreaRect: Rect, level: Int): List<MarkerDescriptor> {
            return markerRepository.getMarkersByCoordinates(
                coordinates = JetMapConfig.Coordinates(
                    startX = visibleAreaRect.left.toInt(),
                    startY = visibleAreaRect.top.toInt(),
                    endX = visibleAreaRect.right.toInt(),
                    endY = visibleAreaRect.bottom.toInt()
                ),
                floorId = level
            ).map { markerEntity ->
                MarkerDescriptor(
                    x = markerEntity.x,
                    y = markerEntity.y,
                    z = markerEntity.floorId,
                    description = markerEntity.description,
                    iconId = markerEntity.iconDrawableRes
                )
            }
        }
    }

    val jetMapController = JetMapController(
        config = JetMapConfig(
            tileSize = 256,
            leftMostTileCoordinate = 31744,
            rightMostTileCoordinate = 34048,
            topMostTileCoordinate = 30976,
            bottomMostTileCoordinate = 32768,
        ),
        tileProvider = AssetTileProvider(tileSize = 256, assetManager = assetManager),
        markerProvider = markerProvider,
        assetManager = assetManager
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            MarkerExtractor(
                assetManager = assetManager,
                resources = resources,
                markerRepository = markerRepository,
                jetMapConfig = jetMapController.config
            ).extractMarkers()
        }
    }
}