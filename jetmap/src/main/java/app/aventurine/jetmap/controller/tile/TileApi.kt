package app.aventurine.jetmap.controller.tile

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow

interface TileApi {
    val tileState: StateFlow<TileState>
    val terrainState: StateFlow<TerrainType>
}