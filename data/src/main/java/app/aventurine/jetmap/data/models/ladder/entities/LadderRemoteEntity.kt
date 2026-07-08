package app.aventurine.jetmap.data.models.ladder.entities

import app.aventurine.jetmap.data.models.RemoteEntity

data class LadderRemoteEntity(
    val x: Int,
    val y: Int,
    val floor: Int
) : RemoteEntity()