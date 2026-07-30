package app.aventurine.jetmap.controller.path

import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.SharedFlow

interface PathApi {
    val pathStateFlow: SharedFlow<PathState?>
    fun findPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    )

    fun clear()
}