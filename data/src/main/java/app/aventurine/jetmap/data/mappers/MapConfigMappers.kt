package app.aventurine.jetmap.data.mappers

import app.aventurine.jetmap.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmap.data.models.config.entities.MapConfigRemoteEntity
import app.aventurine.jetmap.domain.models.MapConfigEntity

fun MapConfigEntity.toLocalEntity(): MapConfigLocalEntity {
    return MapConfigLocalEntity.Default(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}

fun MapConfigEntity.toRemoteEntity(): MapConfigRemoteEntity {
    return MapConfigRemoteEntity(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}

fun MapConfigLocalEntity.toEntity(): MapConfigEntity? {
    if (this !is MapConfigLocalEntity.Default) {
        return null
    }

    return MapConfigEntity(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}

fun MapConfigLocalEntity.toRemoteEntity(): MapConfigRemoteEntity? {
    if (this !is MapConfigLocalEntity.Default) {
        return null
    }

    return MapConfigRemoteEntity(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}

fun MapConfigRemoteEntity.toEntity(): MapConfigEntity {
    return MapConfigEntity(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}

fun MapConfigRemoteEntity.toLocalEntity(): MapConfigLocalEntity.Default {
    return MapConfigLocalEntity.Default(
        lowestFloor = lowestFloor,
        baseFloor = baseFloor,
        highestFloor = highestFloor,
        tileSize = tileSize,
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
        width = width,
        height = height
    )
}