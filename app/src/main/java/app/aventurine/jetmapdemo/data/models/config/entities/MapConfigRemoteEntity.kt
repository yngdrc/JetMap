package app.aventurine.jetmapdemo.data.models.config.entities

import app.aventurine.jetmapdemo.data.models.Entity
import app.aventurine.jetmapdemo.data.models.LocalEntity
import app.aventurine.jetmapdemo.data.models.RemoteEntity
import app.aventurine.jetmapdemo.data.models.marker.entities.MarkerEntity
import com.google.gson.annotations.SerializedName

data class MapConfigRemoteEntity(
    @SerializedName("lowest_floor")
    val lowestFloor: Int,

    @SerializedName("base_floor")
    val baseFloor: Int,

    @SerializedName("highest_floor")
    val highestFloor: Int,

    @SerializedName("tile_size")
    val tileSize: Int,

    @SerializedName("min_x")
    val minX: Int,

    @SerializedName("min_y")
    val minY: Int,

    @SerializedName("max_x")
    val maxX: Int,

    @SerializedName("max_y")
    val maxY: Int,

    val width: Int,
    val height: Int
) : RemoteEntity() {
    override fun toLocalEntity(): MapConfigLocalEntity {
        return MapConfigLocalEntity.Default(
            lowestFloor = lowestFloor,
            baseFloor = baseFloor,
            highestFloor = highestFloor,
            tileSize = tileSize,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            width = width,
            height = height
        )
    }

    override fun toEntity(): Entity {
        return MapConfigEntity(
            lowestFloor = lowestFloor,
            baseFloor = baseFloor,
            highestFloor = highestFloor,
            tileSize = tileSize,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            width = width,
            height = height
        )
    }

}