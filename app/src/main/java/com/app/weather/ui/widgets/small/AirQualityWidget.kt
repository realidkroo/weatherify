package com.app.weather.ui.widgets.small

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        if (data.aqiValue == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("..", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text("AQI", color = secondaryContentColor, fontSize = 12.sp)
                }
            }
        } else {
            Column {
                WidgetLabel(Icons.Default.FilterDrama, "Air Quality", contentColor)
                Spacer(Modifier.height(8.dp))
                val aqi = data.aqiValue
                val aqiLevel = when {
                    aqi <= 50 -> 1
                    aqi <= 100 -> 2
                    aqi <= 150 -> 3
                    aqi <= 200 -> 4
                    aqi <= 300 -> 5
                    else -> 6
                }
                
                val status = when {
                    aqi <= 50 -> "Good"
                    aqi <= 100 -> "Moderate"
                    aqi <= 150 -> "Sensitive"
                    aqi <= 200 -> "Unhealthy"
                    aqi <= 300 -> "Very Unhealthy"
                    else -> "Hazardous"
                }
                
                Text(aqi.toString(), color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("AQI ($status)", color = secondaryContentColor, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        Color(0xFF009966), // Good
                        Color(0xFFFFDE33), // Moderate
                        Color(0xFFFF9933), // USG
                        Color(0xFFCC0033), // Unhealthy
                        Color(0xFF660099), // Very Unhealthy
                        Color(0xFF7E0023)  // Hazardous
                    ).forEachIndexed { i, c ->
                        Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(if (i < aqiLevel) c else contentColor.copy(alpha = 0.2f)))
                    }
                }
            }
        }
    }
}
