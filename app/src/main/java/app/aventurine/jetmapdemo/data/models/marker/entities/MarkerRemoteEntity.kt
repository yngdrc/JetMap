package app.aventurine.jetmapdemo.data.models.marker.entities

import app.aventurine.jetmapdemo.data.models.RemoteEntity
import com.google.gson.annotations.SerializedName

data class MarkerRemoteEntity(
    val x: Int,
    val y: Int,

    @SerializedName("floor")
    val floor: Int,

    @SerializedName("icon_id")
    val iconId: Int,

    val description: String
) : RemoteEntity() {
    override fun toLocalEntity(): MarkerLocalEntity {
        return MarkerLocalEntity(
            x = this.x,
            y = this.y,
            floorId = this.floor,
            iconId = this.iconId,
            description = this.description,
        )
    }

    override fun toEntity(): MarkerEntity {
        return MarkerEntity(
            x = this.x,
            y = this.y,
            floorId = this.floor,
            iconId = this.iconId,
            description = this.description
        )
    }
}