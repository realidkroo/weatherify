package com.app.weather.ui.widgets.small

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData
import java.util.Locale

@Composable
fun UVIndexWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.WbSunny, "UV Index", contentColor)
            Spacer(Modifier.height(8.dp))
            val uv = data.uvIndex
            Text(if (uv != null) String.format(Locale.US, "%.1f", uv) else "__", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(when { uv == null -> "--"; uv < 3 -> "Low"; uv < 6 -> "Moderate"; uv < 8 -> "High"; uv < 11 -> "Very High"; else -> "Extreme" }, color = secondaryContentColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (uv != null) {
                val uvColor = when { uv < 3 -> Color(0xFF66BB6A); uv < 6 -> Color(0xFFFFEE58); uv < 8 -> Color(0xFFFFA726); uv < 11 -> Color(0xFFEF5350); else -> Color(0xFFAB47BC) }
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(((uv / 12.0).coerceIn(0.0, 1.0).toFloat()).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(uvColor))
                }
            }
        }
    }
}
