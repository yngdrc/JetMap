package app.aventurine.jetmap.data.mappers

import app.aventurine.jetmap.data.models.marker.entities.MarkerLocalEntity
import app.aventurine.jetmap.data.models.marker.entities.MarkerRemoteEntity
import app.aventurine.jetmap.domain.models.MarkerEntity

fun MarkerEntity.toLocalEntity(): MarkerLocalEntity {
    return MarkerLocalEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}

fun MarkerEntity.toRemoteEntity(): MarkerRemoteEntity {
    return MarkerRemoteEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}

fun MarkerLocalEntity.toEntity(): MarkerEntity {
    return MarkerEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}

fun MarkerLocalEntity.toRemoteEntity(): MarkerRemoteEntity {
    return MarkerRemoteEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}

fun MarkerRemoteEntity.toEntity(): MarkerEntity {
    return MarkerEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}

fun MarkerRemoteEntity.toLocalEntity(): MarkerLocalEntity {
    return MarkerLocalEntity(
        x = x,
        y = y,
        floor = floor,
        iconId = iconId,
        description = description
    )
}