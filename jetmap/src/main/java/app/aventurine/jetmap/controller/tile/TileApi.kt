package app.aventurine.jetmap.controller.tile

import kotlinx.coroutines.flow.StateFlow

interface TileApi {
    val tileStateFlow: StateFlow<TileState>
    val terrainTypeStateFlow: StateFlow<TerrainType>
}