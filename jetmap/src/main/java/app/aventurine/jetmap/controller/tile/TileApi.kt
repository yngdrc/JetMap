package app.aventurine.jetmap.controller.tile

import kotlinx.coroutines.flow.StateFlow

interface TileApi {
    val tileState: StateFlow<TileState>
}