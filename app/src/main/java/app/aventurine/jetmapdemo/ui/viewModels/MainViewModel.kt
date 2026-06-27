package app.aventurine.jetmapdemo.ui.viewModels

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.query
import app.aventurine.jetmap.models.Marker
import app.aventurine.jetmap.provider.AssetTileProvider
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmapdemo.MarkerExtractor
import app.aventurine.jetmapdemo.data.models.marker.MarkerRepository
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val assetManager: AssetManager,
    private val resources: Resources,
    private val markerRepository: MarkerRepository
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

    fun onQueryChange(
        query: String
    ) {
        _queryStateFlow.update { query }
    }

    val markerProvider = object : MarkerProvider {
        override suspend fun getMarker(
            x: Int,
            y: Int,
            z: Int
        ): Marker {
            val markerEntity = markerRepository.get(uid = "marker_${x}_${y}_$z")
            return Marker(
                x = markerEntity.x,
                y = markerEntity.y,
                z = markerEntity.floorId,
                bitmap = BitmapFactory.decodeResource(
                    resources,
                    markerEntity.iconDrawableRes,
                    BitmapFactory.Options().apply {
                        inScaled = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                ),
                description = markerEntity.description,
                iconId = markerEntity.iconDrawableRes
            )
        }

        override suspend fun getMarkers(visibleAreaRect: Rect, level: Int): List<Marker> {
            return markerRepository.getMarkersByCoordinates(
                coordinates = JetMapConfig.Coordinates(
                    startX = visibleAreaRect.left.toInt(),
                    startY = visibleAreaRect.top.toInt(),
                    endX = visibleAreaRect.right.toInt(),
                    endY = visibleAreaRect.bottom.toInt()
                ),
                floorId = level
            ).map { markerEntity ->
                Marker(
                    x = markerEntity.x,
                    y = markerEntity.y,
                    z = markerEntity.floorId,
                    bitmap = BitmapFactory.decodeResource(
                        resources,
                        markerEntity.iconDrawableRes,
                        BitmapFactory.Options().apply {
                            inScaled = false
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                    ),
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