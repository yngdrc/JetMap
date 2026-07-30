package app.aventurine.jetmapdemo.ui.modules.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.aventurine.jetmapdemo.ui.theme.JetMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetMapTheme {
                SyncScreen()
            }
        }
    }
}