package app.aventurine.jetmap.controller.path

import androidx.compose.ui.unit.IntOffset

interface PathApi {
    fun findPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    )

    fun clear()
}