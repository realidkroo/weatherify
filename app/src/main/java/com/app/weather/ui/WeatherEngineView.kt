package com.app.weather.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

// ─── Why TextureView instead of SurfaceView ───────────────────────────────────
//
// SurfaceView (the old approach) creates an independent Surface that lives
// OUTSIDE the normal View/Compose layer tree. Android composites it separately
// at the display-hardware level, which means:
//   • setZOrderMediaOverlay(true) causes it to punch ABOVE all Compose content
//   • WebView's internal SurfaceView does the same — they fight each other
//   • GlassPillBackground's RenderNode snapshot cannot capture it
//
// TextureView renders into an OpenGL texture that IS part of the normal
// hardware-accelerated View layer. It composites with everything else normally,
// meaning:
//   • No z-order punching
//   • Glass blur / RenderNode captures work correctly
//   • WebViews and other surfaces stack properly
//
// Trade-off: TextureView disables the display's direct scanout path, so there
// is a small extra GPU composite step. For a weather background this is fine —
// the perf hit is negligible and only matters for very high-frequency rendering
// like games at 120fps. Your Rust engine will still render via the Surface it
// receives from onSurfaceTextureAvailable.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WeatherEngineView(modifier: Modifier = Modifier, scrollOffset: Float = 0f) {
    val context = LocalContext.current
    val engineView = remember { EngineTextureView(context) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { engineView },
        update = { view ->
            view.updateScroll(scrollOffset)
        }
    )
}

class EngineTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private var engineSurface: Surface? = null

    init {
        surfaceTextureListener = this
        // TextureView is always hardware-accelerated and composites in the normal layer.
        // No setZOrderMediaOverlay needed — that was the SurfaceView footgun.
        isOpaque = false // allow Compose content underneath to show through if needed
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        engineSurface = Surface(surfaceTexture)
        RustEngineBridge.safeInitEngine(engineSurface!!)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        // Forward resize to engine if you implement RustEngineBridge.resize() later
        // RustEngineBridge.safeResize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        // Return true to let the system release the SurfaceTexture.
        // Tear down engine resources here when you implement destroyEngine().
        // RustEngineBridge.safeDestroyEngine()
        engineSurface?.release()
        engineSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // Called every frame after the texture is updated — no action needed.
    }

    fun updateScroll(offset: Float) {
        RustEngineBridge.safeSetScrollOffset(offset)
    }
}
