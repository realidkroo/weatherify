package com.app.weather.ui.widgets.large

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.*

// ─── Tile math ───────────────────────────────────────────────────────────────

private fun lonToTileX(lon: Double, zoom: Int): Int =
    floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()

private fun latToTileY(lat: Double, zoom: Int): Int {
    val latRad = Math.toRadians(lat)
    return floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
}

private fun tileUrl(x: Int, y: Int, zoom: Int): String {
    val subdomain = listOf("a", "b", "c", "d").random()
    return "https://$subdomain.basemaps.cartocdn.com/dark_all/$zoom/$x/$y@2x.png"
}

private suspend fun fetchStitchedMap(lat: Double, lon: Double, zoom: Int = 12): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val cx = lonToTileX(lon, zoom)
            val cy = latToTileY(lat, zoom)
            val tileSize = 256
            val outputSize = tileSize * 3
            val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val url = tileUrl(cx + dx, cy + dy, zoom)
                    val bytes = URL(url).readBytes()
                    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                    val tile = Bitmap.createScaledBitmap(raw, tileSize, tileSize, true)
                    canvas.drawBitmap(tile, ((dx + 1) * tileSize).toFloat(), ((dy + 1) * tileSize).toFloat(), null)
                    tile.recycle()
                    raw.recycle()
                }
            }
            result
        }.getOrNull()
    }

// ─── Composable ──────────────────────────────────────────────────────────────
// Bitmap is drawn via drawBehind on the already-clipped Box.
// drawBehind draws INSIDE the current clip region — so it can never escape
// the RoundedCornerShape(24.dp) clip applied to the Box.
// A separate Canvas child (even fillMaxSize) can draw outside the parent's
// clip if the canvas doesn't inherit the clip stack, which was the bug.

@Composable
fun MapWidget(data: WeatherData, modifier: Modifier = Modifier) {
    val lat = data.lat
    val lon = data.lon

    if (lat == null || lon == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .drawBehind { drawRect(Color.White.copy(alpha = 0.08f)) },
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

    var bitmap by remember(lat, lon) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(lat, lon) { mutableStateOf(true) }

    LaunchedEffect(lat, lon) {
        isLoading = true
        bitmap = fetchStitchedMap(lat, lon)
        isLoading = false
    }

    val bmp = bitmap
    val imageBitmap = remember(bmp) { bmp?.asImageBitmap() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))   // clip BEFORE drawBehind
            .drawBehind {
                // Background fill
                drawRect(Color(0xFF1A1A2E))

                val ib = imageBitmap
                if (ib != null && bmp != null) {
                    // Scale to fill the draw area (cover), centered.
                    // Both offsets can be negative (image larger than canvas) — that's fine,
                    // drawBehind is already inside the clip so it cannot escape.
                    val scaleX = size.width / bmp.width.toFloat()
                    val scaleY = size.height / bmp.height.toFloat()
                    val scale = maxOf(scaleX, scaleY)
                    val drawW = (bmp.width * scale).toInt()
                    val drawH = (bmp.height * scale).toInt()
                    val offX = ((size.width - drawW) / 2f).toInt()
                    val offY = ((size.height - drawH) / 2f).toInt()
                    drawImage(
                        image = ib,
                        dstOffset = IntOffset(offX, offY),
                        dstSize = IntSize(drawW, drawH)
                    )

                    // Location dot at center
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(Color.White, radius = 11f, center = Offset(cx, cy), style = Stroke(width = 3f))
                    drawCircle(Color(0xFF4FC3F7), radius = 6f, center = Offset(cx, cy))
                }
            }
    ) {
        // Loading / error overlays — these are Compose children, also inside the clip
        when {
            isLoading && bmp == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading map...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                }
            }
            !isLoading && bmp == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Map unavailable", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                    }
                }
            }
        }

        // Label badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind { drawRect(Color(0x99000000)) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${data.location.uppercase()} MAP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
    }
}
