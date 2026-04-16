package com.app.weather.ui

import android.annotation.SuppressLint
import android.graphics.RuntimeShader
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import kotlin.math.*

// ─── Shaders ─────────────────────────────────────────────────────────────────

@Language("AGSL")
private const val SKY_SHADER = """
    uniform float scrollOffset;
    uniform float iResolutionX;
    uniform float iResolutionY;
    layout(color) uniform half4 skyTop;
    layout(color) uniform half4 skyBottom;
    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord.xy / float2(iResolutionX, iResolutionY);
        float parallaxShift = scrollOffset * 0.2;
        vec4 bg = mix(skyTop, skyBottom, uv.y + 0.2 + parallaxShift);
        return half4(bg.r, bg.g, bg.b, 1.0);
    }
"""

@Language("AGSL")
private const val CLOUD_SHADER = """
    uniform float iTime;
    uniform float scrollOffset;
    uniform float iResolutionX;
    uniform float iResolutionY;
    layout(color) uniform half4 cloudColor;
    uniform float cloudDensityMult;
    uniform float windSpeedMult;
    uniform float layerSeed;
    uniform float layerAlphaMult;
    uniform float isFog;

    float hash(float2 p) {
        float3 p3 = fract(float3(p.xyx) * float3(0.1031, 0.1030, 0.0973));
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }

    float noise(float2 x) {
        float2 i = floor(x);
        float2 f = fract(x);
        float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        return mix(
            mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
            mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
            u.y
        );
    }

    const float2x2 m = float2x2(0.8, -0.6, 0.6, 0.8);

    float fbm(float2 p) {
        float f = 0.0;
        float a = 0.5;
        for (int i = 0; i < 6; i++) {
            f += a * noise(p);
            p = m * p * 2.0;
            a *= 0.5;
        }
        return f;
    }

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / float2(iResolutionX, iResolutionY);
        float aspect = iResolutionX / iResolutionY;
        uv.y -= scrollOffset * 0.5;
        float2 cuv = float2(uv.x * aspect, uv.y);
        float2 p = cuv * 3.0 + layerSeed;
        p.x += iTime * 0.012 * windSpeedMult;
        float w = fbm(p * 0.8);
        float n = fbm(p + float2(w, w) * 1.5);
        float vMask = 1.0;
        if (isFog > 0.5) {
            vMask = smoothstep(0.1, 1.0, uv.y + n * 0.2);
            vMask *= smoothstep(0.0, 0.3, uv.y);
        } else {
            float center = 0.25;
            float spread = 0.3;
            float vDist = uv.y - center;
            vMask = exp(-(vDist * vDist) / (2.0 * spread * spread));
            vMask *= smoothstep(0.65, 0.3, uv.y);
            vMask *= smoothstep(-0.1, 0.1, uv.y);
        }
        float density = 0.0;
        if (isFog > 0.5) {
            density = smoothstep(0.2, 0.9, n) * vMask * cloudDensityMult;
        } else {
            density = smoothstep(0.35, 0.75, n) * vMask * clamp(cloudDensityMult, 0.0, 1.5);
        }
        density = clamp(density, 0.0, 1.0);
        float2 lightDir = float2(-0.05, -0.05);
        float ln = fbm(p + lightDir + float2(w, w) * 1.5);
        float lDensity = 0.0;
        if (isFog > 0.5) {
            lDensity = smoothstep(0.2, 0.9, ln) * vMask * cloudDensityMult;
        } else {
            lDensity = smoothstep(0.35, 0.75, ln) * vMask * clamp(cloudDensityMult, 0.0, 1.5);
        }
        float shadow = clamp(density - lDensity, 0.0, 1.0);
        float highlight = clamp(lDensity - density, 0.0, 1.0);
        vec3 col = cloudColor.rgb;
        if (isFog > 0.5) {
            col *= mix(1.0, 0.8, shadow);
            col += highlight * 0.15;
        } else {
            col *= mix(1.0, 0.3, shadow * 2.5);
            col += highlight * 0.5;
        }
        return half4(col.r, col.g, col.b, density * layerAlphaMult);
    }
"""

