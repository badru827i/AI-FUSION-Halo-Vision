package com.aifusion.halovision

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Minimal ARCore camera background plus a diagnostic raw-depth probe. */
class HaloRenderer(private val onStatus: (String) -> Unit) : GLSurfaceView.Renderer {
    private val sessionRef = AtomicReference<Session?>(null)
    private var cameraTextureId = -1
    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var samplerHandle = 0
    private var frameCount = 0
    private var lastStatusAt = 0L

    private val vertices: FloatBuffer = ByteBuffer.allocateDirect(16 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(
                -1f, -1f, 0f, 1f,
                 1f, -1f, 1f, 1f,
                -1f,  1f, 0f, 0f,
                 1f,  1f, 1f, 0f
            ))
            position(0)
        }

    fun setSession(session: Session) {
        sessionRef.set(session)
        if (cameraTextureId >= 0) session.setCameraTextureNames(intArrayOf(cameraTextureId))
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexShader = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()
        val fragmentShader = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
            void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }
        """.trimIndent()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            val status = IntArray(1)
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) throw IllegalStateException("Shader link failed: ${GLES20.glGetProgramInfoLog(p)}")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        samplerHandle = GLES20.glGetUniformLocation(program, "uTexture")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        cameraTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        sessionRef.get()?.setCameraTextureNames(intArrayOf(cameraTextureId))
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val session = sessionRef.get() ?: return
        try {
            val frame: Frame = session.update()
            drawCamera()
            frameCount++
            val now = System.currentTimeMillis()
            if (frameCount % 15 == 0 && now - lastStatusAt > 700) {
                lastStatusAt = now
                onStatus("HALO VISION  •  Tracking: ${frame.camera.trackingState}\n${sampleDepth(frame)}")
            }
        } catch (e: NotYetAvailableException) {
            onStatus("HALO VISION  •  Depth is warming up")
        } catch (e: Exception) {
            onStatus("Scanner paused: ${e.javaClass.simpleName}")
        }
    }

    private fun sampleDepth(frame: Frame): String {
        return try {
            frame.acquireRawDepthImage16Bits().use { image ->
                val shortBuffer = image.planes[0].buffer
                    .order(ByteOrder.nativeOrder()).asShortBuffer()
                var valid = 0
                val step = (shortBuffer.limit() / 300).coerceAtLeast(1)
                var i = 0
                while (i < shortBuffer.limit()) {
                    if ((shortBuffer.get(i).toInt() and 0xFFFF) > 0) valid++
                    i += step
                }
                "Raw depth: ${image.width}×${image.height} · sampled valid: $valid"
            }
        } catch (_: NotYetAvailableException) {
            "Depth: collecting samples — move the phone slowly"
        } catch (_: Exception) {
            "Depth: unavailable in current frame"
        }
    }

    private fun drawCamera() {
        if (program == 0 || cameraTextureId < 0) return
        GLES20.glUseProgram(program)
        vertices.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertices)
        vertices.position(2)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertices)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glUniform1i(samplerHandle, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Shader compilation failed: $log")
        }
        return shader
    }
}
