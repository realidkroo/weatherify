package com.app.weather.ui.widgets.small

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.R
import com.app.weather.ui.WeatherData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MoonPhaseWidget(
    data: WeatherData,
    widgetBg: Color,
    contentColor: Color,
    secondaryContentColor: Color
) {
    val phase = data.moonPhase ?: 0.0
    val phaseName = when {
        phase < 0.03 || phase > 0.97 -> "New Moon"
        phase < 0.22 -> "Waxing Crescent"
        phase < 0.28 -> "First Quarter"
        phase < 0.47 -> "Waxing Gibbous"
        phase < 0.53 -> "Full Moon"
        phase < 0.72 -> "Waning Gibbous"
        phase < 0.78 -> "Last Quarter"
        else -> "Waning Crescent"
    }

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(Icons.Default.NightsStay, "Moon Phase", contentColor)
            Spacer(Modifier.height(8.dp))
            
            Text(phaseName, color = contentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${(phase * 100).toInt()}% illuminated", color = secondaryContentColor, fontSize = 12.sp)
            
            Spacer(Modifier.weight(1f))
            
            Image(
                painter = painterResource(id = R.drawable.iconsmoon),
                contentDescription = null,
                modifier = Modifier.size(72.dp).drawWithContent {
                    drawContent()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.width / 2f

                    val illuminatedPath = Path()
                    val illumination = if (phase <= 0.5) phase * 2.0 else (1.0 - phase) * 2.0
                    val terminatorXRadius = r * Math.abs(2.0 * illumination - 1.0).toFloat()
                    val isTerminatorConvex = illumination > 0.5
                    val steps = 64
                    
                    illuminatedPath.reset()
                    if (phase <= 0.5) {
                        for (i in 0..steps) {
                            val angle = -PI / 2 + PI * i / steps
                            val px = cx + (r * cos(angle)).toFloat()
                            val py = cy + (r * sin(angle)).toFloat()
                            if (i == 0) illuminatedPath.moveTo(px, py) else illuminatedPath.lineTo(px, py)
                        }
                        for (i in steps downTo 0) {
                            val angle = -PI / 2 + PI * i / steps
                            val tx = if (isTerminatorConvex) {
                                cx - (terminatorXRadius * cos(angle)).toFloat()
                            } else {
                                cx + (terminatorXRadius * cos(angle)).toFloat()
                            }
                            val py = cy + (r * sin(angle)).toFloat()
                            illuminatedPath.lineTo(tx, py)
                        }
                    } else {
                        for (i in 0..steps) {
                            val angle = PI / 2 + PI * i / steps
                            val px = cx + (r * cos(angle)).toFloat()
                            val py = cy + (r * sin(angle)).toFloat()
                            if (i == 0) illuminatedPath.moveTo(px, py) else illuminatedPath.lineTo(px, py)
                        }
                        for (i in steps downTo 0) {
                            val angle = PI / 2 + PI * i / steps
                            val tx = if (isTerminatorConvex) {
                                cx + (terminatorXRadius * cos(angle)).toFloat()
                            } else {
                                cx - (terminatorXRadius * cos(angle)).toFloat()
                            }
                            val py = cy + (r * sin(angle)).toFloat()
                            illuminatedPath.lineTo(tx, py)
                        }
                    }
                    illuminatedPath.close()

                    clipPath(illuminatedPath, clipOp = ClipOp.Difference) {
                        drawCircle(Color(0xB3000000), radius = r, center = Offset(cx, cy))
                    }
                }
            )
        }
    }
}
