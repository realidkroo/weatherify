package com.app.weather.ui.widgets.large

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.weather.ui.WeatherData

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapWidget(
    data: WeatherData
) {
    val lat = data.lat
    val lon = data.lon

    if (lat == null || lon == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Waiting for location...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val mapHtml = remember(lat, lon) {
        "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"><link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/><script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script><style>body{margin:0;padding:0}#map{width:100%;height:100vh;}</style></head><body><div id=\"map\"></div><script>var map = L.map('map', {zoomControl:false, attributionControl:false, dragging:false, scrollWheelZoom:false, touchZoom:false, doubleClickZoom:false}).setView([$lat,$lon], 10);L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png').addTo(map);L.circleMarker([$lat,$lon], {radius:6, color:'#fff', fillColor:'#4fc3f7', fillOpacity:1, weight:2}).addTo(map);</script></body></html>"
    }

    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
        AndroidView(
            factory = { ctx ->
                try {
                    WebView(ctx).apply {
                        this.settings.javaScriptEnabled = true
                        this.settings.domStorageEnabled = true
                        this.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webViewClient = WebViewClient()
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadDataWithBaseURL("https://openstreetmap.org", mapHtml, "text/html", "utf-8", null)
                    }
                } catch (_: Exception) {
                    // Fallback: return an empty view if WebView fails
                    android.view.View(ctx)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${data.location.uppercase()} MAP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
    }
}
