package com.app.weather.ui.widgets.small

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

@Composable
fun HumidityWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.WaterDrop, "Humidity", contentColor)
            Spacer(Modifier.height(8.dp))
            val hVal = data.humidityValue ?: 0
            Text("${hVal}%", color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(when { hVal > 80 -> "Very humid"; hVal > 60 -> "Humid"; hVal > 40 -> "Comfortable"; else -> "Dry" }, color = secondaryContentColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((hVal / 100f).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(Color(0xFF4FC3F7)))
            }
        }
    }
}
