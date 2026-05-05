package com.app.weather.ui.widgets.large

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
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
fun RainfallWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.WaterDrop, "Rainfall", contentColor)
            Spacer(Modifier.height(8.dp))
            Text("${String.format(Locale.US, "%.0f", data.rainfallLast24h ?: 0.0)} mm", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text("in last 24h", color = secondaryContentColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("${String.format(Locale.US, "%.0f", data.rainfallNext24h ?: 0.0)} mm expected in next 24h.", color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
