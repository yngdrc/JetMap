package app.aventurine.jetmapdemo.ui.modules.main.composables.bottomSheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState
import app.aventurine.jetmapdemo.utils.getIconDrawableRes

@Composable
fun BottomSheetContent(
    uiState: MainBottomSheetUIState,
    onNavigate: () -> Unit,
    onClose: () -> Unit
) {
    when (uiState) {
        is MainBottomSheetUIState.Initial -> BottomSheetInitialContent()
        is MainBottomSheetUIState.MarkerDetails -> BottomSheetMarkerDetailsContent(
            markerDescriptor = uiState.markerDescriptor,
            onNavigate = onNavigate,
            onClose = onClose
        )

        is MainBottomSheetUIState.Navigation -> BottomSheetNavigationContent(
            startMarker = uiState.startMarkerDescriptor,
            endMarker = uiState.endMarkerDescriptor,
            onClose = onClose
        )
    }
}

@Composable
fun BottomSheetInitialContent() {
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
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(
                insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomSheetMarkerDetailsRow(markerDescriptor = markerDescriptor)

        Row {
            IconButton(
                onClick = onNavigate
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_directions),
                    contentDescription = "Navigate"
                )
            }

            IconButton(
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close"
                )
            }
        }
    }
}

@Composable
fun BottomSheetNavigationContent(
    startMarker: MarkerDescriptor?,
    endMarker: MarkerDescriptor,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(
                insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            startMarker?.let {
                BottomSheetMarkerDetailsRow(markerDescriptor = it)
            }

            BottomSheetMarkerDetailsRow(markerDescriptor = endMarker)
        }

        IconButton(
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Close"
            )
        }
    }
}

class BottomSheetContentPreviewParameterProvider :
    PreviewParameterProvider<MainBottomSheetUIState> {
    override val values = sequenceOf(
        MainBottomSheetUIState.Initial,
        MainBottomSheetUIState.MarkerDetails(
            markerDescriptor = MarkerDescriptor(
                x = 0,
                y = 0,
                z = 0,
                iconId = R.drawable.ic_map_player,
                description = "Marker Name"
            ),
        ),
        MainBottomSheetUIState.Navigation(
            startMarkerDescriptor = MarkerDescriptor(
                x = 0,
                y = 0,
                z = 0,
                iconId = R.drawable.ic_map_player,
                description = "Start Marker"
            ),
            endMarkerDescriptor = MarkerDescriptor(
                x = 10,
                y = 10,
                z = 0,
                iconId = R.drawable.ic_map_player,
                description = "End Marker"
            )
        )
    )
}

@Preview
@Composable
fun BottomSheetContentPreview(
    @PreviewParameter(provider = BottomSheetContentPreviewParameterProvider::class)
    uiState: MainBottomSheetUIState
) {
    BottomSheetContent(
        uiState = uiState,
        onNavigate = {},
        onClose = {}
    )
}