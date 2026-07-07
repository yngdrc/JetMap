package app.aventurine.jetmapdemo.data.models.ladder.entities

import app.aventurine.jetmapdemo.data.models.Entity

data class LadderEntity(
    val x: Int,
    val y: Int,
    val floor: Int,
) : Entity()