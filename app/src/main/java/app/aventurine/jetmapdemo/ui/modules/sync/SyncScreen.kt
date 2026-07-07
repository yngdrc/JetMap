package app.aventurine.jetmapdemo.ui.modules.sync

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aventurine.jetmapdemo.data.models.config.entities.MapConfigLocalEntity
import app.aventurine.jetmapdemo.data.services.sync.SyncState
import app.aventurine.jetmapdemo.ui.modules.main.MainActivity

@Composable
fun SyncScreen(
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val syncState by syncViewModel.syncStateFlow.collectAsStateWithLifecycle()
    val mapConfig by syncViewModel.mapConfigFlow.collectAsStateWithLifecycle(
        initialValue = MapConfigLocalEntity.Empty
    )

    LaunchedEffect(key1 = syncState, mapConfig) {
        if (syncState !is SyncState.Completed) {
            return@LaunchedEffect
        }

        if (mapConfig !is MapConfigLocalEntity.Default) {
            return@LaunchedEffect
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("mapConfig", mapConfig)
        }

        return@LaunchedEffect context.startActivity(intent)
    }

    SyncScreenContent(
        syncState = syncState,
        onRetry = { syncViewModel.sync() }
    )
}

@Composable
fun SyncScreenContent(
    syncState: SyncState,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (syncState.progress != null) {
                LinearProgressIndicator(
                    progress = { syncState.progress }
                )
            }

            Text(text = syncState.text, color = MaterialTheme.colorScheme.onBackground)
            if (syncState is SyncState.Failed) {
                Button(onClick = onRetry) {
                    Text(text = "Retry")
                }
            }
        }
    }
}

@Preview
@Composable
fun SyncScreenInitialPreview() {
    SyncScreenContent(syncState = SyncState.Initial)
}

@Preview
@Composable
fun SyncScreenInProgressPreview() {
    SyncScreenContent(syncState = SyncState.InProgress(progress = 0.5f))
}

@Preview
@Composable
fun SyncScreenCompletedPreview() {
    SyncScreenContent(syncState = SyncState.Completed)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SyncScreenFailedPreview() {
    SyncScreenContent(syncState = SyncState.Failed(error = Exception()))
}