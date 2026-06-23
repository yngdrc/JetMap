package app.aventurine.jetmap.provider

import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.models.Marker

interface MarkerProvider {
    suspend fun getMarker(
        x: Int,
        y: Int,
        z: Int
    ): Marker?

    suspend fun getMarkers(
        visibleAreaRect: Rect,
        level: Int
    ): List<Marker>
}