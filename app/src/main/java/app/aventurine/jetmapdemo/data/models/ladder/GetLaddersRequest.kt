package app.aventurine.jetmapdemo.data.models.ladder

data class GetLaddersRequest(
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val floor: Int
)