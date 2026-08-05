package app.aventurine.jetmap.controller.tile

import androidx.compose.runtime.Immutable
import app.aventurine.jetmap.controller.tile.models.Tile

/**
 * [tiles] are the tiles of the active level/terrain, [fallbackTiles] are already decoded tiles for
 * the same map positions from another level. Drawing the fallback underneath removes the blank
 * flash while switching floors.
 */
@Immutable
data class TileState(
    val tiles: List<Tile> = emptyList(),
    val fallbackTiles: List<Tile> = emptyList()
)

