package app.aventurine.jetmap.data.models.marker.entities

import app.aventurine.jetmap.data.models.RemoteEntity
import com.google.gson.annotations.SerializedName

data class MarkerRemoteEntity(
    val x: Int,
    val y: Int,

    @SerializedName("floor")
    val floor: Int,

    @SerializedName("icon_id")
    val iconId: Int,

    val description: String
) : RemoteEntity()