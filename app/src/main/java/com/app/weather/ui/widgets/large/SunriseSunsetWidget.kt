package com.app.weather.ui.widgets.large

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData

@Composable
fun SunriseSunsetWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    val sunriseStr = if (data.sunriseEpoch != null) epochToTimeStr(data.sunriseEpoch) else "--"
    val sunsetStr  = if (data.sunsetEpoch  != null) epochToTimeStr(data.sunsetEpoch)  else "--"
    val nowMs = System.currentTimeMillis() / 1000L
    val sunProgress = if (data.sunriseEpoch != null && data.sunsetEpoch != null && nowMs in data.sunriseEpoch..data.sunsetEpoch) {
        ((nowMs - data.sunriseEpoch).toFloat() / (data.sunsetEpoch - data.sunriseEpoch).toFloat()).coerceIn(0f, 1f)
    } else if (data.sunriseEpoch != null && nowMs < data.sunriseEpoch) 0f else 1f

    FullWidgetBox(icon = Icons.Default.WbSunny, label = "Sunrise & Sunset", widgetBg = widgetBg, contentColor = contentColor) {
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val w = size.width; val h = size.height; val arcLeft = 16.dp.toPx(); val arcRight = w - 16.dp.toPx(); val arcTop = 8.dp.toPx()
            var x = arcLeft; while (x < arcRight) { drawLine(contentColor.copy(alpha = 0.25f), Offset(x, h - 20.dp.toPx()), Offset(x + 6.dp.toPx(), h - 20.dp.toPx()), strokeWidth = 1.5f); x += 10.dp.toPx() }
            drawPath(Path().apply { moveTo(arcLeft, h - 20.dp.toPx()); cubicTo(arcLeft, arcTop, arcRight, arcTop, arcRight, h - 20.dp.toPx()) }, color = contentColor.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
            val sunX = arcLeft + (arcRight - arcLeft) * sunProgress
            val sunYSimple = h - 20.dp.toPx() - (4 * sunProgress * (1 - sunProgress)) * (h - arcTop - 20.dp.toPx())
            drawCircle(contentColor, radius = 8.dp.toPx(), center = Offset(sunX, sunYSimple)); drawCircle(Color(0xFFFFECB3), radius = 5.dp.toPx(), center = Offset(sunX, sunYSimple))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("SUNRISE", color = secondaryContentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Text(sunriseStr, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SUNSET", color = secondaryContentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Text(sunsetStr, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun epochToTimeStr(epoch: Long): String {
    return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(epoch * 1000))
}
