package app.aventurine.jetmap.data.models.config.entities

import app.aventurine.jetmap.data.models.RemoteEntity
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
) : RemoteEntity()