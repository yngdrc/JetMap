package app.aventurine.jetmapdemo.utils

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmapdemo.data.repositories.marker.MarkerRepository
import kotlinx.coroutines.Dispatchers

class MarkerExtractor(
    private val assetManager: AssetManager,
    private val resources: Resources,
    private val markerRepository: MarkerRepository,
    private val jetMapConfig: JetMapConfig
) {
    private companion object {
        const val MARKERS_PATH = "minimap/minimapmarkers.bin"
        const val SEPARATOR: Byte = 0x0A
        const val X_START_BYTE: Byte = 0x08
        const val Y_START_BYTE: Byte = 0x10
        const val Z_START_BYTE: Byte = 0x18
        const val ICON_START_BYTE: Byte = 0x10
        const val STRING_START_BYTE: Byte = 0x1A
        val MARKER_END_BYTES = byteArrayOf(0x20, 0x00)
    }

    suspend fun extractMarkers() = with(receiver = Dispatchers.IO) {
        val markers = mutableListOf<MarkerLocalEntity>()
        assetManager.open(MARKERS_PATH).use { inputStream ->
            var data = inputStream.readBytes()
            var block: ByteArray

            do {
                block = getBlock(data = data)
                getMarkerData(block = block)?.let(markers::add)
                data = data.drop(block.size).toByteArray()
            } while (block.isNotEmpty())
        }

        markerRepository.persist(entities = markers)
    }

    private fun getBlock(data: ByteArray): ByteArray {
        val startIndex = data.indexOf(element = SEPARATOR)
        if (startIndex == -1) return byteArrayOf()

        val blockSize = data.getOrNull(index = startIndex + 1)?.toInt() ?: return byteArrayOf()
        return data.copyOfRange(startIndex, blockSize) + MARKER_END_BYTES
    }

    private fun getMarkerData(block: ByteArray): MarkerLocalEntity? {
        val iterator = block.iterator()
        val dataMap = mutableMapOf<MarkerProperty, Any>()

        var property = MarkerProperty.BLOCK_START
        while (iterator.hasNext()) {
            when (property) {
                MarkerProperty.BLOCK_START -> {
                    // iterate to block size
                    iterator.next()
                    property = MarkerProperty.BLOCK_SIZE
                }

                MarkerProperty.BLOCK_SIZE -> {
                    // get block size and iterate to coordinate block start
                    val blockSize = iterator.next()
                    dataMap[MarkerProperty.BLOCK_SIZE] = blockSize

                    property = MarkerProperty.COORDINATE_BLOCK_START
                }

                MarkerProperty.COORDINATE_BLOCK_START -> {
                    // iterate to coordinate block size
                    iterator.next()
                    property = MarkerProperty.COORDINATE_BLOCK_SIZE
                }

                MarkerProperty.COORDINATE_BLOCK_SIZE -> {
                    // get coordinate block size and iterate to x block start
                    val coordinateBlockSize = iterator.next()
                    dataMap[MarkerProperty.COORDINATE_BLOCK_SIZE] = coordinateBlockSize

                    property = MarkerProperty.X_BLOCK_START
                }

                MarkerProperty.X_BLOCK_START -> {
                    // iterate to x1, x2, x3
                    iterator.next()

                    property = MarkerProperty.X
                }

                MarkerProperty.X -> {
                    // get x1, x2, x3 and iterate to y block start
                    val x1 = iterator.next()
                    val x2 = iterator.next()
                    val x3 = iterator.next()
                    dataMap[MarkerProperty.X] = x1 + 0x80 * x2 + 0x4000 * x3 - 0x4080 + jetMapConfig.mapSize.width / 2

                    property = MarkerProperty.Y_BLOCK_START
                }

                MarkerProperty.Y_BLOCK_START -> {
                    // iterate to y1, y2, y3
                    iterator.next()

                    property = MarkerProperty.Y
                }

                MarkerProperty.Y -> {
                    // get y1, y2, y3 and iterate to z block start
                    val y1 = iterator.next()
                    val y2 = iterator.next()
                    val y3 = iterator.next()
                    dataMap[MarkerProperty.Y] = y1 + 0x80 * y2 + 0x4000 * y3 - 0x4080 + jetMapConfig.mapSize.height

                    property = MarkerProperty.Z_BLOCK_START
                }

                MarkerProperty.Z_BLOCK_START -> {
                    // iterate to z
                    iterator.next()

                    property = MarkerProperty.Z
                }

                MarkerProperty.Z -> {
                    // get z and iterate to icon block start
                    val z = iterator.next()
                    dataMap[MarkerProperty.Z] = z.toInt()

                    property = MarkerProperty.ICON_BLOCK_START
                }

                MarkerProperty.ICON_BLOCK_START -> {
                    // iterate to icon
                    iterator.next()

                    property = MarkerProperty.ICON
                }

                MarkerProperty.ICON -> {
                    // get icon and iterate to string block start
                    val icon = iterator.next()
                    dataMap[MarkerProperty.ICON] = icon.toInt()

                    property = MarkerProperty.STRING_BLOCK_START
                }

                MarkerProperty.STRING_BLOCK_START -> {
                    // iterate to string block size
                    iterator.next()

                    property = MarkerProperty.STRING_BLOCK_SIZE
                }

                MarkerProperty.STRING_BLOCK_SIZE -> {
                    // get string block size and iterate to string
                    val stringBlockSize = iterator.next()
                    dataMap[MarkerProperty.STRING_BLOCK_SIZE] = stringBlockSize.toInt()

                    property = MarkerProperty.STRING
                }

                MarkerProperty.STRING -> {
                    // get string and break
                    val stringBytes = ByteArray(dataMap[MarkerProperty.STRING_BLOCK_SIZE] as Int)
                    for (i in stringBytes.indices) {
                        stringBytes[i] = iterator.next()
                    }

                    dataMap[MarkerProperty.STRING] = String(stringBytes)
                    property = MarkerProperty.BLOCK_END
                }

                MarkerProperty.BLOCK_END -> {
                    // break
                    break
                }
            }
        }

        return MarkerLocalEntity(
            x = dataMap[MarkerProperty.X] as? Int ?: return null,
            y = dataMap[MarkerProperty.Y] as? Int ?: return null,
            floorId = dataMap[MarkerProperty.Z] as? Int ?: return null,
            iconId = dataMap[MarkerProperty.ICON] as? Int ?: return null,
            description = dataMap[MarkerProperty.STRING] as? String ?: ""
        )
    }

    private fun getBitmapForIcon(icon: Int): Bitmap? {
        val drawableRes = when (icon) {
            0x00 -> R.drawable.marker_checkmark
            0x01 -> R.drawable.marker_questionmark
            0x02 -> R.drawable.marker_exclamationmark
            0x03 -> R.drawable.marker_star
            0x04 -> R.drawable.marker_crossmark
            0x05 -> R.drawable.marker_cross
            0x06 -> R.drawable.marker_lips
            0x07 -> R.drawable.marker_spear
            0x08 -> R.drawable.marker_sword
            0x09 -> R.drawable.marker_flag
            0x0A -> R.drawable.marker_lock
            0x0B -> R.drawable.marker_bag
            0x0C -> R.drawable.marker_skull
            0x0D -> R.drawable.marker_dollar
            0x0E -> R.drawable.marker_up
            0x0F -> R.drawable.marker_down
            0x10 -> R.drawable.marker_right
            0x11 -> R.drawable.marker_left
            0x12 -> R.drawable.marker_green_up
            0x13 -> R.drawable.marker_green_down
            else -> return null
        }

        return BitmapFactory.decodeResource(
            resources,
            drawableRes
        )
    }

    /**
     *    0x0A      [BLOCK_START]
     *                  [BLOCK_SIZE]
     *    0x0A          [COORDINATE_BLOCK_START]
     *                      [COORDINATE_BLOCK_SIZE]
     *    0x08              [X_BLOCK_START]
     *                          [X1]
     *                          [X2]
     *                          [X3]
     *    0x10              [Y_BLOCK_START]
     *                          [Y1]
     *                          [Y2]
     *                          [Y3]
     *    0x18          [Z_BLOCK_START]
     *                      [Z]
     *    0x10          [ICON_BLOCK_START]
     *                      [ICON]
     *    0x1A          [STRING_BLOCK_START]
     *                      [STRING_BLOCK_SIZE]
     *                      [STRING]
     *    0x20 0x00 [BLOCK_END]
     *
     */
    enum class MarkerProperty {
        BLOCK_START,
        BLOCK_SIZE,
        COORDINATE_BLOCK_START,
        COORDINATE_BLOCK_SIZE,
        X_BLOCK_START,
        X,
        Y_BLOCK_START,
        Y,
        Z_BLOCK_START,
        Z,
        ICON_BLOCK_START,
        ICON,
        STRING_BLOCK_START,
        STRING_BLOCK_SIZE,
        STRING,
        BLOCK_END
    }
}