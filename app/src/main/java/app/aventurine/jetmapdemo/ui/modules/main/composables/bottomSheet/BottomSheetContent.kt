package app.aventurine.jetmapdemo.ui.modules.main.composables.bottomSheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.ArrivedPanel
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.CalculatingRoutePanel
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.NavigationBottomBar
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.RoutePreviewPanel
import app.aventurine.jetmapdemo.ui.modules.main.composables.navigation.RouteStepsList
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState

@Composable
fun BottomSheetContent(
    uiState: MainBottomSheetUIState,
    navigationState: NavigationState,
    onPlanRoute: () -> Unit,
    onSwapEndpoints: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onShowOverview: () -> Unit,
    onFocusStep: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        when (uiState) {
            is MainBottomSheetUIState.Initial -> BottomSheetInitialContent()

            is MainBottomSheetUIState.MarkerDetails -> BottomSheetMarkerDetailsContent(
                markerDescriptor = uiState.markerDescriptor,
                onNavigate = onPlanRoute,
                onClose = onClose
            )

            is MainBottomSheetUIState.RoutePlanning -> BottomSheetRoutePlanningContent(
                origin = uiState.origin,
                destination = uiState.destination,
                navigationState = navigationState,
                onSwapEndpoints = onSwapEndpoints,
                onStartNavigation = onStartNavigation,
                onStopNavigation = onStopNavigation,
                onShowOverview = onShowOverview,
                onFocusStep = onFocusStep,
                onClose = onClose
            )
        }
    }
}

/**
 * Never empty: a zero height sheet makes `BottomSheetScaffold.partialExpand()` misbehave.
 */
@Composable
fun BottomSheetInitialContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Wybierz punkt na mapie lub wyszukaj miejsce",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BottomSheetMarkerDetailsContent(
    markerDescriptor: MarkerDescriptor,
    onNavigate: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomSheetMarkerDetailsRow(markerDescriptor = markerDescriptor)

        Row {
            IconButton(onClick = onNavigate) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_directions),
                    contentDescription = "Nawiguj"
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Zamknij"
                )
            }
        }
    }
}

/**
 * Origin / destination picker plus the state driven navigation panels.
 */
@Composable
fun BottomSheetRoutePlanningContent(
    origin: MarkerDescriptor?,
    destination: MarkerDescriptor,
    navigationState: NavigationState,
    onSwapEndpoints: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onShowOverview: () -> Unit,
    onFocusStep: (Int) -> Unit,
    onClose: () -> Unit
) {
    var showSteps by remember { mutableStateOf(value = false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                EndpointRow(
                    label = "Skąd",
                    markerDescriptor = origin,
                    placeholder = "Dotknij punktu startowego na mapie"
                )

                EndpointRow(
                    label = "Dokąd",
                    markerDescriptor = destination,
                    placeholder = ""
                )
            }

            IconButton(
                onClick = onSwapEndpoints,
                enabled = origin != null
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_up),
                    contentDescription = "Zamień punkty"
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Zamknij"
                )
            }
        }

        when (navigationState) {
            is NavigationState.Calculating -> CalculatingRoutePanel()

            is NavigationState.RouteNotFound -> Text(
                modifier = Modifier.padding(all = 16.dp),
                text = "Nie znaleziono trasy",
                color = MaterialTheme.colorScheme.error
            )

            is NavigationState.Preview -> {
                RoutePreviewPanel(
                    route = navigationState.route,
                    onStart = onStartNavigation,
                    onShowSteps = { showSteps = !showSteps },
                    onCancel = onClose
                )

                AnimatedVisibility(visible = showSteps) {
                    RouteStepsList(
                        steps = navigationState.route.steps,
                        onStepClick = onFocusStep
                    )
                }
            }

            is NavigationState.Navigating -> NavigationBottomBar(
                state = navigationState,
                onStop = onStopNavigation,
                onOverview = onShowOverview
            )

            is NavigationState.Arrived -> ArrivedPanel(onClose = onClose)

            NavigationState.Idle -> Unit
        }
    }
}

@Composable
private fun EndpointRow(
    label: String,
    markerDescriptor: MarkerDescriptor?,
    placeholder: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (markerDescriptor != null) {
            BottomSheetMarkerDetailsRow(markerDescriptor = markerDescriptor)
        } else {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun BottomSheetInitialPreview() {
    BottomSheetInitialContent()
}