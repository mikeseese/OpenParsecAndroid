package com.aigch.openparsec.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import com.aigch.openparsec.parsec.CParsec
import com.aigch.openparsec.settings.SettingsHandler
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES rendering surface for the Parsec streaming view.
 * Ported from iOS ParsecGLKViewController / ParsecGLKRenderer.
 *
 * Uses GLSurfaceView with a continuous render mode to drive
 * the Parsec SDK's frame rendering loop via CParsec.renderGLFrame().
 */
class ParsecGLSurfaceView(context: Context) : GLSurfaceView(context) {

    companion object {
        private const val TAG = "ParsecGLSurfaceView"
    }

    init {
        // Use OpenGL ES 3.0 (matching iOS EAGLContext .openGLES3)
        setEGLContextClientVersion(3)
        setRenderer(ParsecRenderer())
        // Render continuously for streaming (like iOS GLKViewController)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /**
     * GLSurfaceView.Renderer implementation.
     * Ported from iOS ParsecGLKRenderer.
     *
     * onDrawFrame is called each frame by the GL thread, equivalent to
     * iOS glkView(_:drawIn:) delegate callback.
     */
    private class ParsecRenderer : Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Log.d(TAG, "GL surface created")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.d(TAG, "GL surface changed: ${width}x${height}")
            // Set client frame dimensions (iOS: CParsec.setFrame in glkView drawIn)
            CParsec.setFrame(width.toFloat(), height.toFloat(), 1f)
        }

        override fun onDrawFrame(gl: GL10?) {
            // Calculate timeout based on configured frame rate
            // (ported from iOS ParsecGLKRenderer)
            val fps = SettingsHandler.preferredFramesPerSecond.let {
                if (it == 0) 60 else it
            }
            val timeout = maxOf(1000 / fps, 8)

            CParsec.renderGLFrame(timeout)
        }
    }
}
