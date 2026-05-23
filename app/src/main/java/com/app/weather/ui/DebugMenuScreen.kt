package com.app.weather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.weather.ui.theme.OpenRundeFontFamily

@Composable
fun DebugMenuScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSelectWeather: (WeatherType) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(top = 64.dp, start = 24.dp, end = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Debug Tools", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick cycle pill
        Row(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f)).clickable {
                val types = WeatherType.entries.toTypedArray()
                onSelectWeather(types.random())
            }.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cycle Random Weather", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Scrolling Animation Test", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Odometer Test Tool ────────────────────────────────────────────
        var testFrom by remember { mutableIntStateOf(0) }
        var testTarget by remember { mutableIntStateOf(24) }
        var activeSnap by remember { mutableStateOf<Int?>(null) }
        var activeAnimate by remember { mutableStateOf(24) }

        LaunchedEffect(testFrom) {
            activeSnap = testFrom
            activeAnimate = testFrom
        }
        
        LaunchedEffect(testTarget) {
            activeSnap = null
            activeAnimate = testTarget
        }

        val numberOptions = remember { (0..99).map { it.toString() } }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.05f)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedOdometerText(
                    temp = activeAnimate,
                    snapTo = activeSnap,
                    style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = OpenRundeFontFamily, letterSpacing = (-0.5).sp),
                    animationEnabled = true
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FROM", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        WheelPicker(
                            options = numberOptions,
                            selectedIndex = testFrom,
                            onIndexSelected = { testFrom = it }
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TARGET", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        WheelPicker(
                            options = numberOptions,
                            selectedIndex = testTarget,
                            onIndexSelected = { testTarget = it }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Visual Effects Toggles ────────────────────────────────────────────
        Text("Visual Effects", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable { onUpdateSettings(settings.copy(enableClouds = !settings.enableClouds)) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Enable Clouds", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Volumetric cloud layer very laggy", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Switch(
                checked = settings.enableClouds,
                onCheckedChange = { onUpdateSettings(settings.copy(enableClouds = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4A90E2),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Force Weather State", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(WeatherType.entries.toTypedArray()) { type ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectWeather(type); onBack() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}