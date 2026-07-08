package app.aventurine.jetmap.controller.path

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.provider.PathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PathController(
    parentScope: CoroutineScope,
    private val pathProvider: PathProvider?
) : PathApi {
    private val scope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob()
    )

    private val _pathStateFlow: MutableStateFlow<PathState?> = MutableStateFlow(value = null)
    val pathStateFlow: SharedFlow<PathState?> = _pathStateFlow.asSharedFlow()

    private var job: Job? = null

    override fun findPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ) {
        job?.cancel()
        _pathStateFlow.update { null }

        if (pathProvider == null) {
            return
        }

        job = scope.launch(Dispatchers.Default) {
            val pathData = pathProvider.getPath(
                startingPoint = startingPoint,
                endingPoint = endingPoint
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

    override fun clear() {
        _pathStateFlow.update { null }
    }
}