package app.aventurine.jetmap.provider

import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import java.io.InputStream

interface MarkerProvider {
    suspend fun getMarkerInputStream(
        markerDescriptor: MarkerDescriptor
    ): InputStream?

    /**
     * Returns the marker under the tap, or `null` when the user tapped empty space.
     * Implementations must not fabricate a marker from the raw coordinates.
     */
    suspend fun getMarker(
        x: Int,
        y: Int,
        z: Int,
        tapArea: Float
    ): MarkerDescriptor?

    suspend fun getMarkers(
        visibleAreaRect: Rect,
        level: Int
    ): List<MarkerDescriptor>
}