@Composable
private fun WidgetLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = label.uppercase(), color = contentColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun FullWidgetBox(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, widgetBg: Color, contentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
        Column {
            WidgetLabel(icon, label, contentColor)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainWeatherScreen(data: WeatherData, settings: AppSettings, onRefresh: () -> Unit = {}) {
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    val maxScroll = 6000f
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    
    var isRefreshing by remember { mutableStateOf(false) }
    var isPullingAllowed by remember { mutableStateOf(false) }

    val pullThreshold = -80f
    val maxOverscroll = -120f

    val scrollableState = rememberScrollableState { delta ->
        if (isRefreshing) return@rememberScrollableState delta
        
        val friction = 0.55f 
        val adjustedDelta = delta * friction

        val prevOffset = scrollOffset.floatValue
        val newOffset = scrollOffset.floatValue - adjustedDelta
        
        if (prevOffset > 0f && newOffset < 0f && !isPullingAllowed) {
            scrollOffset.floatValue = 0f
            return@rememberScrollableState delta
        }
        
        scrollOffset.floatValue = newOffset.coerceIn(maxOverscroll, maxScroll)
        delta 
    }

    LaunchedEffect(scrollableState.isScrollInProgress) {
        if (scrollableState.isScrollInProgress) isPullingAllowed = scrollOffset.floatValue <= 1f
        else {
            if (scrollOffset.floatValue < 0f && !isRefreshing) {
                if (scrollOffset.floatValue <= pullThreshold) isRefreshing = true
            }
        }
    }

    val smoothProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { (scrollOffset.floatValue / 800f).coerceIn(0f, 1f) }
            .collect { target -> 
                launch { 
                    smoothProgress.animateTo(
                        target, 
                        // 👇 Fast speed (350f) but silky glide stop (0.95f)
                        spring(dampingRatio = 0.95f, stiffness = 350f)
                    ) 
                } 
            }
    }

    val visualState = if (settings.visualStateOverride != VisualState.Automatic) settings.visualStateOverride else {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) { in 5..6 -> VisualState.Sunrise; in 7..11 -> VisualState.Day; in 12..16 -> VisualState.Afternoon; in 17..18 -> VisualState.Sunset; in 19..21 -> VisualState.Evening; else -> VisualState.Night }
    }

    val isBrightBg = (visualState == VisualState.Day || visualState == VisualState.Afternoon) && (data.type == WeatherType.Clear || data.type == WeatherType.Clouds)
    val contentColor = if (isBrightBg) Color.Black else Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.7f)
    val widgetBg = if (isBrightBg) Color.White.copy(alpha = 0.5f) else Color(0xFF121212).copy(alpha = 0.6f)

    val headerText = when (settings.headerType) { HeaderType.Greeting -> data.type.title; HeaderType.Standard -> data.location; HeaderType.Sunrise -> if (data.sunriseEpoch != null) "Sunrise at ${epochToTimeStr(data.sunriseEpoch)}" else "Sunrise --"; HeaderType.FeelsLike -> "Feels like ${data.feelsLike ?: "__"}°"; HeaderType.Disabled -> "" }

    val textMeasurer = rememberTextMeasurer()
    val titleTextWidthPx = remember(headerText) { textMeasurer.measure(text = headerText, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)).size.width }
    val exactTitleWidthDp = with(density) { titleTextWidthPx.toDp() }

    val skyTopColor by animateColorAsState(getSkyColors(data.type, visualState).first, animationSpec = tween(1500), label = "")
    val skyBottomColor by animateColorAsState(getSkyColors(data.type, visualState).second, animationSpec = tween(1500), label = "")
    val cloudColor by animateColorAsState(getCloudColor(data.type, visualState), animationSpec = tween(1500), label = "")
    val cloudDensityMult by animateFloatAsState(when (data.type) { WeatherType.Clear -> 0.0f; WeatherType.Clouds -> 1.0f; WeatherType.Rain, WeatherType.Drizzle -> 1.2f; WeatherType.Thunderstorm -> 1.5f; WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke, WeatherType.Dust, WeatherType.Sand, WeatherType.Ash -> 1.2f; WeatherType.Snow -> 1.0f; else -> 1.0f }, animationSpec = tween(1500), label = "")
    val isFogAnim by animateFloatAsState(targetValue = if (data.type in listOf(WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke)) 1f else 0f, animationSpec = tween(1500), label = "")
    val windSpeedMult by animateFloatAsState(when (data.type) { WeatherType.Thunderstorm -> 2.5f; WeatherType.Rain -> 1.5f; WeatherType.Clear -> 0.5f; else -> 1.0f }, animationSpec = tween(1500), label = "")

    val skyShader = remember { RuntimeShader(SKY_SHADER) }
    val cloudBackShader = remember { RuntimeShader(CLOUD_SHADER) }
    val cloudFrontShader = remember { RuntimeShader(CLOUD_SHADER) }
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (isActive) { withInfiniteAnimationFrameMillis { frameTime -> time.floatValue = (frameTime % 100_000L) / 1000f } }
    }

    // GlassState wrapper for the background content so the top bar can blur it
    val internalGlassState = remember { GlassState() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ─── LAYER 1: Scrollable Background Content (Captured by GlassState) ───
        Box(modifier = Modifier
            .fillMaxSize()
            .glassRoot(internalGlassState)
            .scrollable(state = scrollableState, orientation = Orientation.Vertical)
        ) {
            Box(modifier = Modifier.fillMaxSize().drawWithCache {
                onDrawBehind {
                    skyShader.setFloatUniform("iResolutionX", size.width)
                    skyShader.setFloatUniform("iResolutionY", size.height)
                    skyShader.setFloatUniform("scrollOffset", scrollOffset.floatValue / size.height)
                    skyShader.setColorUniform("skyTop", skyTopColor.toArgb())
                    skyShader.setColorUniform("skyBottom", skyBottomColor.toArgb())
                    drawRect(brush = ShaderBrush(skyShader))
                }
            })

            if (settings.enableClouds) {
                Box(modifier = Modifier.fillMaxSize().drawWithCache {
                    onDrawBehind {
                        cloudBackShader.setFloatUniform("iResolutionX", size.width)
                        cloudBackShader.setFloatUniform("iResolutionY", size.height)
                        cloudBackShader.setFloatUniform("iTime", time.floatValue)
                        cloudBackShader.setFloatUniform("scrollOffset", scrollOffset.floatValue / size.height)
                        cloudBackShader.setColorUniform("cloudColor", cloudColor.toArgb())
                        cloudBackShader.setFloatUniform("cloudDensityMult", cloudDensityMult)
                        cloudBackShader.setFloatUniform("windSpeedMult", windSpeedMult)
                        cloudBackShader.setFloatUniform("layerSeed", 0f)
                        cloudBackShader.setFloatUniform("layerAlphaMult", 1f)
                        cloudBackShader.setFloatUniform("isFog", isFogAnim)
                        drawRect(brush = ShaderBrush(cloudBackShader))
                    }
                })
            }

            val refreshText by remember { derivedStateOf { when { isRefreshing -> "Refreshing..."; scrollOffset.floatValue <= pullThreshold + 5f -> "Release to refresh"; else -> "Pull to refresh" } } }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .graphicsLayer { alpha = if (isRefreshing) 1f else (-scrollOffset.floatValue / 60f).coerceIn(0f, 1f) },
                contentAlignment = Alignment.TopCenter
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val loadingRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)), label = "")

                Row(modifier = Modifier.padding(top = 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = contentColor, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (isRefreshing) loadingRotation else -scrollOffset.floatValue * 3f })
                    Text(text = refreshText, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(8000.dp).graphicsLayer {
                val offset = scrollOffset.floatValue
                translationY = if (offset < 0f) -offset * 0.4f else 0f
                
                if (settings.blur) {
                    val pullBlurAlpha = if (isRefreshing) 1f else ((-offset / 80f) * (1f - (offset / 50f).coerceIn(0f, 1f))).coerceIn(0f, 1f)
                    val pullBlur = (pullBlurAlpha * 12f).dp.toPx()
                    if (pullBlur > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlur, pullBlur, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
                    else renderEffect = null
                }
            }) {

                val dynamicQuote = if (settings.quoteStyle == QuoteStyle.Compact) {
                    if (data.type == WeatherType.Rain || data.type == WeatherType.Thunderstorm || data.type == WeatherType.Drizzle) "It feels like ${data.feelsLike ?: "__"}°C today,\nRaining until later. Beware of flood."
                    else "It feels like ${data.feelsLike ?: "__"}°C today,\nRain probability is probably ${data.rainProb}."
                } else {
                    val mainEvent = when (data.type) { WeatherType.Clear -> "It's completely clear."; WeatherType.Mist, WeatherType.Fog -> "Visibility is extremely low due to fog."; WeatherType.Haze, WeatherType.Smoke -> "Air quality is affected by haze."; WeatherType.Rain -> "It's heavily raining."; WeatherType.Drizzle -> "It's light raining now."; WeatherType.Thunderstorm -> "Intense storms are active."; WeatherType.Snow -> "It's snowing heavily."; WeatherType.Clouds -> "It's overcast and cloudy."; WeatherType.Tornado -> "Tornado warning! Seek shelter!"; else -> "Weather conditions are unusual." }
                    val feelScenario = when { data.feelsLike == null -> "fetching data"; data.feelsLike < 10 -> "very cold, definitely bundle up"; data.feelsLike < 20 -> "a bit chilly, bring a light jacket"; data.feelsLike < 28 -> "nice and pleasant outside"; else -> "quite hot, stay hydrated" }
                    "$mainEvent It's ${data.description} right now in ${data.location}, visibility is ${data.visibility}. The temperature feels like ${data.feelsLike ?: "__"}°C ($feelScenario). Rain probability: ${data.rainProb}."
                }

                // TEXT BLOCK: Retains DECAL blur artifact fix & padding bleed area
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val prog = smoothProgress.value
                        translationY = (345f - 220f * prog).dp.toPx()
                        alpha = (1f - prog * 1.5f).coerceIn(0f, 1f)
                        
                        if (settings.blur) {
                            val blurPx = (20f * prog.coerceAtLeast(0f)).dp.toPx()
                            if (blurPx > 0f) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
                                ).asComposeRenderEffect()
                            } else renderEffect = null
                        }
                    }
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp) // padding for unbounded blur bleed
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = dynamicQuote, color = contentColor.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val infiniteTransition = rememberInfiniteTransition(label = "")
                        val windRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
                        val metrics = listOf(Pair(Icons.Default.FilterDrama, data.aqi), Pair(Icons.Default.NorthEast, data.wind), Pair(Icons.Default.Visibility, data.visibility), Pair(Icons.Default.WaterDrop, data.humidity))
                        metrics.forEach { (icon, label) ->
                            Row(modifier = Modifier.wrapContentWidth().clip(RoundedCornerShape(percent = 50)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp).graphicsLayer { if (settings.debugRotateWindSpeed && icon == Icons.Default.NorthEast) rotationZ = windRotation })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                val mapToIcon = remember { { t: WeatherType -> when (t) { WeatherType.Clear -> Icons.Default.WbSunny; WeatherType.Clouds, WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke -> Icons.Default.FilterDrama; WeatherType.Rain, WeatherType.Drizzle -> Icons.Default.WaterDrop; WeatherType.Thunderstorm, WeatherType.Snow, WeatherType.Tornado, WeatherType.Squall -> Icons.Default.Thunderstorm; else -> Icons.Default.FilterDrama } } }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).graphicsLayer {
                    val prog = smoothProgress.value
                    val offset = scrollOffset.floatValue
                    val excessDpPx = (offset - 800f).coerceAtLeast(0f).dp.toPx()
                    translationY = (680f - 500f * prog).dp.toPx() - excessDpPx
                }) {
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

                    Spacer(Modifier.height(12.dp))
                    FullWidgetBox(icon = Icons.Default.DateRange, label = "10-Day forecast", widgetBg = widgetBg, contentColor = contentColor) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.dailyForecast.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(item.day, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.4f))
                                    Icon(mapToIcon(item.type), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.weight(0.5f))
                                    Text(item.tempMin, color = secondaryContentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) {
                                        val pop = item.pop / 100f
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pop.coerceIn(0.05f, 1f)).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.7f)))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.temp, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.WaterDrop, "Precipitation", contentColor)
                                Spacer(Modifier.height(8.dp))
                                val precipMm = data.rainfallNext24h ?: 0.0
                                Text("${String.format("%.1f", precipMm)} mm", color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text("expected today", color = secondaryContentColor, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(((precipMm / 50.0).coerceIn(0.0, 1.0).toFloat()).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(Color(0xFF81D4FA))) }
                            }
                        }
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.WaterDrop, "Humidity", contentColor)
                                Spacer(Modifier.height(8.dp))
                                val hVal = data.humidityValue ?: 0
                                Text("${hVal}%", color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text(when { hVal > 80 -> "Very humid"; hVal > 60 -> "Humid"; hVal > 40 -> "Comfortable"; else -> "Dry" }, color = secondaryContentColor, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((hVal / 100f).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(Color(0xFF4FC3F7))) }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.DeviceThermostat, "Feels like", contentColor)
                                Spacer(Modifier.height(8.dp))
                                Text("${data.feelsLike ?: "__"}°", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(data.feelsLikeDesc, color = secondaryContentColor, fontSize = 12.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.WbSunny, "UV Index", contentColor)
                                Spacer(Modifier.height(8.dp))
                                val uv = data.uvIndex
                                Text(if (uv != null) String.format("%.1f", uv) else "__", color = contentColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                                Text(when { uv == null -> "--"; uv < 3 -> "Low"; uv < 6 -> "Moderate"; uv < 8 -> "High"; uv < 11 -> "Very High"; else -> "Extreme" }, color = secondaryContentColor, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                if (uv != null) {
                                    val uvColor = when { uv < 3 -> Color(0xFF66BB6A); uv < 6 -> Color(0xFFFFEE58); uv < 8 -> Color(0xFFFFA726); uv < 11 -> Color(0xFFEF5350); else -> Color(0xFFAB47BC) }
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha = 0.2f))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(((uv / 12.0).coerceIn(0.0, 1.0).toFloat()).coerceAtLeast(0.03f)).clip(RoundedCornerShape(50)).background(uvColor)) }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (data.lat != null && data.lon != null) {
                        val lat = data.lat; val lon = data.lon
                        val mapHtml = remember(lat, lon) { "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"><link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/><script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script><style>body{margin:0;padding:0}#map{width:100%;height:100vh;}</style></head><body><div id=\"map\"></div><script>var map = L.map('map', {zoomControl:false, attributionControl:false, dragging:false, scrollWheelZoom:false, touchZoom:false, doubleClickZoom:false}).setView([$lat,$lon], 10);L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png').addTo(map);L.circleMarker([$lat,$lon], {radius:6, color:'#fff', fillColor:'#4fc3f7', fillOpacity:1, weight:2}).addTo(map);</script></body></html>" }
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
                            AndroidView(factory = { ctx -> WebView(ctx).also { wv -> wv.settings.javaScriptEnabled = true; wv.settings.domStorageEnabled = true; wv.webViewClient = WebViewClient(); wv.setBackgroundColor(android.graphics.Color.TRANSPARENT); wv.loadDataWithBaseURL("https://openstreetmap.org", mapHtml, "text/html", "utf-8", null) } }, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("${data.location.uppercase()} MAP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp) } }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WidgetLabel(Icons.Default.NorthEast, "Wind", contentColor)
                                Spacer(Modifier.height(6.dp))
                                Canvas(modifier = Modifier.size(70.dp)) {
                                    val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 4.dp.toPx()
                                    drawCircle(color = contentColor.copy(alpha = 0.15f), radius = r)
                                    for (d in 0..315 step 45) { val rd = Math.toRadians(d.toDouble()); drawCircle(color = contentColor.copy(alpha = 0.4f), radius = 2.dp.toPx(), center = Offset(cx + (r - 6.dp.toPx()) * sin(rd).toFloat(), cy - (r - 6.dp.toPx()) * cos(rd).toFloat())) }
                                    val rad = Math.toRadians((data.windDeg ?: 0).toDouble())
                                    val tip = Offset(cx + (r - 8.dp.toPx()) * sin(rad).toFloat(), cy - (r - 8.dp.toPx()) * cos(rad).toFloat())
                                    drawLine(contentColor, Offset(cx - 20.dp.toPx() * sin(rad).toFloat(), cy + 20.dp.toPx() * cos(rad).toFloat()), tip, strokeWidth = 2.5f, cap = StrokeCap.Round)
                                    drawCircle(Color(0xFF4FC3F7), radius = 4.dp.toPx(), center = tip)
                                }
                                Text(data.wind, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (data.windGust != "--") Text("Gust: ${data.windGust}", color = secondaryContentColor, fontSize = 11.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.FilterDrama, "Air Quality", contentColor)
                                Spacer(Modifier.height(8.dp))
                                val aqi = data.aqiValue ?: 0
                                Text(when (aqi) { 1 -> "Good"; 2 -> "Fair"; 3 -> "Moderate"; 4 -> "Poor"; 5 -> "Very Poor"; else -> "--" }, color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text("AQI index $aqi/5", color = secondaryContentColor, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { listOf(Color(0xFF66BB6A), Color(0xFFFFEE58), Color(0xFFFFA726), Color(0xFFEF5350), Color(0xFFAB47BC)).forEachIndexed { i, c -> Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(if (i < aqi) c else contentColor.copy(alpha = 0.2f))) } }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column {
                                WidgetLabel(Icons.Default.Visibility, "Visibility", contentColor)
                                Spacer(Modifier.height(8.dp))
                                Text(data.visibility, color = contentColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                val visKm = data.visibilityM?.div(1000) ?: 0
                                Text(when { visKm >= 10 -> "Clear view"; visKm >= 5 -> "Mostly clear"; visKm >= 2 -> "Light haze"; else -> "Dense fog" }, color = secondaryContentColor, fontSize = 12.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).height(195.dp).clip(RoundedCornerShape(24.dp)).background(widgetBg).padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                WidgetLabel(Icons.Default.Compress, "Pressure", contentColor)
                                Spacer(Modifier.height(6.dp))
                                val pHpa = data.pressure ?: 1013
                                Canvas(modifier = Modifier.size(70.dp)) {
                                    val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 4.dp.toPx()
                                    for (t in 0..120 step 10) { val ang = Math.toRadians(180.0 + t * 1.5); drawLine(contentColor.copy(alpha = 0.3f), Offset(cx + (r - 8.dp.toPx()) * cos(ang).toFloat(), cy + (r - 8.dp.toPx()) * sin(ang).toFloat()), Offset(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat()), strokeWidth = 1.5f) }
                                    val needleAng = Math.toRadians(180.0 + ((pHpa - 950) / 100f).coerceIn(0f, 1f) * 180.0)
                                    drawLine(contentColor, Offset(cx, cy), Offset(cx + (r - 10.dp.toPx()) * cos(needleAng).toFloat(), cy + (r - 10.dp.toPx()) * sin(needleAng).toFloat()), strokeWidth = 2.5f, cap = StrokeCap.Round)
                                    drawCircle(Color(0xFF4FC3F7), radius = 4.dp.toPx(), center = Offset(cx, cy))
                                }
                                Text("$pHpa hPa", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Low", color = secondaryContentColor, fontSize = 10.sp); Text("High", color = secondaryContentColor, fontSize = 10.sp) }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FullWidgetBox(icon = Icons.Default.WbSunny, label = "Sunrise & Sunset", widgetBg = widgetBg, contentColor = contentColor) {
                        val sunriseStr = if (data.sunriseEpoch != null) epochToTimeStr(data.sunriseEpoch) else "--"
                        val sunsetStr  = if (data.sunsetEpoch  != null) epochToTimeStr(data.sunsetEpoch)  else "--"
                        val nowMs = System.currentTimeMillis() / 1000L
                        val sunProgress = if (data.sunriseEpoch != null && data.sunsetEpoch != null && nowMs in data.sunriseEpoch..data.sunsetEpoch) ((nowMs - data.sunriseEpoch).toFloat() / (data.sunsetEpoch - data.sunriseEpoch).toFloat()).coerceIn(0f, 1f) else if (data.sunriseEpoch != null && nowMs < data.sunriseEpoch) 0f else 1f

                        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            val w = size.width; val h = size.height; val arcLeft = 16.dp.toPx(); val arcRight = w - 16.dp.toPx(); val arcTop = 8.dp.toPx()
                            var x = arcLeft; while (x < arcRight) { drawLine(contentColor.copy(alpha = 0.25f), Offset(x, h - 20.dp.toPx()), Offset(x + 6.dp.toPx(), h - 20.dp.toPx()), strokeWidth = 1.5f); x += 10.dp.toPx() }
                            drawPath(Path().apply { moveTo(arcLeft, h - 20.dp.toPx()); cubicTo(arcLeft, arcTop, arcRight, arcTop, arcRight, h - 20.dp.toPx()) }, color = contentColor.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
                            val sunX = arcLeft + (arcRight - arcLeft) * sunProgress
                            val sunYSimple = h - 20.dp.toPx() - (4 * sunProgress * (1 - sunProgress)) * (h - arcTop - 20.dp.toPx())
                            drawCircle(contentColor, radius = 8.dp.toPx(), center = Offset(sunX, sunYSimple)); drawCircle(Color(0xFFFFECB3), radius = 5.dp.toPx(), center = Offset(sunX, sunYSimple))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("SUNRISE", color = secondaryContentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp); Text(sunriseStr, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Column(horizontalAlignment = Alignment.End) { Text("SUNSET", color = secondaryContentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp); Text(sunsetStr, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FullWidgetBox(icon = Icons.Default.WaterDrop, label = "Rainfall", widgetBg = widgetBg, contentColor = contentColor) {
                        Text("${String.format("%.0f", data.rainfallLast24h ?: 0.0)} mm", color = contentColor, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Text("in last 24h", color = secondaryContentColor, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("${String.format("%.0f", data.rainfallNext24h ?: 0.0)} mm expected in next 24h.", color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(12.dp))
                    FullWidgetBox(icon = Icons.Default.NightsStay, label = "Moon Phase", widgetBg = widgetBg, contentColor = contentColor) {
                        val phase = data.moonPhase ?: 0.0
                        val phaseName = when { phase < 0.03 || phase > 0.97 -> "New Moon"; phase < 0.22 -> "Waxing Crescent"; phase < 0.28 -> "First Quarter"; phase < 0.47 -> "Waxing Gibbous"; phase < 0.53 -> "Full Moon"; phase < 0.72 -> "Waning Gibbous"; phase < 0.78 -> "Last Quarter"; else -> "Waning Crescent" }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Canvas(modifier = Modifier.size(72.dp)) {
                                val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2 - 2.dp.toPx()
                                drawCircle(contentColor.copy(alpha = 0.1f), radius = r); drawCircle(Color(0xFFFFECB3).copy(alpha = 0.9f), radius = r)
                                val illumination = if (phase <= 0.5) phase * 2 else (1.0 - phase) * 2  
                                if (phase < 0.5) drawCircle(Color(0xFF1A1A2E), radius = r, center = Offset(cx + (((1.0 - illumination).toFloat()) * 2 - 1) * r, cy))
                                else drawCircle(Color(0xFF1A1A2E), radius = r, center = Offset(cx + (1 - ((1.0 - illumination).toFloat()) * 2) * r, cy))
                            }
                            Column { Text(phaseName, color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("${(phase * 100).toInt()}% through cycle", color = secondaryContentColor, fontSize = 13.sp) }
                        }
                    }
                    Spacer(Modifier.height(120.dp))
                }
            }

            if (settings.enableClouds) {
                Box(modifier = Modifier.fillMaxSize().drawWithCache {
                    onDrawBehind {
                        cloudFrontShader.setFloatUniform("iResolutionX", size.width)
                        cloudFrontShader.setFloatUniform("iResolutionY", size.height)
                        cloudFrontShader.setFloatUniform("iTime", time.floatValue)
                        cloudFrontShader.setFloatUniform("scrollOffset", scrollOffset.floatValue / size.height)
                        cloudFrontShader.setColorUniform("cloudColor", cloudColor.toArgb())
                        cloudFrontShader.setFloatUniform("cloudDensityMult", cloudDensityMult)
                        cloudFrontShader.setFloatUniform("windSpeedMult", windSpeedMult)
                        cloudFrontShader.setFloatUniform("layerSeed", 12f)
                        cloudFrontShader.setFloatUniform("layerAlphaMult", 0.45f)
                        cloudFrontShader.setFloatUniform("isFog", isFogAnim)
                        drawRect(brush = ShaderBrush(cloudFrontShader))
                    }
                })
            }
        }

        // ─── LAYER 2: 3-Step Frosted Glass Overlay for Header (Fades in on scroll) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .graphicsLayer { alpha = smoothProgress.value.coerceIn(0f, 1f) }
        ) {
            if (settings.blur) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GlassPillBackground(state = internalGlassState, blurRadius = 30f, modifier = Modifier.fillMaxWidth().height(40.dp))
                    GlassPillBackground(state = internalGlassState, blurRadius = 15f, modifier = Modifier.fillMaxWidth().height(45.dp))
                    GlassPillBackground(state = internalGlassState, blurRadius = 5f, modifier = Modifier.fillMaxWidth().height(45.dp))
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to skyTopColor.copy(alpha = 0.85f),
                    0.5f to skyTopColor.copy(alpha = 0.4f),
                    1f to Color.Transparent
                )
            ))
        }

        // ─── LAYER 3: Sharp Header Texts (Sits securely on top of the blur) ───

        Text(
            text = headerText, color = contentColor.copy(alpha = 0.9f), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                val prog = smoothProgress.value
                val scale = (18f + 10f * prog) / 28f
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = 32.dp.toPx()
                translationY = (130f - 70f * prog).dp.toPx()
            }
        )

        if (settings.headerType != HeaderType.Disabled) {
            Text(text = "-", color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.graphicsLayer {
                val prog = smoothProgress.value
                alpha = prog.coerceIn(0f, 1f)
                val finalX = exactTitleWidthDp.toPx() + 8.dp.toPx()
                translationX = 32.dp.toPx() + (finalX * prog)
                translationY = (130f - 70f * prog).dp.toPx()
            })
        }

        Text(
            text = "${data.temp ?: "__"}°", color = contentColor, fontSize = 130.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                val prog = smoothProgress.value
                val scale = (130f - 102f * prog) / 130f
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
                alpha = (1f - 0.3f * prog).coerceIn(0f, 1f)
                val exactTitlePx = exactTitleWidthDp.toPx()
                val targetDockedX = 32.dp.toPx() + exactTitlePx + 22.dp.toPx()
                translationX = 32.dp.toPx() + (targetDockedX - 32.dp.toPx()) * prog
                translationY = (175f - 115f * prog).dp.toPx()
            }
        )

        val providerName = settings.provider.lowercase()
        val cityInfoStr = if (settings.headerType == HeaderType.Standard) "" else "${data.location.lowercase()} - "
        val updateText = "${cityInfoStr}Updated at ${data.lastUpdated} from $providerName"

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentWidth().graphicsLayer {
            val prog = smoothProgress.value
            translationY = (640f - 550f * prog).dp.toPx()
            val centerX = (screenWidthPx - size.width) / 2f
            val leftX = 32.dp.toPx()
            translationX = centerX + (leftX - centerX) * prog
        }) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(10.dp).graphicsLayer { rotationZ = 45f })
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = updateText, color = contentColor.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun getSkyColors(weather: WeatherType, state: VisualState): Pair<Color, Color> {
    return when (state) {
        VisualState.Sunrise -> when (weather) { WeatherType.Clear -> Color(0xFFFF5F6D) to Color(0xFFFFC371); WeatherType.Clouds -> Color(0xFFE0EAFC) to Color(0xFFCFDEF3); else -> Color(0xFF3E5151) to Color(0xFFDECBA4) }
        VisualState.Day -> when (weather) { WeatherType.Clear -> Color(0xFF2980B9) to Color(0xFF6DD5FA); WeatherType.Clouds -> Color(0xFF757F9A) to Color(0xFFD7DDE8); WeatherType.Rain -> Color(0xFF203A43) to Color(0xFF2C5364); else -> Color(0xFFBDC3C7) to Color(0xFF2C3E50) }
        VisualState.Afternoon -> when (weather) { WeatherType.Clear -> Color(0xFF4CA1AF) to Color(0xFFC4E0E5); else -> Color(0xFF304352) to Color(0xFFD7D2CC) }
        VisualState.Sunset -> when (weather) { WeatherType.Clear -> Color(0xFF41295a) to Color(0xFF2F0743); WeatherType.Clouds -> Color(0xFF1c92d2) to Color(0xFFf2fcfe); else -> Color(0xFF3a6186) to Color(0xFF89253e) }
        VisualState.Evening -> when (weather) { WeatherType.Clear -> Color(0xFF232526) to Color(0xFF414345); else -> Color(0xFF141E30) to Color(0xFF243B55) }
        VisualState.Night -> when (weather) { WeatherType.Clear -> Color(0xFF0D0D0D) to Color(0xFF000000); else -> Color(0xFF141E30) to Color(0xFF000000) }
        else -> Color(0xFF2980B9) to Color(0xFF6DD5FA)
    }
}

private fun getCloudColor(weather: WeatherType, state: VisualState): Color {
    return when (state) { VisualState.Sunrise -> Color(0xFFFFD194); VisualState.Day -> if (weather == WeatherType.Clear) Color(0xFFFFFFFF) else Color(0xFFF0F0F0); VisualState.Afternoon -> Color(0xFFE3F2FD); VisualState.Sunset -> Color(0xFFFF8A65); VisualState.Evening -> Color(0xFFB0BEC5); VisualState.Night -> Color(0xFF455A64); else -> Color.White }
}

private fun epochToTimeStr(epoch: Long): String { return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(epoch * 1000)) }