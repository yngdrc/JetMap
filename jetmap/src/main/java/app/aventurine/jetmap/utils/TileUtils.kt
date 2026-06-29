package app.aventurine.jetmap.utils

import app.aventurine.jetmap.controller.motion.VisibleArea
import app.aventurine.jetmap.controller.tile.models.Tile

internal fun Tile.shouldRecycle(
    visibleArea: VisibleArea,
    level: Int
): Boolean {
    return x !in (visibleArea.left..visibleArea.right)
            || y !in (visibleArea.top..visibleArea.bottom)
            || z != level
}