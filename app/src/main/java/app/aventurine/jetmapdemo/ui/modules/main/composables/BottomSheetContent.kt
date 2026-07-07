package app.aventurine.jetmapdemo.ui.modules.main.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.utils.getIconDrawableRes

@Composable
fun BottomSheetContent(
    focusedMarker: MarkerDescriptor,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            key(focusedMarker.iconId) {
                val iconRes = try {
                    getIconDrawableRes(iconId = focusedMarker.iconId)
                } catch (e: Exception) {
                    null
                }

                if (iconRes != null) {
                    Image(
                        modifier = Modifier.size(24.dp),
                        bitmap = ImageBitmap.imageResource(
                            id = iconRes
                        ),
                        contentDescription = "Marker Icon",
                        filterQuality = FilterQuality.None
                    )
                }
            }

            Text(text = focusedMarker.description)
        }

        IconButton(
            onClick = onClose
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close"
            )
        }
    }
}

@Preview
@Composable
fun BottomSheetContentPreview() {
    BottomSheetContent(
        focusedMarker = MarkerDescriptor(
            x = 0,
            y = 0,
            z = 0,
            iconId = R.drawable.ic_map_player,
            description = "Marker Name"
        ),
        onClose = {}
    )
}