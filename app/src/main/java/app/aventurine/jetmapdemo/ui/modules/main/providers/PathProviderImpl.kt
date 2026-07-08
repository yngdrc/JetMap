package app.aventurine.jetmapdemo.ui.modules.main.providers

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import app.aventurine.jetmapdemo.utils.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher,
    private val ladderRepository: LadderRepository
) : PathProvider {
    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): List<IntOffset> {
        val (startOffset, startFloor) = startingPoint
        val (endOffset, endFloor) = endingPoint

        // TODO cch algorithm
        if (startFloor == endFloor) {
            return findPathForLevel(
                startingPoint = startOffset,
                endingPoint = endOffset,
                floor = startFloor
            )
        }

        val allLadders = withContext(Dispatchers.IO) {
            ladderRepository.getAllOffline().getOrDefault(defaultValue = listOf())
        }

        return listOf()
    }

    private suspend fun findPathForLevel(
        startingPoint: IntOffset,
        endingPoint: IntOffset,
        floor: Int
    ): List<IntOffset> {
        val mapBitmap = withContext(Dispatchers.IO) {
            mapStitcher.stitch(floor = floor)
        }

        if (mapBitmap == null) {
            return listOf()
        }

        val path = pathFinder.aStar(
            start = Node(
                offset = startingPoint,
                g = 0,
                h = 0,
                parent = null
            ),
            end = endingPoint,
            mapBitmap = mapBitmap
        )

        mapBitmap.recycle()
        return path
    }
}