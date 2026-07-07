package app.aventurine.jetmap.domain.models

data class MarkerEntity(
    val x: Int,
    val y: Int,
    val floor: Int,
    val iconId: Int,
    val description: String
) : Entity()