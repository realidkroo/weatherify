package com.app.weather.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language
import kotlin.math.*
import com.app.weather.ui.widgets.large.*
import com.app.weather.ui.widgets.small.*

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


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainWeatherScreen(
    data: WeatherData,
    settings: AppSettings,
    onRefresh: () -> Unit = {},
    testOdometerFrom: Int? = null,
    testOdometerTarget: Int? = null,
    testOdometerTrigger: Int = 0
) {
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    val maxScroll = 10000f
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    var isRefreshing by remember { mutableStateOf(false) }
    var isPullingAllowed by remember { mutableStateOf(false) }

    val pullThreshold = -120f
    val maxOverscroll = -160f

    var lastScrollDirection by remember { mutableFloatStateOf(0f) }

    val scrollableState = rememberScrollableState { delta ->
        lastScrollDirection = delta
        if (isRefreshing) return@rememberScrollableState delta

        val friction = 0.55f
        val adjustedDelta = delta * friction

        val prevOffset = scrollOffset.floatValue
        val newOffset = scrollOffset.floatValue - adjustedDelta

        if (newOffset > 0f) {
            isPullingAllowed = false
        }

        if (newOffset < 0f) {
            if (prevOffset > 0f || (prevOffset == 0f && !isPullingAllowed)) {
                scrollOffset.floatValue = 0f
                return@rememberScrollableState (prevOffset - 0f) / friction
            }
        }

        scrollOffset.floatValue = newOffset.coerceIn(maxOverscroll, maxScroll)
        delta
    }

    LaunchedEffect(scrollableState.isScrollInProgress) {
        if (scrollableState.isScrollInProgress) {
            isPullingAllowed = scrollOffset.floatValue <= 1f
        } else {
            if (scrollOffset.floatValue < 0f && !isRefreshing) {
                if (scrollOffset.floatValue <= pullThreshold) isRefreshing = true
            }
            if (scrollOffset.floatValue < 0f) {
                launch {
                    animate(
                        initialValue = scrollOffset.floatValue,
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) { value, _ ->
                        scrollOffset.floatValue = value
                    }
                }
            }

            val offset = scrollOffset.floatValue
            if (offset > 0f && offset < 800f) {
                launch {
                    val target = if (lastScrollDirection < 0) 800f else 0f
                    animate(
                        initialValue = scrollOffset.floatValue,
                        targetValue = target,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    ) { value, _ ->
                        scrollOffset.floatValue = value
                    }
                }
            }

            if (offset > 0f && offset < 800f) {
                launch {
                    delay(5000)
                    if (!scrollableState.isScrollInProgress && scrollOffset.floatValue > 0f && scrollOffset.floatValue < 800f) {
                        animate(
                            initialValue = scrollOffset.floatValue,
                            targetValue = 0f,
                            animationSpec = tween(1500, easing = FastOutSlowInEasing)
                        ) { value, _ ->
                            scrollOffset.floatValue = value
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
        }
    }

    LaunchedEffect(data.lastUpdatedMs) {
        isRefreshing = false
    }

    val smoothProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { (scrollOffset.floatValue / 800f).coerceIn(0f, 1f) }
            .collect { target ->
                launch {
                    if (settings.animation) {
                        smoothProgress.animateTo(
                            target,
                            spring(dampingRatio = 0.72f, stiffness = 180f)
                        )
                    } else {
                        smoothProgress.snapTo(target)
                    }
                }
            }
    }

    val visualState = if (settings.visualStateOverride != VisualState.Automatic) settings.visualStateOverride else {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..6 -> VisualState.Sunrise
            in 7..15 -> VisualState.Day
            in 16..17 -> VisualState.Afternoon
            18 -> VisualState.Sunset
            in 19..20 -> VisualState.Evening
            else -> VisualState.Night
        }
    }

    val contentColor = Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.7f)
    val widgetBg = Color.White.copy(alpha = 0.12f)

    val headerText = when (settings.headerType) { HeaderType.Greeting -> data.type.title; HeaderType.Standard -> data.location; HeaderType.Sunrise -> if (data.sunriseEpoch != null) "Sunrise at ${epochToTimeStr(data.sunriseEpoch)}" else "Sunrise --"; HeaderType.FeelsLike -> "Feels like ${data.feelsLike ?: "__"}°"; HeaderType.Disabled -> "" }

    val textMeasurer = rememberTextMeasurer()
    val titleTextWidthPx = remember(headerText) { textMeasurer.measure(text = headerText, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)).size.width }
    val exactTitleWidthDp = with(density) { titleTextWidthPx.toDp() }

    val skyTopColor by animateColorAsState(getSkyColors(data.type, visualState).first, animationSpec = if (settings.animation) tween(1500) else snap(), label = "")
    val skyBottomColor by animateColorAsState(getSkyColors(data.type, visualState).second, animationSpec = if (settings.animation) tween(1500) else snap(), label = "")
    val cloudColor by animateColorAsState(getCloudColor(data.type, visualState), animationSpec = if (settings.animation) tween(1500) else snap(), label = "")
    val cloudDensityMult by animateFloatAsState(when (data.type) { WeatherType.Clear -> 0.0f; WeatherType.Clouds -> 1.0f; WeatherType.Rain, WeatherType.Drizzle -> 1.2f; WeatherType.Thunderstorm -> 1.5f; WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke, WeatherType.Dust, WeatherType.Sand, WeatherType.Ash -> 1.2f; WeatherType.Snow -> 1.0f; else -> 1.0f }, animationSpec = if (settings.animation) tween(1500) else snap(), label = "")
    val isFogAnim by animateFloatAsState(targetValue = if (data.type in listOf(WeatherType.Mist, WeatherType.Fog, WeatherType.Haze, WeatherType.Smoke)) 1f else 0f, animationSpec = if (settings.animation) tween(1500) else snap(), label = "")
    val windSpeedMult by animateFloatAsState(when (data.type) { WeatherType.Thunderstorm -> 2.5f; WeatherType.Rain -> 1.5f; WeatherType.Clear -> 0.5f; else -> 1.0f }, animationSpec = if (settings.animation) tween(1500) else snap(), label = "")

    val skyShader = remember { RuntimeShader(SKY_SHADER) }
    val cloudBackShader = remember { RuntimeShader(CLOUD_SHADER) }
    val cloudFrontShader = remember { RuntimeShader(CLOUD_SHADER) }
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (isActive) { withInfiniteAnimationFrameMillis { frameTime -> time.floatValue = (frameTime % 100_000L) / 1000f } }
    }

    var displayTemp by remember(data.temp) { mutableStateOf(data.temp) }
    var forceSnap by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(testOdometerTrigger) {
        if (testOdometerTrigger > 0) {
            val f = testOdometerFrom ?: -1
            val t = testOdometerTarget ?: -1
            forceSnap = f
            displayTemp = f
            delay(100)
            forceSnap = null
            displayTemp = t
        }
    }

    val internalGlassState = remember { GlassState() }

    Box(modifier = Modifier.fillMaxSize().background(if (settings.vulkan) Color.Transparent else Color.Black)) {

        Box(modifier = Modifier
            .fillMaxSize()
            .glassRoot(internalGlassState)
            .scrollable(state = scrollableState, orientation = Orientation.Vertical)
        ) {
            if (settings.vulkan) {
                // ─── Experimental Vulkan/Rust Backend ───
                WeatherEngineView(
                    modifier = Modifier.fillMaxSize(),
                    scrollOffset = scrollOffset.floatValue
                )
            } else {
                // ─── Stable AGSL Shader Backend ───
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
            }

            val refreshText by remember { derivedStateOf { when { 
                isRefreshing -> "Refreshing..."
                scrollOffset.floatValue <= pullThreshold - 30f -> "please release please >_<"
                scrollOffset.floatValue <= pullThreshold + 5f -> "Release to refresh"
                else -> "Pull to refresh" 
            } } }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .graphicsLayer { 
                        alpha = if (isRefreshing) 1f else (-scrollOffset.floatValue / 60f).coerceIn(0f, 1f) 
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val loadingRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)), label = "")

                Row(
                    modifier = Modifier
                        .padding(top = 60.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = contentColor, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (isRefreshing && settings.animation) loadingRotation else if (isRefreshing) 0f else -scrollOffset.floatValue * 3f })
                    AnimatedContent(
                        targetState = refreshText,
                        transitionSpec = {
                            fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                        },
                        label = "refreshTextAnim"
                    ) { text ->
                        Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(8000.dp).graphicsLayer {
                val offset = scrollOffset.floatValue
                translationY = if (offset < 0f) -offset * 0.4f else 0f

                if (settings.blur) {
                    val pullBlurPx = if (offset < 0f) (-offset / 8f).coerceIn(0f, 15f).dp.toPx() else 0f
                    if (pullBlurPx > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlurPx, pullBlurPx, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
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
                    .padding(bottom = 32.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = dynamicQuote, color = contentColor.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val infiniteTransition = rememberInfiniteTransition(label = "")
                        val windRotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
                        val metrics = listOf(Pair(Icons.Default.FilterDrama, data.aqi), Pair(Icons.Default.NorthEast, data.wind), Pair(Icons.Default.Visibility, data.visibility), Pair(Icons.Default.WaterDrop, data.humidity))
                        metrics.forEach { (icon, label) ->
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp).graphicsLayer { if (settings.debugRotateWindSpeed && icon == Icons.Default.NorthEast) rotationZ = if (settings.animation) windRotation else 0f })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }


                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).graphicsLayer {
                    val prog = smoothProgress.value
                    val offset = scrollOffset.floatValue
                    val excessDpPx = (offset - 800f).coerceAtLeast(0f)
                    translationY = (680f - 540f * prog).dp.toPx() - excessDpPx
                }) {
                    HourlyForecastWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor)

                    Spacer(Modifier.height(12.dp))
                    DailyForecastWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor)

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { UVIndexWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                        Box(modifier = Modifier.weight(1f)) { FeelsLikeWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { PrecipitationWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                        Box(modifier = Modifier.weight(1f)) { HumidityWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                    }

                    Spacer(Modifier.height(12.dp))
                    MapWidget(data = data)

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { WindWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                        Box(modifier = Modifier.weight(1f)) { AirQualityWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { VisibilityWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                        Box(modifier = Modifier.weight(1f)) { PressureWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                    }

                    Spacer(Modifier.height(12.dp))
                    SunriseSunsetWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor)

                    Spacer(Modifier.height(12.dp))
                    RainfallWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor)

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { MoonPhaseWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                        Box(modifier = Modifier.weight(1f)) { AveragesWidget(data = data, widgetBg = widgetBg, contentColor = contentColor, secondaryContentColor = secondaryContentColor) }
                    }

                    Spacer(Modifier.height(140.dp))
                }
            }

            if (!settings.vulkan && settings.enableClouds) {
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

        Text(
            text = headerText, color = contentColor.copy(alpha = 0.9f), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                val prog = smoothProgress.value
                val scale = (18f + 10f * prog) / 28f
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = 32.dp.toPx()
                translationY = (130f - 70f * prog).dp.toPx()
                
                if (settings.blur) {
                    val offset = scrollOffset.floatValue
                    val pullBlurPx = if (offset < 0f) (-offset / 8f).coerceIn(0f, 15f).dp.toPx() else 0f
                    if (pullBlurPx > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlurPx, pullBlurPx, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                }
            }
        )

        if (settings.headerType != HeaderType.Disabled) {
            Text(text = "-", color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.graphicsLayer {
                val prog = smoothProgress.value
                alpha = prog.coerceIn(0f, 1f)
                val finalX = exactTitleWidthDp.toPx() + 8.dp.toPx()
                translationX = 32.dp.toPx() + (finalX * prog)
                translationY = (130f - 70f * prog).dp.toPx()
                
                if (settings.blur) {
                    val offset = scrollOffset.floatValue
                    val pullBlurPx = if (offset < 0f) (-offset / 8f).coerceIn(0f, 15f).dp.toPx() else 0f
                    if (pullBlurPx > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlurPx, pullBlurPx, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                }
            })
        }

        // ───  MATHEMATICAL ODOMETER ──────────────────────────────────────────────
        AnimatedOdometerText(
            temp = displayTemp,
            snapTo = forceSnap,
            style = TextStyle(fontSize = 130.sp, fontWeight = FontWeight.Bold, color = contentColor),
            animationEnabled = settings.animation,
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
                
                if (settings.blur) {
                    val offset = scrollOffset.floatValue
                    val pullBlurPx = if (offset < 0f) (-offset / 8f).coerceIn(0f, 15f).dp.toPx() else 0f
                    if (pullBlurPx > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlurPx, pullBlurPx, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                }
            }
        )

        var currentTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while(true) {
                delay(1000)
                currentTickMs = System.currentTimeMillis()
            }
        }

        val updateText by remember(data.lastUpdatedMs, settings.provider, settings.headerType, data.location) {
            derivedStateOf {
                val providerName = settings.provider.lowercase()
                val cityInfoStr = if (settings.headerType == HeaderType.Standard) "" else "${data.location.lowercase()} - "
                val elapsedSec = ((currentTickMs - data.lastUpdatedMs) / 1000).coerceAtLeast(0)

                val timeString = when {
                    data.lastUpdatedMs == 0L -> "Connecting..."
                    data.temp == null && data.lastUpdatedMs > 0L -> "Cached — refreshing..."
                    elapsedSec < 15 -> "Updated just now"
                    elapsedSec < 60 -> "Updated $elapsedSec seconds ago"
                    else -> "Updated at ${data.lastUpdated}"
                }

                "${cityInfoStr}$timeString from $providerName"
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.wrapContentWidth().graphicsLayer {
            val prog = smoothProgress.value
            translationY = (640f - 550f * prog).dp.toPx()
            val centerX = (screenWidthPx - size.width) / 2f
            val leftX = 32.dp.toPx()
            translationX = centerX + (leftX - centerX) * prog
            
            if (settings.blur) {
                val offset = scrollOffset.floatValue
                val pullBlurPx = if (offset < 0f) (-offset / 8f).coerceIn(0f, 15f).dp.toPx() else 0f
                if (pullBlurPx > 0f) renderEffect = android.graphics.RenderEffect.createBlurEffect(pullBlurPx, pullBlurPx, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
            }
        }) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(10.dp).graphicsLayer { rotationZ = 45f })
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = updateText, color = contentColor.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ───  ODOMETER COMPONENT ───────────────────────────────────────────────────

@Composable
fun AnimatedOdometerText(
    temp: Int?,
    style: TextStyle,
    modifier: Modifier = Modifier,
    snapTo: Int? = null,
    animationEnabled: Boolean = true
) {
    val isNull = temp == null
    val textStr = temp?.toString() ?: "--"
    val snapStr = snapTo?.toString()

    val maxLength = if (isNull) 2 else max(textStr.length, snapStr?.length ?: 0).coerceAtLeast(2)
    val paddedText = if (isNull) textStr else textStr.padStart(maxLength, '0')
    val paddedSnap = snapStr?.padStart(maxLength, '0')

    val maskModifier = modifier.graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.2f to Color.Black,
                0.8f to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

    Row(
        modifier = maskModifier.animateContentSize(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        paddedText.forEachIndexed { index, char ->
            val snapChar = paddedSnap?.getOrNull(index)

            DigitColumn(
                targetChar = char,
                snapChar = snapChar,
                style = style,
                animationEnabled = animationEnabled,
                delayMillis = index * 150
            )
        }
        Text("°", style = style)
    }
}

@Composable
private fun DigitColumn(
    targetChar: Char,
    snapChar: Char?,
    style: TextStyle,
    animationEnabled: Boolean,
    delayMillis: Int
) {
    val textMeasurer = rememberTextMeasurer()

    val charWidthPx = remember(style) {
        (0..9).maxOf { textMeasurer.measure(it.toString(), style).size.width }
    }
    val heightPx = remember(style) {
        textMeasurer.measure("0", style).size.height.toFloat()
    }

    if (!targetChar.isDigit()) {
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) { p.place(0, 0) }
            },
            contentAlignment = Alignment.Center
        ) {
            Text(targetChar.toString(), style = style)
        }
        return
    }

    val targetDigit = targetChar.digitToInt().toFloat()
    val anim = remember { Animatable(snapChar?.digitToIntOrNull()?.toFloat() ?: targetDigit) }

    LaunchedEffect(snapChar) {
        if (snapChar != null && snapChar.isDigit()) {
            anim.snapTo(snapChar.digitToInt().toFloat())
        }
    }

    LaunchedEffect(targetDigit, snapChar) {
        if (snapChar == null) {
            val currentMod = anim.value % 10f
            val currentWrapped = if (currentMod < 0) currentMod + 10f else currentMod
            var diff = targetDigit - currentWrapped

            if (diff > 5f) diff -= 10f
            if (diff < -5f) diff += 10f

            val finalTarget = anim.value + diff

            if (abs(anim.targetValue - finalTarget) > 0.01f) {
                if (animationEnabled) {
                    delay(delayMillis.toLong())
                    anim.animateTo(finalTarget, spring(dampingRatio = 0.95f, stiffness = 60f))
                } else {
                    anim.snapTo(finalTarget)
                }
            }
        }
    }

    Box(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(charWidthPx, placeable.height) {
                placeable.place((charWidthPx - placeable.width) / 2, 0)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Text("0", style = style, color = Color.Transparent)

        val currentValue = anim.value
        val minIdx = (floor(currentValue) - 2).toInt()
        val maxIdx = (ceil(currentValue) + 2).toInt()

        for (i in minIdx..maxIdx) {
            val displayDigit = (i % 10).let { if (it < 0) it + 10 else it }
            val offset = (i - currentValue) * heightPx
            val absOffset = abs(i - currentValue)

            val itemAlpha = (1f - (absOffset * 0.55f)).coerceIn(0f, 1f)
            val itemBlur = (absOffset * 10f).coerceIn(0f, 20f)

            if (itemAlpha > 0f) {
                Text(
                    text = displayDigit.toString(),
                    style = style,
                    modifier = Modifier.graphicsLayer {
                        translationY = offset
                        alpha = itemAlpha
                        if (itemBlur > 0.1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                itemBlur, itemBlur, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        } else {
                            renderEffect = null
                        }
                    }
                )
            }
        }
    }
}

//────────────────────────────────────────────────────────────────────────────

private fun getSkyColors(weather: WeatherType, state: VisualState): Pair<Color, Color> {
    return when (state) {
        VisualState.Sunrise -> when (weather) {
            WeatherType.Clear -> Color(0xFF6B728E) to Color(0xFFD6A587)
            WeatherType.Clouds -> Color(0xFF5C6479) to Color(0xFFA6968B)
            else -> Color(0xFF4A4E5B) to Color(0xFF7A736E)
        }
        VisualState.Day -> when (weather) {
            WeatherType.Clear -> Color(0xFF4B6E94) to Color(0xFF90B4CE)
            WeatherType.Clouds -> Color(0xFF6C7A89) to Color(0xFF95A5A6)
            WeatherType.Rain, WeatherType.Drizzle -> Color(0xFF4C5B66) to Color(0xFF6E7D88)
            WeatherType.Thunderstorm -> Color(0xFF34414A) to Color(0xFF4B5A66)
            else -> Color(0xFF7A8B99) to Color(0xFFA5B1BB)
        }
        VisualState.Afternoon -> when (weather) {
            WeatherType.Clear -> Color(0xFF3A5A80) to Color(0xFF8BA9C4)
            else -> Color(0xFF5D6D7E) to Color(0xFF85929E)
        }
        VisualState.Sunset -> when (weather) {
            WeatherType.Clear -> Color(0xFF384166) to Color(0xFFB57064)
            WeatherType.Clouds -> Color(0xFF3E4357) to Color(0xFF876A68)
            else -> Color(0xFF2C3242) to Color(0xFF5C4A4D)
        }
        VisualState.Evening -> when (weather) {
            WeatherType.Clear -> Color(0xFF1B2336) to Color(0xFF323B52)
            else -> Color(0xFF191E2B) to Color(0xFF2A313E)
        }
        VisualState.Night -> when (weather) {
            WeatherType.Clear -> Color(0xFF0C101A) to Color(0xFF151B26)
            else -> Color(0xFF0F121A) to Color(0xFF12151C)
        }
        else -> Color(0xFF4B6E94) to Color(0xFF90B4CE)
    }
}

private fun getCloudColor(weather: WeatherType, state: VisualState): Color {
    return when (state) {
        VisualState.Sunrise -> Color(0xFFD8B9AD)
        VisualState.Day -> if (weather == WeatherType.Clear) Color(0xFFE8ECEF) else Color(0xFFB0B9C2)
        VisualState.Afternoon -> Color(0xFFD6DFE8)
        VisualState.Sunset -> Color(0xFFA18281)
        VisualState.Evening -> Color(0xFF535C6D)
        VisualState.Night -> Color(0xFF2C3342)
        else -> Color(0xFFE8ECEF)
    }
}

