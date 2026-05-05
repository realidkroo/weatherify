package com.app.weather.ui.widgets.large

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.WeatherData
import com.app.weather.ui.WeatherType

@Composable
fun HourlyForecastWidget(
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

    FullWidgetBox(icon = Icons.Default.WatchLater, label = "Hourly forecast", widgetBg = widgetBg, contentColor = contentColor) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            data.hourlyForecast.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(mapToIcon(item.type), contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(item.temp, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(item.time, color = secondaryContentColor, fontSize = 11.sp)
                }
            }
        }
    }
}
