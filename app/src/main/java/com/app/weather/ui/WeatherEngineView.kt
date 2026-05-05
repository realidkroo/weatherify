package com.app.weather.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WeatherEngineView(modifier: Modifier = Modifier, scrollOffset: Float = 0f) {
    val context = LocalContext.current
    val engineView = remember { EngineSurfaceView(context) }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { engineView },
        update = { view ->
            view.updateScroll(scrollOffset)
        }
    )
}

class EngineSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
        // Ensure the surface is treated as a media layer for better performance
        holder.setFormat(PixelFormat.RGBA_8888)
        setZOrderMediaOverlay(true)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Native call to initialize Vulkan/WGPU with this Surface
        RustEngineBridge.safeInitEngine(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Native call to resize swapchain
        // RustEngineBridge.resize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Native call to tear down engine resources
        // RustEngineBridge.destroyEngine()
    }

    fun updateScroll(offset: Float) {
        RustEngineBridge.safeSetScrollOffset(offset)
    }
}
