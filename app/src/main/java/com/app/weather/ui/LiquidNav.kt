package com.app.weather.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.abs

private fun hapticTick(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}

private fun hapticConfirm(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}


@Composable
fun LiquidGlassNavBar(
    modifier:          Modifier,
    barBackground:     Color,
    weatherType:       WeatherType,
    activeDestination: Destination,
    glassState:        GlassState,
    onWeatherCycle:    () -> Unit,
    onNavigate:        (Destination) -> Unit
) {
    val density = LocalDensity.current
    val view    = LocalView.current
    val settings = LocalAppSettings.current

    val pillX = remember { SpringFloat(0f) }
    val pillW = remember { SpringFloat(200f) }
    val barSX = remember { SpringFloat(1f) }
    val barSY = remember { SpringFloat(1f) }
    val iconS = remember { Array(3) { SpringFloat(1f) } }

    var pillXpx    by remember { mutableFloatStateOf(0f) }
    var pillWpx    by remember { mutableFloatStateOf(200f) }
    var barSXState by remember { mutableFloatStateOf(1f) }
    var barSYState by remember { mutableFloatStateOf(1f) }
    var barOrigX   by remember { mutableFloatStateOf(0.5f) }
    val iconSt     = remember { Array(3) { mutableFloatStateOf(1f) } }

    val tabBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    val currentTabIndex = when (activeDestination) {
        Destination.Weather  -> 0
        Destination.Search   -> 1
        Destination.Settings -> 2
    }
    var activeTab  by remember { mutableIntStateOf(currentTabIndex) }

    var isDragging   by remember { mutableStateOf(false) }
    var dragTotalX   by remember { mutableFloatStateOf(0f) }
    var dragTotalY   by remember { mutableFloatStateOf(0f) }
    var dragVelX     by remember { mutableFloatStateOf(0f) }
    var dragVelY     by remember { mutableFloatStateOf(0f) }
    var atLeftEdge   by remember { mutableStateOf(false) }
    var atRightEdge  by remember { mutableStateOf(false) }
    var dragOriginX  by remember { mutableFloatStateOf(0.5f) }

    var lastHapticTab by remember { mutableIntStateOf(currentTabIndex) }

    fun snapPillTo(index: Int) {
        tabBounds[index]?.let { (l, w) -> pillX.target = l; pillW.target = w }
    }

    fun nearestTab(xPx: Float): Int {
        var best = 0; var bestDist = Float.MAX_VALUE
        tabBounds.forEach { (i, p) ->
            val d = abs(xPx - (p.first + p.second / 2f))
            if (d < bestDist) { bestDist = d; best = i }
        }
        return best
    }

    LaunchedEffect(currentTabIndex) {
        if (!isDragging) { activeTab = currentTabIndex; snapPillTo(currentTabIndex) }
    }
    LaunchedEffect(tabBounds.size) { if (tabBounds.size == 3) snapPillTo(activeTab) }

    LaunchedEffect(Unit) {
        var lastMs = 0L
        while (isActive) {
            withInfiniteAnimationFrameMillis { ms ->
                val dt = if (lastMs == 0L) 0.016f else ((ms - lastMs) / 1000f).coerceIn(0f, 0.05f)
                lastMs = ms

                if (isDragging) {
                    val hMag = abs(dragTotalX)
                    val vMag = abs(dragTotalY)
                    barSX.target = if (atLeftEdge || atRightEdge) (1f + hMag * 0.0015f).coerceIn(1f, 1.20f) else (1f - hMag * 0.00015f).coerceIn(0.97f, 1f)
                    barSY.target = (1f + vMag * 0.0013f).coerceIn(1f, 1.14f)
                    barOrigX = dragOriginX
                } else {
                    barSX.target = 1f; barSY.target = 1f
                }

                pillX.step(dt, stiffness = 340f, damping = 24f, animationEnabled = settings.animation)
                pillW.step(dt, stiffness = 340f, damping = 24f, animationEnabled = settings.animation)
                barSX.step(dt, stiffness = 100f, damping = 10f, animationEnabled = settings.animation)
                barSY.step(dt, stiffness = 100f, damping = 10f, animationEnabled = settings.animation)

                for (i in 0..2) {
                    iconS[i].step(dt, stiffness = 380f, damping = 20f, animationEnabled = settings.animation)
                    iconSt[i].floatValue = iconS[i].value
                }

                pillXpx = pillX.value; pillWpx = pillW.value; barSXState = barSX.value; barSYState = barSY.value
            }
        }
    }

    val weatherIcon = when (weatherType) {
        WeatherType.Clear                             -> Icons.Default.WbSunny
        WeatherType.Clouds, WeatherType.Mist,
        WeatherType.Fog, WeatherType.Haze,
        WeatherType.Smoke                             -> Icons.Default.FilterDrama
        WeatherType.Rain, WeatherType.Drizzle         -> Icons.Default.WaterDrop
        WeatherType.Thunderstorm, WeatherType.Snow,
        WeatherType.Tornado, WeatherType.Squall       -> Icons.Default.Thunderstorm
        else                                          -> Icons.Default.FilterDrama
    }
    
    val blurAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(weatherType) {
        if (settings.animation) {
            blurAnim.animateTo(10f, animationSpec = tween(150))
            blurAnim.animateTo(0f,  animationSpec = tween(150))
        } else {
            blurAnim.snapTo(0f)
        }
    }

    val outsetPx    = with(density) { 0.dp.toPx() } 
    val pillCrnrPx  = with(density) { 100.dp.toPx() }
    val hPadPx      = with(density) { 8.dp.toPx() }
    val vPadPx      = with(density) { 6.dp.toPx() }
    
    val pillFill = Color.White.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = barSXState
                scaleY = barSYState
                transformOrigin = TransformOrigin(barOrigX, 0.5f)
            }
    ) {
        // Native RenderNode 0-latency background blur
        // Binds directly to the GPU display list of the root layout!
        GlassPillBackground(
            state = glassState,
            blurRadius = 24f,
            tint = barBackground.copy(alpha = 0.35f),
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.matchParentSize()
        )

        // Pill indicator + icons
        Box(
            modifier = Modifier
                .drawWithCache {
                    onDrawBehind {
                        val px = pillXpx + hPadPx - outsetPx
                        val py = vPadPx  - outsetPx
                        val pw = pillWpx + outsetPx * 2f
                        val ph = (size.height - vPadPx * 2f) + outsetPx * 2f
                        drawRoundRect(
                            color = pillFill, 
                            topLeft = Offset(px, py), size = Size(pw, ph), 
                            cornerRadius = CornerRadius(pillCrnrPx)
                        )
                    }
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true; dragTotalX = 0f; dragTotalY = 0f; dragVelX = 0f; dragVelY = 0f; atLeftEdge = false; atRightEdge = false; dragOriginX = 0.5f
                            lastHapticTab = nearestTab(offset.x)
                            hapticTick(view)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragTotalX += dragAmount.x; dragTotalY += dragAmount.y; dragVelX = dragAmount.x; dragVelY = dragAmount.y
                            val nearestIdx = nearestTab(pillX.value + pillW.value / 2f)
                            atLeftEdge = nearestIdx == 0 && dragTotalX < 0f
                            atRightEdge = nearestIdx == 2 && dragTotalX > 0f
                            dragOriginX = when { dragTotalX > 20f -> 0.05f; dragTotalX < -20f -> 0.95f; else -> 0.5f }
                            val draggedIdx = nearestTab(change.position.x)
                            snapPillTo(draggedIdx)
                            if (draggedIdx != lastHapticTab) { hapticTick(view); lastHapticTab = draggedIdx }
                        },
                        onDragEnd = {
                            isDragging = false; atLeftEdge = false; atRightEdge = false; dragTotalX = 0f; dragTotalY = 0f
                            barSX.impulse(-dragVelX * 0.020f); barSY.impulse(-dragVelY * 0.016f)
                            val nearestIdx = nearestTab(pillX.value + pillW.value / 2f)
                            activeTab = nearestIdx; snapPillTo(nearestIdx)
                            hapticConfirm(view)
                            when (nearestIdx) { 0 -> onNavigate(Destination.Weather); 1 -> onNavigate(Destination.Search); 2 -> onNavigate(Destination.Settings) }
                        },
                        onDragCancel = {
                            isDragging = false; atLeftEdge = false; atRightEdge = false; dragTotalX = 0f; dragTotalY = 0f; activeTab = currentTabIndex; snapPillTo(activeTab)
                        }
                    )
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NavTabItem(
                    index = 0, iconScaleProvider = { iconSt[0].floatValue }, onBoundsReady = { l, w -> tabBounds[0] = Pair(l, w) },
                    onClick = { hapticTick(view); onNavigate(Destination.Weather); iconS[0].impulse(8f) },
                    onHoverChange = { h -> iconS[0].target = if (h) 1.2f else 1f }
                ) {
                    Crossfade(targetState = weatherIcon, animationSpec = if (settings.animation) tween(300) else snap(), label = "") { icon ->
                        Icon(imageVector = icon, contentDescription = "Weather", tint = Color.White, modifier = Modifier.size(24.dp).blur(if (settings.blur) blurAnim.value.dp else 0.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded))
                    }
                }

                NavTabItem(
                    index = 1, iconScaleProvider = { iconSt[1].floatValue }, onBoundsReady = { l, w -> tabBounds[1] = Pair(l, w) },
                    onClick = { hapticTick(view); onNavigate(Destination.Search); iconS[1].impulse(8f) },
                    onHoverChange = { h -> iconS[1].target = if (h) 1.2f else 1f }
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = if (activeTab == 1) 1f else 0.65f), modifier = Modifier.size(24.dp))
                }

                NavTabItem(
                    index = 2, iconScaleProvider = { iconSt[2].floatValue }, onBoundsReady = { l, w -> tabBounds[2] = Pair(l, w) },
                    onClick = { hapticTick(view); onNavigate(Destination.Settings); iconS[2].impulse(8f) },
                    onHoverChange = { h -> iconS[2].target = if (h) 1.2f else 1f }
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = if (activeTab == 2) 1f else 0.65f), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(index: Int, iconScaleProvider: () -> Float, onBoundsReady: (leftPx: Float, widthPx: Float) -> Unit, onClick: () -> Unit, onHoverChange: (Boolean) -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(isPressed) { onHoverChange(isPressed) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .onGloballyPositioned { coords -> val off = coords.positionInParent(); onBoundsReady(off.x, coords.size.width.toFloat()) }
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 10.dp)
            .graphicsLayer { 
                val scale = iconScaleProvider()
                scaleX = scale; scaleY = scale 
            }
    ) { content() }
}