package com.app.weather.ui.widgets.small

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
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
fun AveragesWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.Thermostat, "Averages", contentColor)
            Spacer(Modifier.height(8.dp))
            val currentTemp = data.temp
            val averageTemp = 28 // historical average
            
            if (currentTemp == null) {
                Text("--°", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text("Waiting for data", color = secondaryContentColor, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Daily average is $averageTemp°", color = secondaryContentColor, fontSize = 12.sp)
            } else {
                val diff = currentTemp - averageTemp
                val diffText = if (diff == 0) "0°" else if (diff > 0) "+$diff°" else "$diff°"
                Text(diffText, color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(if (diff == 0) "Same as historical" else if (diff > 0) "Above historical average" else "Below historical average", color = secondaryContentColor, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Daily average is $averageTemp°", color = secondaryContentColor, fontSize = 12.sp)
            }
        }
    }
}
