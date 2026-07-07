package app.aventurine.jetmapdemo.ui.modules.main.providers

import app.aventurine.jetmap.controller.tile.models.TileDescriptor
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.provider.TileProvider
import java.io.InputStream

class TileProviderImpl(
    private val fileStorage: FileStorage,
    private val mapConfig: MapConfigEntity
) : TileProvider {
    override fun getTileInputStream(
        tileDescriptor: TileDescriptor
    ): InputStream? {
        return fileStorage.getFileIfExists(
            fileName = "${tileDescriptor.terrainType.name}_${tileDescriptor.x * mapConfig.tileSize + mapConfig.minX}_${tileDescriptor.y * mapConfig.tileSize + mapConfig.minY}_${tileDescriptor.z}.png"
        ).getOrNull()?.inputStream()
    }
}