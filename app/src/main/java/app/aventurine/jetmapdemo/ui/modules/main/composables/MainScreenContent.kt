package app.aventurine.jetmapdemo.ui.modules.main.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.motion.CameraMode
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.composables.bottomSheet.BottomSheetContent
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.ManeuverBanner
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    mainBottomSheetUIState: MainBottomSheetUIState,
    navigationState: NavigationState,
    scaffoldState: BottomSheetScaffoldState,
    snackbarHostState: SnackbarHostState,
    jetMapController: JetMapController,
    query: String,
    searchResults: List<MarkerEntity>,
    onPlanRoute: () -> Unit,
    onSwapEndpoints: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onShowOverview: () -> Unit,
    onRecenter: () -> Unit,
    onFocusStep: (Int) -> Unit,
    onCloseBottomSheet: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchResultTap: (MarkerEntity) -> Unit,
    onToggleMarkers: () -> Unit,
    onToggleTerrainType: () -> Unit,
    onViewportPaddingChanged: (left: Float, top: Float, right: Float, bottom: Float) -> Unit
) {
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = SHEET_PEEK_HEIGHT.dp,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        sheetContent = {
            BottomSheetContent(
                uiState = mainBottomSheetUIState,
                navigationState = navigationState,
                onPlanRoute = onPlanRoute,
                onSwapEndpoints = onSwapEndpoints,
                onStartNavigation = onStartNavigation,
                onStopNavigation = onStopNavigation,
                onShowOverview = onShowOverview,
                onFocusStep = onFocusStep,
                onClose = onCloseBottomSheet
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            JetMap(
                jetMapController = jetMapController,
                backgroundColorSelector = { level ->
                    when (level) {
                        WATER_LEVEL -> Color(red = 51, green = 102, blue = 153)
                        else -> Color.Black
                    }
                }
            )

            val initializationState by jetMapController.initializationState
            if (!initializationState) {
                return@Box
            }

            val level by jetMapController.motionApi.levelStateFlow
                .collectAsStateWithLifecycle()

            val cameraMode by jetMapController.navigationApi.cameraModeFlow
                .collectAsStateWithLifecycle()

            // The route must be framed inside the part of the screen that is not covered by UI.
            LaunchedEffect(innerPadding, density) {
                with(density) {
                    onViewportPaddingChanged(
                        0f,
                        MANEUVER_BANNER_RESERVED_HEIGHT.dp.toPx(),
                        0f,
                        (SHEET_PEEK_HEIGHT.dp + insets.calculateBottomPadding()).toPx()
                    )
                }
            }

            (navigationState as? NavigationState.Navigating)?.let { navigating ->
                ManeuverBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(all = 16.dp),
                    state = navigating
                )
            }

            MapOverlay(
                modifier = Modifier.padding(innerPadding),
                query = query,
                searchResults = searchResults,
                currentLevel = level,
                showSearch = navigationState !is NavigationState.Navigating,
                onChangeLevel = jetMapController.motionApi::changeLevel,
                onToggleMarkers = onToggleMarkers,
                onToggleTerrainType = onToggleTerrainType,
                onQueryChange = onQueryChange,
                onSearchResultTap = onSearchResultTap
            )

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(all = 16.dp),
                visible = navigationState is NavigationState.Navigating &&
                        cameraMode == CameraMode.FREE
            ) {
                SmallFloatingActionButton(onClick = onRecenter) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                        contentDescription = "Wyśrodkuj"
                    )
                }
            }
        }
    }
}

private const val SHEET_PEEK_HEIGHT = 96
private const val MANEUVER_BANNER_RESERVED_HEIGHT = 120
private const val WATER_LEVEL = 7
