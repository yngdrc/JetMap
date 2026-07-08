package app.aventurine.jetmapdemo.ui.modules.main.providers

import androidx.compose.ui.unit.IntOffset
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import app.aventurine.jetmapdemo.utils.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PathProviderImpl(
    private val pathFinder: AStarPathFinder,
    private val mapStitcher: MinimapStitcher,
    private val ladderRepository: LadderRepository
) : PathProvider {
    override suspend fun getPath(
        startingPoint: Pair<IntOffset, Int>,
        endingPoint: Pair<IntOffset, Int>
    ): List<IntOffset> {
        val ladders = withContext(Dispatchers.IO) {
            ladderRepository.getAllOffline().getOrDefault(
                defaultValue = listOf()
            ).filter { ladder ->
                ladder.floor == 7
            }
        }

        val mapBitmap = withContext(Dispatchers.IO) {
            mapStitcher.stitch(floor = startingPoint.second)
        }

        if (mapBitmap == null) {
            return listOf()
        }

        val basePath = pathFinder.aStar(
            start = Node(
                offset = startingPoint.first,
                g = 0,
                h = 0,
                parent = null
            ),
            end = endingPoint.first,
            mapBitmap = mapBitmap
        )

        if (basePath.isNotEmpty()) {
            return basePath
        }

        val pathFromLadders = coroutineScope {
            ladders.map { ladder ->
                async {
                    pathFinder.aStar(
                        start = Node(
                            offset = IntOffset(x = ladder.x, y = ladder.y),
                            g = 0,
                            h = 0,
                            parent = null
                        ),
                        end = endingPoint.first,
                        mapBitmap = mapBitmap
                    )
                }
            }.awaitAll()
        }.filter { path ->
            path.isNotEmpty()
        }

        mapBitmap.recycle()
        return pathFromLadders.minBy { it.size }
    }
}