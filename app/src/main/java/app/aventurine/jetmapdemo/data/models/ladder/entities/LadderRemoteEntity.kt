package app.aventurine.jetmapdemo.data.models.ladder.entities

import app.aventurine.jetmapdemo.data.models.RemoteEntity

data class LadderRemoteEntity(
    val x: Int,
    val y: Int,
    val floor: Int
) : RemoteEntity() {
    override fun toLocalEntity(): LadderLocalEntity {
        return LadderLocalEntity(
            x = x,
            y = y,
            floor = floor
        )
    }

    override fun toEntity(): LadderEntity {
        return LadderEntity(
            x = x,
            y = y,
            floor = floor
        )
    }
}