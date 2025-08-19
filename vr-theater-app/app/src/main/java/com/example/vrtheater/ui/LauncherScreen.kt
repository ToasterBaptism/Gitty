package com.example.vrtheater.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vrtheater.launcher.GameScanner
import com.example.vrtheater.vr.ControllerMonitor

@Composable
fun LauncherScreen(
    context: Context,
    games: List<GameScanner.AppInfo>,
    controllers: ControllerMonitor,
    onStartProjection: () -> Unit,
    onStopProjection: () -> Unit,
    projectionRunning: Boolean
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("VR Theater", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartProjection, enabled = !projectionRunning) { Text("Start Projection") }
            Button(onClick = onStopProjection, enabled = projectionRunning) { Text("Stop Projection") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { context.startActivity(android.content.Intent(context, com.example.vrtheater.MainActivity::class.java).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); putExtra("show_settings", true) }) }) { Text("Settings") }
        }
        Text("Controllers: ${controllers.connectedControllers.size}")
        Text("Installed Games")
        LazyColumn(Modifier.weight(1f)) {
            items(games.size) { idx ->
                val app = games[idx]
                ListItem(
                    headlineContent = { Text(app.appLabel) },
                    supportingContent = { Text(app.packageName) },
                    trailingContent = {
                        Button(onClick = { GameScanner.launchApp(context, app.packageName) }) { Text("Launch") }
                    }
                )
                Divider()
            }
        }
    }
}