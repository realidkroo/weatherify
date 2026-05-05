package com.app.weather.ui.widgets.small

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
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
fun PressureWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WidgetLabel(Icons.Default.Compress, "Pressure", contentColor)
            Spacer(Modifier.height(6.dp))
            val pHpa = data.pressure ?: 1013
            Canvas(modifier = Modifier.size(70.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 4.dp.toPx()
                for (t in 0..120 step 10) {
                    val ang = Math.toRadians(180.0 + t * 1.5)
                    drawLine(contentColor.copy(alpha = 0.3f), Offset(cx + (r - 8.dp.toPx()) * cos(ang).toFloat(), cy + (r - 8.dp.toPx()) * sin(ang).toFloat()), Offset(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat()), strokeWidth = 1.5f)
                }
                val needleAng = Math.toRadians(180.0 + ((pHpa - 950) / 100f).coerceIn(0f, 1f) * 180.0)
                drawLine(contentColor, Offset(cx, cy), Offset(cx + (r - 10.dp.toPx()) * cos(needleAng).toFloat(), cy + (r - 10.dp.toPx()) * sin(needleAng).toFloat()), strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF4FC3F7), radius = 4.dp.toPx(), center = Offset(cx, cy))
            }
            Text("$pHpa hPa", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Low", color = secondaryContentColor, fontSize = 10.sp)
                Text("High", color = secondaryContentColor, fontSize = 10.sp)
            }
        }
    }
}
