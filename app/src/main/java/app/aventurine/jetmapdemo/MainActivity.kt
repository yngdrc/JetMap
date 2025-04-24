package app.aventurine.jetmapdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import app.aventurine.jetmap.ui.JetMap
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.ui.theme.JetMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetMapTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    JetMap(
                        modifier = Modifier.padding(innerPadding),
                        tileProvider = TileProvider(),
                        config = JetMapConfig(
                            tileSize = 256,
                            mapSize = IntSize(2560, 2048)
                        ),
                        assetManager = assets
                    )
                }
            }
        }
    }
}