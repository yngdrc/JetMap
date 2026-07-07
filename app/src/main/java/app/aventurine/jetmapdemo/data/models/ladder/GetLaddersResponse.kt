package app.aventurine.jetmapdemo.data.models.ladder

import app.aventurine.jetmapdemo.data.models.ladder.entities.LadderRemoteEntity

data class GetLaddersResponse(
    val ladders: List<LadderRemoteEntity>
)