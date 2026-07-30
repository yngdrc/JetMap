package app.aventurine.jetmapdemo.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import kotlin.math.absoluteValue
import kotlin.math.min
import androidx.core.graphics.get
import androidx.core.graphics.luminance
import app.aventurine.jetmap.controller.path.BlockType

data class Node(
    val offset: IntOffset,
    val g: Int,
    val h: Int,
    val parent: Node?
) {
    val f: Int
        get() = g + h
}

class AStarPathFinder {
    suspend fun aStar(
        start: Node,
        end: IntOffset,
        mapBitmap: Bitmap
    ): List<IntOffset> = withContext(Dispatchers.Default) {
        val open = PriorityQueue<Node>(compareBy { it.f })
        val openBestG = HashMap<IntOffset, Int>()
        val closed = HashSet<IntOffset>()

        open.add(start)
        openBestG[start.offset] = start.g

        while (open.isNotEmpty() && isActive) {
            val current = open.poll()!!

            if (current.offset == end) {
                return@withContext generateSequence(current) { it.parent }
                    .map { it.offset }
                    .toList()
                    .reversed()
            }

            if (current.g > (openBestG[current.offset] ?: Int.MAX_VALUE)) continue

            closed.add(current.offset)

            for (neighbor in getNeighbors(current, end, mapBitmap)) {
                if (neighbor.offset in closed) continue
                val bestG = openBestG[neighbor.offset] ?: Int.MAX_VALUE
                if (neighbor.g < bestG) {
                    openBestG[neighbor.offset] = neighbor.g
                    open.add(neighbor)
                }
            }
        }

        emptyList()
    }

    private fun getNeighbors(node: Node, end: IntOffset, mapBitmap: Bitmap): List<Node> {
        val x = node.offset.x
        val y = node.offset.y

        val straight = listOf(
            IntOffset(x, y - 1),
            IntOffset(x, y + 1),
            IntOffset(x - 1, y),
            IntOffset(x + 1, y),
        ).mapNotNull { offset ->
            offset.toNode(mapBitmap = mapBitmap, node = node, end = end, isDiagonal = false)
        }

        val diagonal = listOf(
            IntOffset(x - 1, y - 1),
            IntOffset(x + 1, y - 1),
            IntOffset(x + 1, y + 1),
            IntOffset(x - 1, y + 1),
        ).mapNotNull { offset ->
            offset.toNode(mapBitmap = mapBitmap, node = node, end = end, isDiagonal = true)
        }

        return straight + diagonal
    }

    private fun heuristic(a: IntOffset, b: IntOffset): Int {
        val dx = (a.x - b.x).absoluteValue
        val dy = (a.y - b.y).absoluteValue
        return 10 * (dx + dy) - 6 * min(dx, dy)
    }

    private fun isWalkable(color: Int): Boolean {
//        val blockType = BlockType.fromColor(color)
//        return blockType.isWalkable

        return Color.rgb(255, 255, 0) != color
    }

    private fun IntOffset.toNode(
        mapBitmap: Bitmap,
        node: Node,
        end: IntOffset,
        isDiagonal: Boolean
    ): Node? {
        val pixel = try {
            mapBitmap[x, y]
        } catch (e: Exception) {
            return null
        }

        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val color = Color.rgb(r, g, b)
        if (!isWalkable(color = color)) {
            return null
        }

        val baseG = node.g + if (isDiagonal) 14 else 10

        // grayscale r = g = b, consider using single channel
        val frictionG = (ColorUtils.calculateLuminance(color) * 100).toInt()

        return Node(
            offset = this,
            g = baseG + frictionG,
            h = heuristic(this, end),
            parent = node
        )
    }
}