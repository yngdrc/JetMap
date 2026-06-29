package app.aventurine.jetmap.controller.pathfinding

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.utils.MinimapStitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
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

    private val _pathStateFlow: MutableStateFlow<PathState?> = MutableStateFlow(value = null)
    val pathStateFlow: SharedFlow<PathState?> = _pathStateFlow.asSharedFlow()

    private var job: Job? = null

    fun findPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ) {
        job?.cancel()
        _pathStateFlow.update { null }

        job = scope.launch(Dispatchers.Default) {
            val mapBitmap = withContext(Dispatchers.IO) {
                mapStitcher.stitch(floor = 7)
            }

            if (mapBitmap == null) {
                return@launch
            }

            val pathData = pathFinder.aStar(
                start = Node(offset = startingPoint.first, g = 0, h = 0, parent = null),
                end = endingPoint.first,
                mapBitmap = mapBitmap
            )

            mapBitmap.recycle()
            _pathStateFlow.update {
                PathState(
                    startingPoint = startingPoint.first,
                    endingPoint = endingPoint.first,
                    pathData = pathData
                )
            }
        }
    }

    fun clear() {
        _pathStateFlow.update { null }
    }
}