package app.aventurine.jetmap.data.models.config.entities

import app.aventurine.jetmap.data.models.LocalEntity
import kotlinx.serialization.Serializable

@Serializable
sealed class MapConfigLocalEntity : LocalEntity() {

    @Serializable
    data object Empty : MapConfigLocalEntity()

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