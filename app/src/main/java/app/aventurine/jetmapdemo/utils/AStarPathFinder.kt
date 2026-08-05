package app.aventurine.jetmapdemo.utils

import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

/**
 * Grid A* over a [CostMap].
 *
 * Fixes over the previous version:
 *  - works on a primitive grid instead of `Bitmap.get` per neighbour,
 *  - bounds are checked explicitly, not through exceptions,
 *  - terrain friction and the heuristic use the same scale, so the heuristic actually guides the
 *    search instead of degenerating into Dijkstra,
 *  - diagonal moves may not cut corners,
 *  - the result is "string pulled" (line of sight simplification), which is what makes the drawn
 *    route look like a map route instead of grid stairs.
 */
class AStarPathFinder {

    suspend fun findPath(
        start: IntOffset,
        end: IntOffset,
        costMap: CostMap,
        maxIterations: Int = DEFAULT_MAX_ITERATIONS
    ): List<IntOffset> = withContext(Dispatchers.Default) {
        if (!costMap.isWalkable(start.x, start.y) || !costMap.isWalkable(end.x, end.y)) {
            return@withContext emptyList()
        }

        val width = costMap.width
        val startIndex = start.y * width + start.x
        val endIndex = end.y * width + end.x

        if (startIndex == endIndex) {
            return@withContext listOf(start)
        }

        val gScore = HashMap<Int, Int>()
        val parents = HashMap<Int, Int>()
        val closed = HashSet<Int>()

        // f in the high 32 bits, cell index in the low ones: ordering for free, no allocation
        // of a comparator object per node.
        val open = PriorityQueue<Long>()

        gScore[startIndex] = 0
        open.add(encode(f = heuristic(start.x, start.y, end.x, end.y), index = startIndex))

        var iterations = 0

        while (open.isNotEmpty()) {
            if (iterations++ % YIELD_INTERVAL == 0) {
                coroutineContext.ensureActive()
            }

            if (iterations > maxIterations) {
                return@withContext emptyList()
            }

            val currentIndex = decodeIndex(open.poll()!!)
            if (currentIndex == endIndex) {
                return@withContext simplify(
                    path = reconstruct(parents = parents, endIndex = endIndex, width = width),
                    costMap = costMap
                )
            }

            if (!closed.add(currentIndex)) {
                continue
            }

            val currentX = currentIndex % width
            val currentY = currentIndex / width
            val currentG = gScore[currentIndex] ?: continue

            for (direction in 0 until NEIGHBOUR_COUNT) {
                val neighbourX = currentX + NEIGHBOUR_DX[direction]
                val neighbourY = currentY + NEIGHBOUR_DY[direction]

                if (!costMap.isWalkable(neighbourX, neighbourY)) {
                    continue
                }

                val isDiagonal = direction >= 4
                if (isDiagonal && !canCutCorner(costMap, currentX, currentY, neighbourX, neighbourY)) {
                    continue
                }

                val neighbourIndex = neighbourY * width + neighbourX
                if (neighbourIndex in closed) {
                    continue
                }

                val stepCost = (if (isDiagonal) DIAGONAL_COST else STRAIGHT_COST) +
                        costMap.friction(neighbourX, neighbourY)

                val tentativeG = currentG + stepCost
                if (tentativeG >= (gScore[neighbourIndex] ?: Int.MAX_VALUE)) {
                    continue
                }

                gScore[neighbourIndex] = tentativeG
                parents[neighbourIndex] = currentIndex
                open.add(
                    encode(
                        f = tentativeG + heuristic(neighbourX, neighbourY, end.x, end.y),
                        index = neighbourIndex
                    )
                )
            }
        }

        emptyList()
    }

