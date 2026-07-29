package app.aventurine.jetmapdemo.ui.modules.main.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.controller.path.PathState
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmap.utils.gestureApi
import app.aventurine.jetmap.utils.motionApi
import app.aventurine.jetmap.utils.pathApi
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.composables.bottomSheet.BottomSheetContent
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    mainBottomSheetUIState: MainBottomSheetUIState,
    scaffoldState: BottomSheetScaffoldState,
    snackbarHostState: SnackbarHostState,
    jetMapController: JetMapController,
    query: String,
    searchResults: List<MarkerEntity>,
    onNavigate: () -> Unit,
    onCloseBottomSheet: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onToggleMarkers: () -> Unit,
    onToggleTerrainType: () -> Unit
) {
    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        sheetContent = {
            BottomSheetContent(
                uiState = mainBottomSheetUIState,
                onNavigate = onNavigate,
                onClose = onCloseBottomSheet
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            JetMap(
                jetMapController = jetMapController,
                myLocationResId = R.drawable.ic_map_player,
                backgroundColorSelector = { level ->
                    when (level) {
                        7 -> Color(red = 51, green = 102, blue = 153)
                        else -> Color.Black
                    }
                }
            )

            val initializationState by jetMapController.initializationState
            if (!initializationState) {
                return@Box
            }

            val level by jetMapController.motionApi.levelStateFlow.collectAsStateWithLifecycle()
            val pathState by jetMapController.pathApi.pathStateFlow
                .collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(pathState) {
                val (message, duration) = when (pathState) {
                    is PathState.FindingRoute -> "Finding route" to SnackbarDuration.Indefinite
                    is PathState.RouteNotFound -> "Route not found" to SnackbarDuration.Long
                    else -> {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        return@LaunchedEffect
                    }
                }

                snackbarHostState.showSnackbar(message = message, duration = duration)
            }

            MapOverlay(
                modifier = Modifier.padding(innerPadding),
                query = query,
                searchResults = searchResults,
                currentLevel = level,
                onChangeLevel = jetMapController.motionApi::changeLevel,
                onToggleMarkers = onToggleMarkers,
                onToggleTerrainType = onToggleTerrainType,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSearchResultTap = { markerEntity ->
                    jetMapController.gestureApi.changeFocusedMarker(
                        focusedMarker = MarkerDescriptor(
                            x = markerEntity.x,
                            y = markerEntity.y,
                            z = markerEntity.floor,
                            iconId = markerEntity.iconId,
                            description = markerEntity.description
                        )
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun MainScreenContentPreview() {

}