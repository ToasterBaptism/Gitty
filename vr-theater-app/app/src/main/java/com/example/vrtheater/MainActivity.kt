package com.example.vrtheater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.core.content.getSystemService
import androidx.activity.compose.BackHandler
import com.example.vrtheater.launcher.GameScanner
import com.example.vrtheater.settings.SettingsRepository
import com.example.vrtheater.ui.LauncherScreen
import com.example.vrtheater.ui.SettingsScreen
import com.example.vrtheater.vr.ControllerMonitor
import com.example.vrtheater.vr.OverlayService

class MainActivity : ComponentActivity() {

    private val mediaProjectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK && res.data != null) {
            OverlayService.start(this, res.resultCode, res.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel()
        val repo = SettingsRepository(this)
        val controllerMonitor = com.example.vrtheater.vr.ControllerMonitor(this)
        controllerMonitor.start()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val games by remember { mutableStateOf(GameScanner.scanInstalledGames(this)) }
                var projectionRunning by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(intent?.getBooleanExtra("show_settings", false) == true) }

                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(repo)
                    LaunchedEffect(Unit) { intent?.removeExtra("show_settings") }
                } else {
                    LauncherScreen(
                        context = this,
                        games = games,
                        controllers = controllerMonitor,
                        onStartProjection = {
                            ensureOverlayPermission()
                            requestMediaProjection()
                            projectionRunning = true
                        },
                        onStopProjection = {
                            OverlayService.stop(this)
                            projectionRunning = false
                        },
                        projectionRunning = projectionRunning
                    )
                }
            }
        }
    }

    private fun ensureOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun requestMediaProjection() {
        val mpm: MediaProjectionManager? = getSystemService()
        val intent = mpm?.createScreenCaptureIntent() ?: return
        mediaProjectionLauncher.launch(intent)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(OverlayService.CHANNEL_ID, "VR Projection", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(ch)
        }
    }
}