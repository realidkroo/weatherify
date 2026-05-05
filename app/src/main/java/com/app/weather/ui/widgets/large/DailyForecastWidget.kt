package com.app.weather.ui.widgets.large

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData
import com.app.weather.ui.WeatherType

@Composable
fun DailyForecastWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    val mapToIcon = { t: WeatherType ->
        when (t) {
            WeatherType.Clear -> Icons.Default.WbSunny
            WeatherType.Clouds, WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke -> Icons.Default.FilterDrama
            WeatherType.Rain, WeatherType.Drizzle -> Icons.Default.WaterDrop
            WeatherType.Thunderstorm, WeatherType.Snow, WeatherType.Tornado, WeatherType.Squall -> Icons.Default.Thunderstorm
            else -> Icons.Default.FilterDrama
        }
    }

    FullWidgetBox(icon = Icons.Default.DateRange, label = "10-Day forecast", widgetBg = widgetBg, contentColor = contentColor) {
        val globalMin = data.dailyForecast.minOfOrNull { it.tempMin.replace("°", "").toIntOrNull() ?: 0 } ?: 0
        val globalMax = data.dailyForecast.maxOfOrNull { it.temp.replace("°", "").toIntOrNull() ?: 100 } ?: 100
        val tempRange = (globalMax - globalMin).coerceAtLeast(1).toFloat()

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.dailyForecast.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(item.day, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.4f))
                    Icon(mapToIcon(item.type), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.weight(0.5f))
                    Text(item.tempMin, color = secondaryContentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) {
                        val dayMin = item.tempMin.replace("°", "").toIntOrNull() ?: globalMin
                        val dayMax = item.temp.replace("°", "").toIntOrNull() ?: globalMax
                        
                        val startFrac = ((dayMin - globalMin) / tempRange).coerceIn(0f, 1f)
                        val widthFrac = ((dayMax - dayMin) / tempRange).coerceIn(0.05f, 1f)
                        
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (startFrac > 0f) Spacer(modifier = Modifier.weight(startFrac))
                            Box(modifier = Modifier.weight(widthFrac).fillMaxHeight().clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.7f)))
                            val endFrac = 1f - startFrac - widthFrac
                            if (endFrac > 0f) Spacer(modifier = Modifier.weight(endFrac))
                        }
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    Text(item.temp, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                }
            }
        }
    }
}
