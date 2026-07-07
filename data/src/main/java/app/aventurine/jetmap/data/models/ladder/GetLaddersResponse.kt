package app.aventurine.jetmap.data.models.ladder

import app.aventurine.jetmap.data.models.ladder.entities.LadderRemoteEntity

data class GetLaddersResponse(
    val ladders: List<LadderRemoteEntity>
)