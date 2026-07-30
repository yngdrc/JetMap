package app.aventurine.jetmap.controller.path

import android.graphics.Color

enum class BlockType(val color: Int) {
    EMPTY(color = Color.rgb(0, 0, 0)),
    TREE(color = Color.rgb(0, 102, 0)),
    GRASS(color = Color.rgb(0, 204, 0)),
    WATER(color = Color.rgb(51, 102, 153)),
    ROCK(color = Color.rgb(102, 102, 102)),
    EARTH(color = Color.rgb(153, 102, 51)),
    STONE(color = Color.rgb(153, 153, 153)),
    GRASS_LIGHT(color = Color.rgb(153, 255, 102)),
    ICE(color = Color.rgb(204, 255, 255)),
    WALL(color = Color.rgb(255, 51, 0)),
    LAVA(color = Color.rgb(255, 102, 0)),
    SAND(color = Color.rgb(255, 204, 153)),
    LADDER(color = Color.rgb(255, 255, 0)),
    SNOW(color = Color.rgb(255, 255, 255)),;

    val isWalkable: Boolean
        get() = when (this) {
            EMPTY, TREE, WATER, ROCK, WALL, LAVA -> false
            else -> true
        }

    companion object {
        fun fromColor(color: Int): BlockType {
            return entries.find { blockType ->
                blockType.color == color
            } ?: EMPTY
        }
    }
}