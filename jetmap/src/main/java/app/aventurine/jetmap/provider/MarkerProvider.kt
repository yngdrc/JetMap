package app.aventurine.jetmap.provider

import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import java.io.InputStream

interface MarkerProvider {
    suspend fun getMarkerInputStream(
        markerDescriptor: MarkerDescriptor
    ): InputStream?

    suspend fun getMarker(
        x: Int,
        y: Int,
        z: Int,
        tapArea: Float
    ): MarkerDescriptor

    suspend fun getMarkers(
        visibleAreaRect: Rect,
        level: Int
    ): List<MarkerDescriptor>
}