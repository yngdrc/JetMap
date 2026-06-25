package app.aventurine.jetmap.controller.pathfinding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmap.utils.MinimapStitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PathfindingController(
    parentScope: CoroutineScope,
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher
) {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _pathState: MutableStateFlow<PathState?> = MutableStateFlow(null)
    val pathState: SharedFlow<PathState?> = _pathState.asSharedFlow()

    private var job: Job? = null

    fun findPath(
        startingPoint: IntOffset,
        endingPoint: IntOffset
    ) {
        job?.cancel()
        _pathState.update { null }

        job = scope.launch(Dispatchers.Default) {
            val mapBitmap = withContext(Dispatchers.IO) {
                mapStitcher.stitch(floor = 7)
            }

            if (mapBitmap == null) {
                return@launch
            }

            val pathData = pathFinder.aStar(
                start = Node(offset = startingPoint, g = 0, h = 0, parent = null),
                end = endingPoint,
                mapBitmap = mapBitmap
            )


            mapBitmap.recycle()
            _pathState.update {
                PathState(
                    startingPoint = startingPoint,
                    endingPoint = endingPoint,
                    pathData = pathData
                )
            }
        }
    }

    fun clear() {
        _pathState.update { null }
    }
}