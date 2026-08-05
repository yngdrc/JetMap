package app.aventurine.jetmapdemo.ui.modules.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmapdemo.ui.modules.main.composables.MainScreenContent
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val error by mainViewModel.errorStateFlow.collectAsStateWithLifecycle()
    val controller = mainViewModel.jetMapController

    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = error ?: "Nie można wczytać mapy")
        }
        return
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }

    val bottomSheetUIState by mainViewModel.bottomSheetUIStateFlow.collectAsStateWithLifecycle()
    val navigationState by mainViewModel.navigationStateFlow.collectAsStateWithLifecycle()
    val query by mainViewModel.queryStateFlow.collectAsStateWithLifecycle()
    val searchResults by mainViewModel.searchResultsFlow.collectAsStateWithLifecycle()

    LaunchedEffect(bottomSheetUIState, navigationState) {
        val shouldExpand = bottomSheetUIState !is MainBottomSheetUIState.Initial ||
                navigationState !is NavigationState.Idle

        if (shouldExpand) {
            scaffoldState.bottomSheetState.expand()
        } else {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    MainScreenContent(
        mainBottomSheetUIState = bottomSheetUIState,
        navigationState = navigationState,
        scaffoldState = scaffoldState,
        snackbarHostState = snackbarHostState,
        jetMapController = controller,
        query = query,
        searchResults = searchResults,
        onPlanRoute = mainViewModel::startRoutePlanning,
        onSwapEndpoints = mainViewModel::swapEndpoints,
        onStartNavigation = { mainViewModel.startNavigation() },
        onStopNavigation = mainViewModel::stopNavigation,
        onShowOverview = { mainViewModel.showRouteOverview() },
        onRecenter = { mainViewModel.recenter() },
        onFocusStep = { stepIndex -> mainViewModel.focusStep(stepIndex) },
        onCloseBottomSheet = mainViewModel::closeBottomSheet,
        onQueryChange = mainViewModel::onQueryChange,
        onSearchResultTap = mainViewModel::onMarkerSelected,
        onToggleMarkers = { controller.uiApi.toggleMarkers() },
        onToggleTerrainType = { controller.tileApi.toggleTerrainType() },
        onViewportPaddingChanged = mainViewModel::setViewportPadding
    )
}