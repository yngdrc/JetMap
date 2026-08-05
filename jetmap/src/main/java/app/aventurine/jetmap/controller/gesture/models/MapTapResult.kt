package app.aventurine.jetmap.controller.gesture.models

import androidx.compose.ui.geometry.Offset
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor

/**
 * Result of a tap: where the user tapped in map space and which marker (if any) was hit.
 *
 * [marker] is null for empty space. Consumers that need a "drop a pin anywhere" behaviour
 * (route origin picking) can fall back to [mapOffset]; consumers that only care about real
 * points of interest simply ignore taps with a null marker.
 */
data class MapTapResult(
    val mapOffset: Offset,
    val level: Int,
    val marker: MarkerDescriptor?
) {
    val x: Int get() = mapOffset.x.toInt()
    val y: Int get() = mapOffset.y.toInt()
}

