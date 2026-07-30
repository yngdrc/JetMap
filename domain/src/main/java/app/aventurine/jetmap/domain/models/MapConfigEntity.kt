package app.aventurine.jetmap.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapConfigEntity(
    val lowestFloor: Int,
    val baseFloor: Int,
    val highestFloor: Int,
    val tileSize: Int,
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
    val width: Int,
    val height: Int,
): Entity(), Parcelable