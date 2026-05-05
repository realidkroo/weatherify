package com.app.weather.ui.widgets.small

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WidgetLabel(icon: ImageVector, label: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label.uppercase(), color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}
