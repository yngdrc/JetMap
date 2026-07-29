package app.aventurine.jetmapdemo.ui.modules.main.composables.bottomSheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmapdemo.utils.getIconDrawableRes

@Composable
fun BottomSheetMarkerDetailsRow(
    markerDescriptor: MarkerDescriptor
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        key(markerDescriptor.iconId) {
            markerDescriptor.iconId?.let { iconId ->
                Image(
                    modifier = Modifier.size(24.dp),
                    bitmap = ImageBitmap.imageResource(id = iconId),
                    contentDescription = "Marker Icon",
                    filterQuality = FilterQuality.None
                )
            }
        }

        Text(text = markerDescriptor.description)
    }
}