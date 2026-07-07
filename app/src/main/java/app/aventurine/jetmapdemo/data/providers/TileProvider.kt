package app.aventurine.jetmapdemo.data.providers

import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmapdemo.data.fileStorage.FileStorage
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigEntity
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import java.io.InputStream

class TileProvider(
    private val fileStorage: FileStorage,
    private val mapConfig: MapConfigLocalEntity.Default
) : TileProvider {
    override fun getTileInputStream(
        tileDescriptor: TileDescriptor
    ): InputStream? {
        return fileStorage.getFileIfExists(
            fileName = "${tileDescriptor.terrainType.name}_${tileDescriptor.x * mapConfig.tileSize + mapConfig.minX}_${tileDescriptor.y * mapConfig.tileSize + mapConfig.minY}_${tileDescriptor.z}.png"
        ).getOrNull()?.inputStream()
    }
}