package app.aventurine.jetmapdemo.data.models.marker.entities

import androidx.annotation.DrawableRes
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.data.models.Entity

data class MarkerEntity(
    val x: Int,
    val y: Int,
    val floorId: Int,
    val iconId: Int,
    val description: String
) : Entity() {

    @get:DrawableRes
    val iconDrawableRes: Int
        get() = when (iconId) {
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
            else -> throw IllegalArgumentException()
        }

//    fun calculateX(mapConfig: JetMapConfig): Double {
//        return ((x.toDouble() - mapConfig.leftMostTileCoordinate) / mapConfig.mapWidth)
//    }
//
//    fun calculatedY(mapConfig: JetMapConfig): Double {
//        return ((y.toDouble() - mapConfig.topMostTileCoordinate) / mapConfig.mapHeight)
//    }
}