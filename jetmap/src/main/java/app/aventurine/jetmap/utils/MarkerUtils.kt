package app.aventurine.jetmap.utils

import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.controller.marker.models.Marker

internal fun Marker.shouldRecycle(
    visibleAreaRect: Rect,
    level: Int
): Boolean {
    return x.toFloat() !in (visibleAreaRect.left..visibleAreaRect.right)
            || y.toFloat() !in (visibleAreaRect.top..visibleAreaRect.bottom)
            || z != level
}