package app.aventurine.jetmapdemo.ui.modules.main.providers

import android.content.Context
import android.content.res.Resources
import app.aventurine.jetmap.domain.fileStorage.FileStorage
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.domain.repositories.LadderRepository
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.provider.MarkerProvider
import app.aventurine.jetmap.provider.PathProvider
import app.aventurine.jetmap.provider.TileProvider
import app.aventurine.jetmapdemo.utils.AStarPathFinder
import app.aventurine.jetmapdemo.utils.MinimapStitcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class MapProviders(
    val tileProvider: TileProvider,
    val markerProvider: MarkerProvider,
    val pathProvider: PathProvider
)

/**
 * Builds the map providers for a concrete [MapConfigEntity].
 *
 * The providers need runtime data (the map config comes from navigation arguments), so they can
 * not be plain `@Provides` bindings. Injecting the factory keeps the `Context`, `Resources` and
 * repositories out of the ViewModel, which used to construct everything by hand.
 */
class MapProvidersFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val resources: Resources,
    private val markerRepository: MarkerRepository,
    private val ladderRepository: LadderRepository,
    private val fileStorage: FileStorage
) {
    fun create(mapConfig: MapConfigEntity): MapProviders = MapProviders(
        tileProvider = TileProviderImpl(
            fileStorage = fileStorage,
            mapConfig = mapConfig
        ),
        markerProvider = MarkerProviderImpl(
            markerRepository = markerRepository,
            resources = resources
        ),
        pathProvider = PathProviderImpl(
            pathFinder = AStarPathFinder(),
            mapStitcher = MinimapStitcher(
                context = context,
                mapConfig = mapConfig
            ),
            ladderRepository = ladderRepository
        )
    )
}