    /** Diagonal moves are only allowed when both adjacent orthogonal cells are free. */
    private fun canCutCorner(
        costMap: CostMap,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean = costMap.isWalkable(toX, fromY) && costMap.isWalkable(fromX, toY)

    private fun heuristic(x: Int, y: Int, endX: Int, endY: Int): Int {
        val dx = abs(x - endX)
        val dy = abs(y - endY)
        // Octile distance, admissible because the step cost is never below STRAIGHT_COST.
        return STRAIGHT_COST * (dx + dy) + (DIAGONAL_COST - 2 * STRAIGHT_COST) * min(dx, dy)
    }

    private fun reconstruct(
        parents: Map<Int, Int>,
        endIndex: Int,
        width: Int
    ): List<IntOffset> {
        val path = ArrayList<IntOffset>()
        var index: Int? = endIndex

        while (index != null) {
            path.add(IntOffset(x = index % width, y = index / width))
            index = parents[index]
        }

        path.reverse()
        return path
    }

    /**
     * Removes every point that can be skipped without leaving the walkable area. Turns the raw
     * grid staircase into a handful of straight segments, but shortcuts are bounded so the line
     * keeps following the terrain instead of flying across half of the floor.
     */
    private fun simplify(path: List<IntOffset>, costMap: CostMap): List<IntOffset> {
        if (path.size < 3) {
            return path
        }

        val result = ArrayList<IntOffset>()
        result.add(path.first())

        var anchor = 0
        var probe = 1

        while (probe < path.lastIndex) {
            val tooLong = probe + 1 - anchor > MAX_SHORTCUT_STEPS
            if (tooLong || !hasLineOfSight(path[anchor], path[probe + 1], costMap)) {
                result.add(path[probe])
                anchor = probe
            }
            probe++
        }

        result.add(path.last())
        return result
    }

    /**
     * Conservative (supercover) Bresenham: a diagonal step is only allowed when both orthogonal
     * cells around the corner are walkable, and both of them are tested as well.
     *
     * The thin classic Bresenham line slipped diagonally between two blocked cells, which is why
     * the simplified route could cross non walkable tiles.
     */
    private fun hasLineOfSight(from: IntOffset, to: IntOffset, costMap: CostMap): Boolean {
        var x = from.x
        var y = from.y
        val dx = abs(to.x - x)
        val dy = abs(to.y - y)
        val stepX = if (to.x > x) 1 else -1
        val stepY = if (to.y > y) 1 else -1
        var error = dx - dy

        while (true) {
            if (!costMap.isWalkable(x, y)) {
                return false
            }

            if (x == to.x && y == to.y) {
                return true
            }

            val doubledError = error shl 1
            val moveX = doubledError > -dy
            val moveY = doubledError < dx

            if (moveX && moveY) {
                // Diagonal step: both shoulder cells must be free, otherwise the line would cut
                // through the corner of an obstacle.
                if (!costMap.isWalkable(x + stepX, y) || !costMap.isWalkable(x, y + stepY)) {
                    return false
                }
            }

            if (moveX) {
                error -= dy
                x += stepX
            }
            if (moveY) {
                error += dx
                y += stepY
            }
        }
    }

    private fun encode(f: Int, index: Int): Long =
        (f.toLong() shl 32) or (index.toLong() and 0xFFFFFFFFL)

    private fun decodeIndex(value: Long): Int = (value and 0xFFFFFFFFL).toInt()

    private companion object {
        const val STRAIGHT_COST = 10
        const val DIAGONAL_COST = 14
        const val NEIGHBOUR_COUNT = 8
        const val YIELD_INTERVAL = 512
        const val DEFAULT_MAX_ITERATIONS = 2_000_000

        /** Upper bound of a line-of-sight shortcut, in grid steps. */
        const val MAX_SHORTCUT_STEPS = 48

        val NEIGHBOUR_DX = intArrayOf(0, 0, -1, 1, -1, 1, -1, 1)
        val NEIGHBOUR_DY = intArrayOf(-1, 1, 0, 0, -1, -1, 1, 1)
    }
}