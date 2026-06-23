package app.aventurine.jetmapdemo.data.models.marker.entities

import app.aventurine.jetmapdemo.data.base.RemoteEntity

data class MarkerRemoteEntity(
    val x: Int,
    val y: Int,
    val floorId: Int,
    val iconId: Int,
    val description: String,
    val uid: String
) : RemoteEntity() {
    override fun toLocalEntity(): MarkerLocalEntity {
        return MarkerLocalEntity(
            x = this.x,
            y = this.y,
            floorId = this.floorId,
            iconId = this.iconId,
            description = this.description,
            uid = this.uid
        )
    }

    override fun toEntity(): MarkerEntity {
        return MarkerEntity(
            x = this.x,
            y = this.y,
            floorId = this.floorId,
            iconId = this.iconId,
            description = this.description,
            uid = this.uid
        )
    }
}