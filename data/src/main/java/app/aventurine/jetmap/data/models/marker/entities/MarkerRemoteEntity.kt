package app.aventurine.jetmap.data.models.marker.entities

import app.aventurine.jetmap.data.models.RemoteEntity
import app.aventurine.jetmap.domain.models.MarkerEntity
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
            floor = this.floor,
            iconId = this.iconId,
            description = this.description,
        )
    }

    override fun toEntity(): MarkerEntity {
        return MarkerEntity(
            x = this.x,
            y = this.y,
            floor = this.floor,
            iconId = this.iconId,
            description = this.description
        )
    }
}