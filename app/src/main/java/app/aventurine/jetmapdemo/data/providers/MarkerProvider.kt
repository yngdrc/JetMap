package app.aventurine.jetmapdemo.data.providers

import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import java.io.InputStream

class MarkerProvider(
    private val markerRepository: MarkerRepository,
    private val resources: Resources
) : MarkerProvider {
    override suspend fun getMarkerInputStream(
        markerDescriptor: MarkerDescriptor
    ): InputStream {
        return resources.openRawResource(markerDescriptor.iconId)
    }

    override suspend fun getMarker(x: Int, y: Int, z: Int): MarkerDescriptor {
        val markerEntity = markerRepository.get(x = x, y = y, z = z)
        return MarkerDescriptor(
            x = markerEntity?.x ?: x,
            y = markerEntity?.y ?: y,
            z = markerEntity?.floorId ?: z,
            iconId = markerEntity?.iconDrawableRes ?: R.drawable.ic_map_player,
            description = markerEntity?.description ?: "$x, $y"
        )
    }

    override suspend fun getMarkers(visibleAreaRect: Rect, level: Int): List<MarkerDescriptor> {
        return markerRepository.getMarkersByCoordinates(
            coordinates = JetMapConfig.Coordinates(
                startX = visibleAreaRect.left.toInt(),
                startY = visibleAreaRect.top.toInt(),
                endX = visibleAreaRect.right.toInt(),
                endY = visibleAreaRect.bottom.toInt()
            ),
            floorId = level
        ).map { markerEntity ->
            MarkerDescriptor(
                x = markerEntity.x,
                y = markerEntity.y,
                z = markerEntity.floorId,
                description = markerEntity.description,
                iconId = markerEntity.iconDrawableRes
            )
        }
    }
}