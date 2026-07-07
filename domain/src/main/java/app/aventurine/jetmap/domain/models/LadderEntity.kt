package app.aventurine.jetmap.domain.models

data class LadderEntity(
    val x: Int,
    val y: Int,
    val floor: Int,
) : Entity()