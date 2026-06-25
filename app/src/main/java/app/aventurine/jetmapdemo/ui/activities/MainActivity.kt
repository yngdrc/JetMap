package app.aventurine.jetmapdemo.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmapdemo.ui.viewModels.MainViewModel
import app.aventurine.jetmapdemo.ui.theme.JetMapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetMapTheme {
                val focusedMarkerState by mainViewModel.jetMapState.gestureApi.focusedMarker
                val sheetState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        skipHiddenState = false
                    )
                )

                LaunchedEffect(focusedMarkerState) {
                    when {
                        focusedMarkerState != null -> sheetState.bottomSheetState.expand()
                        else -> sheetState.bottomSheetState.hide()
                    }
                }

                BottomSheetScaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                    sheetContent = {
                        focusedMarkerState?.let { focusedMarker ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .windowInsetsPadding(
                                        WindowInsets.safeDrawing
                                            .only(WindowInsetsSides.Bottom)
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = focusedMarker.second)
                                }

                                IconButton(
                                    onClick = {
                                        mainViewModel.jetMapState.gestureApi.clear()
                                        mainViewModel.jetMapState.pathfindingController.clear()
                                    }
                                ) {
                                    Text(text = "Close")
                                }
                            }
                        }
                    },
                    scaffoldState = sheetState
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        JetMap(
                            jetMapState = mainViewModel.jetMapState,
                            myLocationResId = app.aventurine.jetmapdemo.R.drawable.ic_map_player,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    mainViewModel.jetMapState.motionApi.changeLevel { currentLevel ->
                                        currentLevel - 1
                                    }
                                }
                            ) {
                                Text(text = "+")
                            }

                            SmallFloatingActionButton(
                                onClick = {
                                    mainViewModel.jetMapState.motionApi.changeLevel { currentLevel ->
                                        currentLevel + 1
                                    }
                                }
                            ) {
                                Text(text = "-")
                            }
                        }
                    }
                }
            }
        }
    }
}