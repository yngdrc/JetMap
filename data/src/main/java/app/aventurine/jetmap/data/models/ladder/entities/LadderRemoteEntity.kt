package app.aventurine.jetmap.data.models.ladder.entities

import app.aventurine.jetmap.data.models.RemoteEntity
import app.aventurine.jetmap.domain.models.LadderEntity

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