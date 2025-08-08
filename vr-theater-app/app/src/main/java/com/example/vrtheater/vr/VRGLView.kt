package com.example.vrtheater.vr

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VRGLView(context: Context) : GLSurfaceView(context), GLSurfaceView.Renderer {
    private var oesTexId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null

    private lateinit var rendererImpl: VRRenderer
    private var headTracker: HeadTracker? = null

    private var pendingSettings: Settings? = null

    data class Settings(
        val eyeSeparation: Float,
        val k1: Float,
        val k2: Float,
        val screenScale: Float,
        val screenTilt: Float,
    )

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setHeadTracker(tracker: HeadTracker?) {
        headTracker = tracker
    }

    fun updateSettings(eyeSeparation: Float, k1: Float, k2: Float, screenScale: Float, screenTilt: Float) {
        pendingSettings = Settings(eyeSeparation, k1, k2, screenScale, screenTilt)
    }

    fun getInputSurface(): Surface {
        if (inputSurface == null) {
            oesTexId = createOesTexture()
            surfaceTexture = SurfaceTexture(oesTexId)
            inputSurface = Surface(surfaceTexture)
        }
        return inputSurface!!
    }

    fun attachFrameSource() {
        surfaceTexture?.setOnFrameAvailableListener { requestRender() }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        rendererImpl = VRRenderer()
        rendererImpl.init()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        rendererImpl.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture?.updateTexImage()
        val st = FloatArray(16)
        surfaceTexture?.getTransformMatrix(st)
        pendingSettings?.let {
            rendererImpl.setCalibration(it.eyeSeparation, it.k1, it.k2, it.screenScale, it.screenTilt)
            pendingSettings = null
        }
        val yaw = headTracker?.yaw ?: 0f
        val pitch = headTracker?.pitch ?: 0f
        rendererImpl.setHeadOrientation(yaw, pitch)
        rendererImpl.draw(oesTexId, st)
    }

    private fun createOesTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return tex[0]
    }
}