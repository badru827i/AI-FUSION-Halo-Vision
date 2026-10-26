package com.aifusion.halovision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException

class MainActivity : ComponentActivity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var statusView: TextView
    private lateinit var renderer: HaloRenderer
    private var arSession: Session? = null
    private var installRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
        }
        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(28, 24, 28, 24)
            setBackgroundColor(0xB8071526.toInt())
            text = "HALO VISION  •  Initialising"
        }
        renderer = HaloRenderer { message ->
            runOnUiThread { if (!isFinishing) statusView.text = message }
        }
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(glView, FrameLayout.LayoutParams(-1, -1))
            addView(statusView, FrameLayout.LayoutParams(-1, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
            })
        }
        setContentView(root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            statusView.text = "HALO VISION  •  Camera permission granted"
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            resumeArSession()
        }
        glView.onResume()
    }

    private fun resumeArSession() {
        try {
            if (arSession == null) {
                if (!installRequested) {
                    val installStatus = com.google.ar.core.ArCoreApk.getInstance()
                        .requestInstall(this, !installRequested)
                    if (installStatus == com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                        installRequested = true
                        return
                    }
                }
                arSession = Session(this)
            }

            val session = arSession ?: return
            val config = Config(session)
            config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }
            session.configure(config)
            renderer.setSession(session)
            session.resume()
            statusView.text = if (config.depthMode == Config.DepthMode.AUTOMATIC) {
                "HALO VISION  •  ARCore ready · Depth enabled"
            } else {
                "HALO VISION  •  ARCore ready · Depth unavailable"
            }
        } catch (e: UnavailableException) {
            statusView.text = "ARCore unavailable: ${e.javaClass.simpleName}"
        } catch (e: CameraNotAvailableException) {
            statusView.text = "Camera unavailable. Close other camera apps and retry."
        } catch (e: Exception) {
            statusView.text = "Startup error: ${e.javaClass.simpleName}"
        }
    }

    override fun onPause() {
        glView.onPause()
        try { arSession?.pause() } catch (_: Exception) { }
        super.onPause()
    }

    override fun onDestroy() {
        arSession?.close()
        arSession = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            resumeArSession()
        } else if (requestCode == CAMERA_REQUEST) {
            statusView.text = "Camera permission is required to scan a room."
        }
    }

    companion object {
        private const val CAMERA_REQUEST = 1001
    }
}
