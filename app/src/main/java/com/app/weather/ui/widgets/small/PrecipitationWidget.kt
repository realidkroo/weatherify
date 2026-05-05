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
import java.util.Locale

@Composable
fun PrecipitationWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.WaterDrop, "Precipitation", contentColor)
            Spacer(Modifier.height(8.dp))
            val precipMm = data.rainfallNext24h ?: 0.0
            Text("${String.format(Locale.US, "%.1f", precipMm)} mm", color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("expected today", color = secondaryContentColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(((precipMm / 50.0).coerceIn(0.0, 1.0).toFloat()).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(Color(0xFF81D4FA)))
            }
        }
    }
}
