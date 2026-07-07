package app.aventurine.jetmapdemo.data.models.config.entities

import android.os.Parcelable
import app.aventurine.jetmapdemo.data.models.LocalEntity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
sealed class MapConfigLocalEntity : LocalEntity(), Parcelable {

    @Parcelize
    @Serializable
    data object Empty : MapConfigLocalEntity()

    @Parcelize
    @Serializable
    data class Default(
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
    ) : MapConfigLocalEntity()

}