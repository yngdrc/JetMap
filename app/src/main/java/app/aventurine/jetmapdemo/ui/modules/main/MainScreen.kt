package app.aventurine.jetmapdemo.ui.modules.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.utils.gestureApi
import app.aventurine.jetmap.utils.motionApi
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.composables.BottomSheetContent
import app.aventurine.jetmapdemo.ui.modules.main.composables.MapOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val focusedMarker by mainViewModel.jetMapController.gestureApi.focusedMarkerFlow
        .collectAsStateWithLifecycle()

    val query by mainViewModel.queryStateFlow.collectAsStateWithLifecycle()
    val searchResults by mainViewModel.searchResultsFlow.collectAsStateWithLifecycle()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    LaunchedEffect(focusedMarker) {
        when {
            focusedMarker != null -> scaffoldState.bottomSheetState.expand()
            else -> scaffoldState.bottomSheetState.partialExpand()
        }
    }

    MainScreenContent(
        focusedMarker = focusedMarker,
        scaffoldState = scaffoldState,
        jetMapController = mainViewModel.jetMapController,
        query = query,
        searchResults = searchResults,
        onCloseBottomSheet = {
            mainViewModel.jetMapController.gestureApi.changeFocusedMarker(focusedMarker = null)
            mainViewModel.jetMapController.pathfindingController.clear()
        },
        onQueryChange = mainViewModel::onQueryChange,
        onSearch = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    focusedMarker: MarkerDescriptor?,
    scaffoldState: BottomSheetScaffoldState,
    jetMapController: JetMapController,
    query: String,
    searchResults: List<MarkerEntity>,
    onCloseBottomSheet: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetContent = {
            if (focusedMarker == null) {
                return@BottomSheetScaffold
            }

            BottomSheetContent(
                focusedMarker = focusedMarker,
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
            MapOverlay(
                modifier = Modifier.padding(innerPadding),
                query = query,
                searchResults = searchResults,
                currentLevel = level,
                onChangeLevel = jetMapController.motionApi::changeLevel,
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