package app.aventurine.jetmapdemo.ui.modules.main.providers

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import app.aventurine.jetmapdemo.utils.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher
) : PathProvider {
    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): List<IntOffset> {
        val mapBitmap = withContext(Dispatchers.IO) {
            mapStitcher.stitch(floor = startingPoint.second)
        }

        if (mapBitmap == null) {
            return listOf()
        }

        return pathFinder.aStar(
            start = Node(
                offset = startingPoint.first,
                g = 0,
                h = 0,
                parent = null
            ),
            end = endingPoint.first,
            mapBitmap = mapBitmap
        )
    }
}