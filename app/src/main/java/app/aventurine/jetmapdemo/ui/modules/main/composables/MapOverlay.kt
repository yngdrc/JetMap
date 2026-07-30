package app.aventurine.jetmapdemo.ui.modules.main.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.utils.getIconDrawableRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapOverlay(
    modifier: Modifier = Modifier,
    query: String,
    searchResults: List<MarkerEntity>,
    currentLevel: Int,
    onChangeLevel: (Int) -> Unit,
    onToggleMarkers: () -> Unit,
    onToggleTerrainType: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchResultTap: (MarkerEntity) -> Unit
) {
    val paddingValues = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter),
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    trailingIcon = {
                        if (query.isNotEmpty() || expanded) {
                            IconButton(
                                onClick = {
                                    expanded = false
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Close"
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(text = "Search...")
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            LazyColumn {
                items(searchResults) { searchResult ->
                    ListItem(
                        modifier = Modifier.pointerInput(searchResult) {
                            detectTapGestures(
                                onTap = {
                                    onSearchResultTap(searchResult)
                                    expanded = false
                                }
                            )
                        },
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                    getIconDrawableRes(iconId = searchResult.iconId)
                                    ?.let { iconDrawableRes ->
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(id = iconDrawableRes),
                                        contentDescription = "Marker",
                                        tint = null
                                    )
                                }

                                Text(
                                    text = searchResult.description.ifEmpty {
                                        "${searchResult.x}, ${searchResult.y}"
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        Column(
            modifier = modifier
                .padding(
                    start = max(paddingValues.calculateRightPadding(layoutDirection), 16.dp),
                    top = max(paddingValues.calculateTopPadding(), 16.dp),
                    end = max(paddingValues.calculateRightPadding(layoutDirection), 16.dp),
                    bottom = max(paddingValues.calculateBottomPadding(), 16.dp),
                )
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = onToggleMarkers
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Toggle markers"
                )
            }

            SmallFloatingActionButton(
                onClick = {
                    onChangeLevel(currentLevel - 1)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_up),
                    contentDescription = "Up"
                )
            }

            SmallFloatingActionButton(
                onClick = {
                    onChangeLevel(currentLevel + 1)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_down),
                    contentDescription = "Down"
                )
            }

            SmallFloatingActionButton(
                onClick = onToggleTerrainType
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_map),
                    contentDescription = "Toggle terrain type"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MapOverlayPreview() {
    MapOverlay(
        currentLevel = 7,
        query = "",
        searchResults = emptyList(),
        onChangeLevel = {},
        onQueryChange = {},
        onSearch = {},
        onSearchResultTap = {},
        onToggleMarkers = {},
        onToggleTerrainType = {}
    )
}