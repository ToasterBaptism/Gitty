package com.example.vrtheater.vr

import android.opengl.GLES11Ext
import android.opengl.GLES20

class VRRenderer {
    private var program = 0
    private var uOes = 0
    private var uEyeOffset = 0
    private var uDistort = 0
    private var uScreenScale = 0
    private var uScreenTilt = 0
    private var uYawPitch = 0
    private var uTexMatrix = 0

    private var eyeSeparation: Float = 0.03f
    private var k1: Float = 0.22f
    private var k2: Float = 0.24f
    private var screenScale: Float = 1.0f
    private var screenTilt: Float = 0.0f
    private var yaw: Float = 0.0f
    private var pitch: Float = 0.0f

    private val vertices = floatArrayOf(
        // x, y, u, v
        -1f, -1f, 0f, 1f,
         0f, -1f, 1f, 1f,
        -1f,  1f, 0f, 0f,
         0f,  1f, 1f, 0f,

         0f, -1f, 0f, 1f,
         1f, -1f, 1f, 1f,
         0f,  1f, 0f, 0f,
         1f,  1f, 1f, 0f,
    )
    private val indices = shortArrayOf(0,1,2,1,3,2, 4,5,6,5,7,6)

    private var vbo = 0
    private var ibo = 0

    fun init() {
        program = linkProgram(VS, FS)
        uOes = GLES20.glGetUniformLocation(program, "uOes")
        uEyeOffset = GLES20.glGetUniformLocation(program, "uEyeOffset")
        uDistort = GLES20.glGetUniformLocation(program, "uDistort")
        uScreenScale = GLES20.glGetUniformLocation(program, "uScreenScale")
        uScreenTilt = GLES20.glGetUniformLocation(program, "uScreenTilt")
        uYawPitch = GLES20.glGetUniformLocation(program, "uYawPitch")
        uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
        vbo = createBuffer(vertices)
        ibo = createIndexBuffer(indices)
    }

    fun onSurfaceChanged(w: Int, h: Int) { }

    fun setCalibration(eyeSeparation: Float, k1: Float, k2: Float, screenScale: Float, screenTilt: Float) {
        this.eyeSeparation = eyeSeparation
        this.k1 = k1
        this.k2 = k2
        this.screenScale = screenScale
        this.screenTilt = screenTilt
    }

    fun setHeadOrientation(yaw: Float, pitch: Float) {
        this.yaw = yaw
        this.pitch = pitch
    }

    fun draw(oesTex: Int, st: FloatArray) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearColor(0.04f, 0.05f, 0.08f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)

        val aPos = GLES20.glGetAttribLocation(program, "aPos")
        val aUv = GLES20.glGetAttribLocation(program, "aUv")
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, 0)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 16, 8)

        // Bind external texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex)
        GLES20.glUniform1i(uOes, 0)

        // Pass SurfaceTexture transform
        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, st, 0)

        GLES20.glUniform2f(uYawPitch, yaw, pitch)
        GLES20.glUniform1f(uScreenScale, screenScale)
        GLES20.glUniform1f(uScreenTilt, screenTilt)
        GLES20.glUniform2f(uDistort, k1, k2)

        // Left eye
        GLES20.glUniform1f(uEyeOffset, -eyeSeparation)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 0)

        // Right eye
        GLES20.glUniform1f(uEyeOffset, +eyeSeparation)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 12)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun createBuffer(data: FloatArray): Int {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        val bb = java.nio.ByteBuffer.allocateDirect(data.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        bb.put(data).position(0)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, bb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return buffers[0]
    }

    private fun createIndexBuffer(data: ShortArray): Int {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[0])
        val bb = java.nio.ByteBuffer.allocateDirect(data.size * 2).order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
        bb.put(data).position(0)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, data.size * 2, bb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        return buffers[0]
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val status = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            throw RuntimeException("Shader compile error: $info")
        }
        return s
    }

    private fun linkProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val info = GLES20.glGetProgramInfoLog(p)
            GLES20.glDeleteProgram(p)
            throw RuntimeException("Program link error: $info")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }

    companion object {
        private const val VS = """
            attribute vec2 aPos; 
            attribute vec2 aUv; 
            varying vec2 vUv; 
            uniform float uEyeOffset; 
            uniform float uScreenScale; 
            uniform float uScreenTilt; 
            uniform vec2 uYawPitch; 
            uniform mat4 uTexMatrix; 
            void main(){ 
                vUv = aUv; 
                float tilt = uScreenTilt; 
                vec2 pos = vec2(aPos.x + uEyeOffset, aPos.y); 
                // Apply simple head-driven offset and scale
                pos.x += uYawPitch.x * 0.2; 
                pos.y += uYawPitch.y * 0.2; 
                float cs = uScreenScale; 
                pos *= cs; 
                pos.y += tilt; 
                gl_Position = vec4(pos, 0.0, 1.0); 
            }
        """

        private const val FS = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float; 
            varying vec2 vUv; 
            uniform samplerExternalOES uOes; 
            uniform vec2 uDistort; 
            uniform mat4 uTexMatrix; 
            void main(){ 
                vec2 uv = vUv * 2.0 - 1.0; 
                float r2 = dot(uv, uv); 
                float factor = 1.0 + uDistort.x * r2 + uDistort.y * r2*r2; 
                vec2 barrel = uv * factor; 
                vec2 tuv = (barrel + 1.0) * 0.5; 
                // Apply SurfaceTexture transform matrix to tuv
                vec4 tex = uTexMatrix * vec4(tuv, 0.0, 1.0); 
                vec2 texCoord = tex.xy; 
                // Clamp instead of discard to avoid black borders on some devices
                texCoord = clamp(texCoord, 0.001, 0.999);
                gl_FragColor = texture2D(uOes, texCoord); 
            }
        """
    }
}