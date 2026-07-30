package app.aventurine.jetmap.data.models.tile

import app.aventurine.jetmap.controller.tile.TerrainType
import com.google.gson.annotations.SerializedName

data class GetTileRequest(
    val x: Int,
    val y: Int,
    val floor: Int,

    @SerializedName("terrain_type")
    val terrainType: TerrainType
)