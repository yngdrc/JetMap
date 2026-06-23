package app.aventurine.jetmapdemo.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import app.aventurine.jetmap.provider.AssetTileProvider
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmapdemo.ui.viewModels.MainViewModel
import app.aventurine.jetmapdemo.ui.theme.JetMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel
        enableEdgeToEdge()
        setContent {
            JetMapTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    JetMap(
                        modifier = Modifier.padding(paddingValues = innerPadding),
                        tileProvider = AssetTileProvider(tileSize = 256, assetManager = assets),
                        markerProvider = mainViewModel.markerProvider,
                        config = mainViewModel.jetMapConfig,
                        resources = resources
                    )
                }
            }
        }
    }
}