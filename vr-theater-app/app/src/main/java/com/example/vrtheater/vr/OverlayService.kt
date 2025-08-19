package com.example.vrtheater.vr

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import androidx.core.app.NotificationCompat
import com.example.vrtheater.MainActivity
import com.example.vrtheater.R
import com.example.vrtheater.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "vr_projection"
        private const val NOTIF_ID = 42
        private const val EXTRA_RESULT_CODE = "res_code"
        private const val EXTRA_RESULT_DATA = "res_data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val i = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: VRGLView? = null
    private var headTracker: HeadTracker? = null
    private var virtualDisplay: VirtualDisplay? = null

    private lateinit var repo: SettingsRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repo = SettingsRepository(this)
        headTracker = HeadTracker(this).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        setupOverlay()
        startProjectionFromIntent(intent)
        observeSettings()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("VR projection running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlay() {
        if (overlayView != null) return
        overlayView = VRGLView(this)
        overlayView?.setHeadTracker(headTracker)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(overlayView, params)
    }

    private fun startProjectionFromIntent(intent: Intent?) {
        if (intent == null) return
        val rc = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (rc == -1 || data == null) return
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mpm.getMediaProjection(rc, data)
        val metrics = resources.displayMetrics
        createVirtualDisplay(metrics, projection)
    }

    private fun createVirtualDisplay(metrics: DisplayMetrics, projection: android.media.projection.MediaProjection) {
        val view = overlayView ?: return
        // Ensure the GL side is ready before obtaining the surface
        view.setOnSurfaceReady { surface ->
            // Set default buffer size to avoid black frames on some devices
            view.setDefaultBufferSize(metrics.widthPixels, metrics.heightPixels)
            virtualDisplay = projection.createVirtualDisplay(
                "vr-theater",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
            view.attachFrameSource()
        }
    }

    private fun observeSettings() {
        val view = overlayView ?: return
        serviceScope.launch {
            repo.settings.collectLatest { s ->
                view.updateSettings(
                    eyeSeparation = s.eyeSeparation,
                    k1 = s.k1,
                    k2 = s.k2,
                    screenScale = s.screenScale,
                    screenTilt = s.screenTilt,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        headTracker?.stop()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}