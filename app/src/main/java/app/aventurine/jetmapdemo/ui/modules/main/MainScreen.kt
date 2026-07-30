package app.aventurine.jetmapdemo.ui.modules.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.utils.gestureApi
import app.aventurine.jetmap.utils.pathApi
import app.aventurine.jetmap.utils.tileApi
import app.aventurine.jetmap.utils.uiApi
import app.aventurine.jetmapdemo.ui.modules.main.composables.MainScreenContent
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }

    val mainBottomSheetUIState by mainViewModel.bottomSheetUIState
    LaunchedEffect(mainBottomSheetUIState) {
        when (mainBottomSheetUIState) {
            is MainBottomSheetUIState.Initial -> scaffoldState.bottomSheetState.partialExpand()
            else -> scaffoldState.bottomSheetState.expand()
        }
    }

    val query by mainViewModel.queryStateFlow.collectAsStateWithLifecycle()
    val searchResults by mainViewModel.searchResultsFlow.collectAsStateWithLifecycle()

    val focusedMarker by mainViewModel.jetMapController.gestureApi.focusedMarkerFlow
        .collectAsStateWithLifecycle()

    MainScreenContent(
        mainBottomSheetUIState = mainViewModel.bottomSheetUIState.value,
        scaffoldState = scaffoldState,
        snackbarHostState = snackbarHostState,
        jetMapController = mainViewModel.jetMapController,
        query = query,
        searchResults = searchResults,
        onNavigate = {
            focusedMarker?.let {
                mainViewModel.setBottomSheetUIState(
                    uiState = MainBottomSheetUIState.Navigation(
                        startMarkerDescriptor = null,
                        endMarkerDescriptor = it
                    )
                )
            }
        },
        onCloseBottomSheet = {
            mainViewModel.jetMapController.gestureApi.changeFocusedMarker(focusedMarker = null)
            mainViewModel.jetMapController.pathApi.clear()
        },
        onQueryChange = mainViewModel::onQueryChange,
        onSearch = {},
        onToggleMarkers = {
            mainViewModel.jetMapController.uiApi.toggleMarkers()
        },
        onToggleTerrainType = {
            mainViewModel.jetMapController.tileApi.toggleTerrainType()
        }
    )
}