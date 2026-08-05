package app.aventurine.jetmapdemo.utils

import android.graphics.Color
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Flat, primitive cost grid of a single floor.
 *
 * Replaces the previous `Bitmap` based approach:
 *  - one byte per cell instead of 4 (an 8192x8192 floor is 64 MB instead of 256 MB),
 *  - no JNI call per neighbour (`Bitmap.get` was called hundreds of thousands of times),
 *  - no `try/catch` used as a bounds check,
 *  - terrain friction is pre-computed through a 256 entry LUT instead of calling
 *    `ColorUtils.calculateLuminance` (which does three `pow()` calls) for every neighbour.
 */
class CostMap(
    val width: Int,
    val height: Int
) {
    private val cells = ByteArray(width * height) { BLOCKED }

    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    private fun raw(x: Int, y: Int): Int = cells[y * width + x].toInt() and 0xFF

    fun isWalkable(x: Int, y: Int): Boolean = inBounds(x, y) && raw(x, y) != BLOCKED_INT

    fun isLadder(x: Int, y: Int): Boolean = inBounds(x, y) && raw(x, y) == LADDER_INT

    /** Extra movement cost of the terrain, in the same scale as the base step cost. */
    fun friction(x: Int, y: Int): Int = when (val value = raw(x, y)) {
        BLOCKED_INT -> 0
        LADDER_INT -> 0
        else -> value
    }

    /**
     * Nearest walkable cell around [x], [y] within [maxRadius], or null when there is none.
     *
     * Lets the user drop a route point anywhere: tapping a wall, a tree or water snaps to the
     * closest reachable cell instead of failing with "route not found".
     */
    fun nearestWalkable(x: Int, y: Int, maxRadius: Int = DEFAULT_SNAP_RADIUS): Pair<Int, Int>? {
        if (isWalkable(x, y)) {
            return x to y
        }

        for (radius in 1..maxRadius) {
            for (offset in -radius..radius) {
                val candidates = arrayOf(
                    (x + offset) to (y - radius),
                    (x + offset) to (y + radius),
                    (x - radius) to (y + offset),
                    (x + radius) to (y + offset)
                )

                candidates.forEach { (candidateX, candidateY) ->
                    if (isWalkable(candidateX, candidateY)) {
                        return candidateX to candidateY
                    }
                }
            }
        }

        return null
    }

    /**
     * Writes one decoded tile into the grid. The tile bitmap can be recycled right after.
     */
    fun writeTile(
        pixels: IntArray,
        tileWidth: Int,
        tileHeight: Int,
        originX: Int,
        originY: Int
    ) {
        for (row in 0 until tileHeight) {
            val targetY = originY + row
            if (targetY !in 0..<height) continue

            val rowOffset = targetY * width
            val pixelRowOffset = row * tileWidth

            for (column in 0 until tileWidth) {
                val targetX = originX + column
                if (targetX !in 0..<width) continue

                cells[rowOffset + targetX] = classify(pixels[pixelRowOffset + column])
            }
        }
    }

    private fun classify(pixel: Int): Byte {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF

        if (BLOCKED_COLORS.contains(Color.rgb(red, green, blue))) {
            return BLOCKED
        }

        val friction = (
                LUMINANCE_LUT[red] * RED_WEIGHT +
                        LUMINANCE_LUT[green] * GREEN_WEIGHT +
                        LUMINANCE_LUT[blue] * BLUE_WEIGHT
                ) * MAX_FRICTION

        return friction.roundToInt().coerceIn(0, MAX_FRICTION.toInt()).toByte()
    }

    companion object {
        const val MAX_FRICTION = 10f
        const val DEFAULT_SNAP_RADIUS = 48

        private const val BLOCKED_INT = 0xFF
        private const val LADDER_INT = 0xFE
        private val BLOCKED: Byte = BLOCKED_INT.toByte()
        private val LADDER: Byte = LADDER_INT.toByte()

        private const val RED_WEIGHT = 0.2126f
        private const val GREEN_WEIGHT = 0.7152f
        private const val BLUE_WEIGHT = 0.0722f

        /** sRGB -> linear, computed once instead of per pixel lookup. */
        private val LUMINANCE_LUT = FloatArray(256) { index ->
            val channel = index / 255f
            if (channel <= 0.04045f) {
                channel / 12.92f
            } else {
                ((channel + 0.055f) / 1.055f).pow(2.4f)
            }
        }

        /** Colours that can never be walked on, matching the minimap legend. */
        private val BLOCKED_COLORS = setOf(
            Color.rgb(255, 255, 0),
        )
    }
}
