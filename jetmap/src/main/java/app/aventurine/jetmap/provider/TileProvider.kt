package app.aventurine.jetmap.provider

import java.io.InputStream

interface TileProvider {
    fun getTileInputStream(
        x: Int,
        y: Int,
        z: Int
    ): InputStream?
}