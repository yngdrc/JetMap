package app.aventurine.jetmap.data.mappers

import app.aventurine.jetmap.data.models.ladder.entities.LadderLocalEntity
import app.aventurine.jetmap.data.models.ladder.entities.LadderRemoteEntity
import app.aventurine.jetmap.domain.models.LadderEntity

fun LadderEntity.toLocalEntity(): LadderLocalEntity {
    return LadderLocalEntity(
        x = x,
        y = y,
        floor = floor,
    )
}

fun LadderEntity.toRemoteEntity(): LadderRemoteEntity {
    return LadderRemoteEntity(
        x = x,
        y = y,
        floor = floor
    )
}

fun LadderLocalEntity.toEntity(): LadderEntity {
    return LadderEntity(
        x = x,
        y = y,
        floor = floor
    )
}

fun LadderLocalEntity.toRemoteEntity(): LadderRemoteEntity {
    return LadderRemoteEntity(
        x = x,
        y = y,
        floor = floor
    )
}

fun LadderRemoteEntity.toEntity(): LadderEntity {
    return LadderEntity(
        x = x,
        y = y,
        floor = floor
    )
}

fun LadderRemoteEntity.toLocalEntity(): LadderLocalEntity {
    return LadderLocalEntity(
        x = x,
        y = y,
        floor = floor
    )
}