package com.app.weather.ui.widgets.large

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    FullWidgetBox(icon = Icons.Default.WaterDrop, label = "Rainfall", widgetBg = widgetBg, contentColor = contentColor) {
        Text("${String.format(Locale.US, "%.0f", data.rainfallLast24h ?: 0.0)} mm", color = contentColor, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("in last 24h", color = secondaryContentColor, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Text("${String.format(Locale.US, "%.0f", data.rainfallNext24h ?: 0.0)} mm expected in next 24h.", color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}
