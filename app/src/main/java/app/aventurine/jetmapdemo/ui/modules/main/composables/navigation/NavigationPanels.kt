package app.aventurine.jetmapdemo.ui.modules.main.composables.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmap.controller.navigation.models.Maneuver
import app.aventurine.jetmap.controller.navigation.models.Route
import app.aventurine.jetmap.controller.navigation.models.RouteStep
import app.aventurine.jetmapdemo.R
import kotlin.math.roundToInt

/**
 * Top maneuver banner: the big arrow plus "za X" and the instruction, like Google Maps.
 */
@Composable
fun ManeuverBanner(
    modifier: Modifier = Modifier,
    state: NavigationState.Navigating
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
        ) {
            Icon(
                modifier = Modifier.size(size = 40.dp),
                painter = painterResource(id = state.currentStep.maneuver.iconRes()),
                contentDescription = state.currentStep.instruction
            )

            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = formatDistance(distance = state.distanceToNextManeuver),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = state.currentStep.instruction,
                    style = MaterialTheme.typography.bodyMedium
                )

                state.nextStep?.let { next ->
                    Text(
                        text = "Następnie: ${next.instruction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isOffRoute) {
                Text(
                    text = "Poza trasą",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Route preview card shown before the user starts navigating.
 */
@Composable
fun RoutePreviewPanel(
    modifier: Modifier = Modifier,
    route: Route,
    onStart: () -> Unit,
    onShowSteps: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = formatDuration(seconds = route.estimatedDurationSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDistance(distance = route.totalDistance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (route.levels.size > 1) {
                Text(
                    text = "Poziomy: ${route.levels.joinToString(separator = ", ")}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(
                modifier = Modifier.weight(weight = 1f),
                onClick = onStart
            ) {
                Text(text = "Start")
            }

            OutlinedButton(onClick = onShowSteps) {
                Text(text = "Kroki")
            }

            OutlinedButton(onClick = onCancel) {
                Text(text = "Anuluj")
            }
        }
    }
}

/**
 * Bottom bar during guidance: ETA, remaining distance and the exit button.
 */
@Composable
fun NavigationBottomBar(
    modifier: Modifier = Modifier,
    state: NavigationState.Navigating,
    onStop: () -> Unit,
    onOverview: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = formatDuration(seconds = state.remainingSeconds),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatDistance(distance = state.remainingDistance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(onClick = onOverview) {
            Text(text = "Podgląd")
        }

        Button(onClick = onStop) {
            Text(text = "Zakończ")
        }
    }
}

@Composable
fun RouteStepsList(
    modifier: Modifier = Modifier,
    steps: List<RouteStep>,
    onStepClick: (Int) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(items = steps) { index, step ->
            ListItem(
                modifier = Modifier.clip(shape = RoundedCornerShape(size = 8.dp)),
                leadingContent = {
                    Icon(
                        painter = painterResource(id = step.maneuver.iconRes()),
                        contentDescription = null
                    )
                },
                headlineContent = { Text(text = step.instruction) },
                supportingContent = {
                    Text(text = "${formatDistance(step.distance)} - poziom ${step.level}")
                },
                trailingContent = {
                    IconButton(onClick = { onStepClick(index) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_up),
                            contentDescription = "Pokaż na mapie"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun CalculatingRoutePanel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(size = 20.dp))
        Text(text = "Wyznaczanie trasy...")
    }
}

@Composable
fun ArrivedPanel(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = "Dotarłeś do celu",
            style = MaterialTheme.typography.titleMedium
        )

        Button(onClick = onClose) {
            Text(text = "Zamknij")
        }
    }
}

internal fun Maneuver.iconRes(): Int = when (this) {
    Maneuver.TURN_LEFT, Maneuver.SLIGHT_LEFT, Maneuver.SHARP_LEFT -> R.drawable.marker_left
    Maneuver.TURN_RIGHT, Maneuver.SLIGHT_RIGHT, Maneuver.SHARP_RIGHT -> R.drawable.marker_right
    Maneuver.LEVEL_UP -> R.drawable.marker_green_up
    Maneuver.LEVEL_DOWN -> R.drawable.marker_green_down
    Maneuver.U_TURN -> R.drawable.marker_down
    Maneuver.ARRIVE -> R.drawable.marker_flag
    else -> R.drawable.marker_up
}

internal fun formatDistance(distance: Float): String {
    val rounded = distance.roundToInt()
    return if (rounded >= 1000) {
        "${"%.1f".format(rounded / 1000f)} tys. kratek"
    } else {
        "$rounded kratek"
    }
}

internal fun formatDuration(seconds: Float): String {
    val totalMinutes = (seconds / 60f).roundToInt()
    return when {
        totalMinutes < 1 -> "< 1 min"
        totalMinutes < 60 -> "$totalMinutes min"
        else -> "${totalMinutes / 60} godz. ${totalMinutes % 60} min"
    }
}

@Preview
@Composable
private fun ArrivedPanelPreview() {
    ArrivedPanel(
        modifier = Modifier.background(color = Color.White),
        onClose = {}
    )
}

