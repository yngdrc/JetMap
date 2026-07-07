package app.aventurine.jetmap.controller.pathfinding

import android.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.get
import app.aventurine.jetmap.ui.JetMapConfig
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
                mapStitcher.stitch(floor = startingPoint.second)
            }

            if (mapBitmap == null) {
                return@launch
            }

//            val ladders = (0..<mapBitmap.width).flatMap { x ->
//                (0..<mapBitmap.height).mapNotNull { y ->
//                    val pixel = mapBitmap[x, y]
//                    val r = Color.red(pixel)
//                    val g = Color.green(pixel)
//                    val b = Color.blue(pixel)
//                    val color = Color.rgb(r, g, b)
//
//                    if (color != BlockType.LADDER.color) {
//                        return@mapNotNull null
//                    }
//
//                    IntOffset(x = x, y = y)
//                }
//            }

            val pathData = pathFinder.aStar(
                start = Node(
                    offset = startingPoint.first,
                    g = 0,
                    h = 0,
                    parent = null
                ),
                end = endingPoint.first,
                mapBitmap = mapBitmap
            )

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