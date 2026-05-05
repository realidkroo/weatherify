package com.app.weather.ui.widgets.small

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WindWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WidgetLabel(Icons.Default.NorthEast, "Wind", contentColor)
            Spacer(Modifier.height(6.dp))
            Canvas(modifier = Modifier.size(70.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 4.dp.toPx()
                drawCircle(color = contentColor.copy(alpha = 0.15f), radius = r)
                for (d in 0..315 step 45) {
                    val rd = Math.toRadians(d.toDouble())
                    drawCircle(color = contentColor.copy(alpha = 0.4f), radius = 2.dp.toPx(), center = Offset(cx + (r - 6.dp.toPx()) * sin(rd).toFloat(), cy - (r - 6.dp.toPx()) * cos(rd).toFloat()))
                }
                val rad = Math.toRadians((data.windDeg ?: 0).toDouble())
                val tip = Offset(cx + (r - 8.dp.toPx()) * sin(rad).toFloat(), cy - (r - 8.dp.toPx()) * cos(rad).toFloat())
                drawLine(contentColor, Offset(cx - 20.dp.toPx() * sin(rad).toFloat(), cy + 20.dp.toPx() * cos(rad).toFloat()), tip, strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF4FC3F7), radius = 4.dp.toPx(), center = tip)
            }
            Text(data.wind, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (data.windGust != "--") Text("Gust: ${data.windGust}", color = secondaryContentColor, fontSize = 11.sp)
        }
    }
}
