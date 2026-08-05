package app.aventurine.jetmapdemo.ui.modules.main.providers

import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.utils.getIconDrawableRes
import kotlinx.coroutines.CancellationException
import java.io.InputStream

class MarkerProviderImpl(
    private val markerRepository: MarkerRepository,
    private val resources: Resources
) : MarkerProvider {
    override suspend fun getMarkerInputStream(
        markerDescriptor: MarkerDescriptor
    ): InputStream? {
        return try {
            resources.openRawResource(markerDescriptor.iconId ?: return null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns `null` for empty space. Previously a synthetic marker was fabricated from the tap
     * coordinates, so tapping anywhere always opened the details sheet.
     */
    override suspend fun getMarker(x: Int, y: Int, z: Int, tapArea: Float): MarkerDescriptor? {
        val markerEntity = markerRepository.get(x = x, y = y, z = z, tapArea = tapArea)
            ?: return null

        return MarkerDescriptor(
            x = markerEntity.x,
            y = markerEntity.y,
            z = markerEntity.floor,
            iconId = getIconDrawableRes(iconId = markerEntity.iconId),
            description = markerEntity.description.ifEmpty { "${markerEntity.x}, ${markerEntity.y}" }
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
                z = markerEntity.floor,
                description = markerEntity.description,
                iconId = getIconDrawableRes(iconId = markerEntity.iconId)
            )
        }
    }
}