package com.app.weather.ui.widgets.small

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData

@Composable
fun AirQualityWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.FilterDrama, "Air Quality", contentColor)
            Spacer(Modifier.height(8.dp))
            val aqi = data.aqiValue ?: 0
            Text(when (aqi) { 1 -> "Good"; 2 -> "Fair"; 3 -> "Moderate"; 4 -> "Poor"; 5 -> "Very Poor"; else -> "--" }, color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("AQI index $aqi/5", color = secondaryContentColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(Color(0xFF66BB6A), Color(0xFFFFEE58), Color(0xFFFFA726), Color(0xFFEF5350), Color(0xFFAB47BC)).forEachIndexed { i, c ->
                    Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(if (i < aqi) c else contentColor.copy(alpha = 0.2f)))
                }
            }
        }
    }
}
