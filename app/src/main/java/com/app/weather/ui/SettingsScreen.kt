package com.app.weather.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import com.app.weather.R
import kotlinx.coroutines.delay

enum class SubNavType { Push, Pop, Instant }

@Composable
fun OdometerOverlayContent(
    settings: AppSettings,
    onTest: (from: Int, target: Int) -> Unit
) {
    val digitOptions = remember { (0..9).map { it.toString() } }

    var fromTens by remember { mutableIntStateOf(0) }
    var fromOnes by remember { mutableIntStateOf(0) }
    var targetTens by remember { mutableIntStateOf(0) }
    var targetOnes by remember { mutableIntStateOf(0) }

    fun getVal(tens: Int, ones: Int): Int {
        val combined = tens.toString() + ones.toString()
        return combined.toIntOrNull() ?: 0
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Animation, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
        Text("Odometer Animation", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Change it foor the temp on the front :3", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("FROM", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("TARGET", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)))

            Row(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f)) { WheelPicker(options = digitOptions, selectedIndex = fromTens, onIndexSelected = { fromTens = it }) }
                    Box(modifier = Modifier.weight(1f)) { WheelPicker(options = digitOptions, selectedIndex = fromOnes, onIndexSelected = { fromOnes = it }) }
                }

                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).align(Alignment.CenterVertically).background(Color.White.copy(alpha = 0.1f)))

                Row(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f)) { WheelPicker(options = digitOptions, selectedIndex = targetTens, onIndexSelected = { targetTens = it }) }
                    Box(modifier = Modifier.weight(1f)) { WheelPicker(options = digitOptions, selectedIndex = targetOnes, onIndexSelected = { targetOnes = it }) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { onTest(getVal(fromTens, fromOnes), getVal(targetTens, targetOnes)) }.padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Text("Test Animation", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ExperimentalOverlayContent(
    settings: AppSettings,
    currentWeather: WeatherType,
    onUpdate: (AppSettings) -> Unit,
    onWeatherSelect: (WeatherType) -> Unit
) {
    val weatherTypes = WeatherType.entries.toTypedArray()
    val visualStates = VisualState.entries.toTypedArray()

    var selectedWeather by remember { mutableIntStateOf(weatherTypes.indexOf(currentWeather).coerceAtLeast(0)) }
    var selectedVisual by remember { mutableIntStateOf(visualStates.indexOf(settings.visualStateOverride).coerceAtLeast(0)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
        Text("Experimental Menu", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Modify weather and time visual states", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("WEATHER", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("TIME OF DAY", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)))

            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(options = weatherTypes.map { it.title }, selectedIndex = selectedWeather, onIndexSelected = { selectedWeather = it })
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).align(Alignment.CenterVertically).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(options = visualStates.map { it.title }, selectedIndex = selectedVisual, onIndexSelected = { selectedVisual = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White)
                    .clickable {
                        onWeatherSelect(weatherTypes[selectedWeather])
                        onUpdate(settings.copy(visualStateOverride = visualStates[selectedVisual]))
                    }.padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Text("Apply Options", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun SettingsScreen(
    settings:            AppSettings,
    currentMenu:         String,
    onMenuChange:        (String) -> Unit,
    onUpdateSettings:    (AppSettings) -> Unit,
    onOpenOverlay:       (OverlayType) -> Unit,
    onBack:              () -> Unit,
    onSelectWeather:     (WeatherType) -> Unit = {},
    onTestOdometer:      (Int, Int) -> Unit = { _, _ -> }
) {
    val bgColor = MaterialTheme.colorScheme.background
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant
    var navType by remember { mutableStateOf(SubNavType.Push) }

    var displayedTitle by remember { mutableStateOf(
        when (currentMenu) {
            "DebugMenu" -> "Debug Menus"
            "General" -> "General"
            "Appearance" -> "Appearance"
            else -> "Settings"
        }
    ) }

    LaunchedEffect(currentMenu, navType) {
        val newTitle = when (currentMenu) {
            "DebugMenu" -> "Debug Menus"
            "General" -> "General"
            "Appearance" -> "Appearance"
            else -> "Settings"
        }
        displayedTitle = newTitle
    }

    val coroutineScope = rememberCoroutineScope()
    val swipeOffset   = remember { Animatable(0f) }
    var swipeBgMenu   by remember { mutableStateOf<String?>(null) }

    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    fun handleBack(requestedNavType: SubNavType = SubNavType.Pop) {
        if (currentMenu != "Main") { navType = requestedNavType; onMenuChange("Main") } else onBack()
    }

    BackHandler(enabled = currentMenu != "Main") { handleBack(SubNavType.Pop) }

    val mainListState       = rememberLazyListState()
    val generalListState    = rememberLazyListState()
    val appearanceListState = rememberLazyListState()
    val debugListState      = rememberLazyListState()

    @Composable
    fun MenuList(menuState: String, state: LazyListState, scrollable: Boolean, modifier: Modifier = Modifier) {
        val isMain = menuState == "Main"

        LazyColumn(
            state = state,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = if (isMain) 168.dp else 252.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = scrollable
        ) {
            when (menuState) {
                "Main" -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(50))
                                .background(cardBgColor),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Find some settings...",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF3B2323)).padding(vertical = 4.dp)) {
                            SettingsItemOverlay("Need permission", "Tap here to fix!", Icons.Outlined.WarningAmber, tint = Color(0xFFFF6B6B)) { onOpenOverlay(OverlayType.Permissions) }
                        }
                    }
                    item {
                        SettingsGroup {
                            SettingsItemOverlay("General", "general settings", Icons.Outlined.Settings) { navType = SubNavType.Push; onMenuChange("General") }
                            SettingsItemOverlay("Appearance", "appearance settings", Icons.Outlined.Palette) { navType = SubNavType.Push; onMenuChange("Appearance") }
                            SettingsItemOverlay("Notifications", "notification settings", Icons.Outlined.Notifications) {}
                            SettingsItemOverlay("Widgets", "widgets settings", Icons.Outlined.Widgets) {}
                            SettingsItemOverlay("Provider selector", "OpenWeather, BMKG, or Google", Icons.Outlined.CloudQueue) { onOpenOverlay(OverlayType.Provider) }
                            SettingsItemOverlay("Set your own API Keys", "if you prefer to not use our api keys", Icons.Outlined.VpnKey) {}
                            SettingsItemOverlay("Debug Menus", "well.. debug menus", Icons.Outlined.BugReport) { navType = SubNavType.Push; onMenuChange("DebugMenu") }
                            SettingsItemOverlay("Language", "english", Icons.Outlined.Translate) {}
                            SettingsItemOverlay("Credits", "made by roo, with <3", Icons.Outlined.Info) { onOpenOverlay(OverlayType.Credits) }
                        }
                    }
                }
                "General" -> {
                    item {
                        SettingsGroup {
                            SettingsItemOverlay("Temperature unit", "C", Icons.Outlined.Thermostat) {}
                            SettingsItemOverlay("Wind Speed unit", "Kilometers per hour ( Km/h )", Icons.Outlined.Air) {}
                            SettingsItemOverlay("Pressure Unit", "inches of mercury", Icons.Outlined.Compress) {}
                            SettingsItemOverlay("Visibility Unit", "Meters/kilometers", Icons.Outlined.Visibility) {}
                            SettingsItemOverlay("Refresh cycle", "${settings.refreshIntervalSec / 60}m ${settings.refreshIntervalSec % 60}s", Icons.AutoMirrored.Outlined.RotateRight) { onOpenOverlay(OverlayType.RefreshCycle) }
                            SettingsSwitch("Location Based Weather", "display weather based on the location youre in using IP address or GPS.", Icons.Outlined.LocationOn, settings.locationBasedWeather) { onUpdateSettings(settings.copy(locationBasedWeather = it)) }
                            if (settings.locationBasedWeather) {
                                SettingsSwitch("Precise Location", "Use high-accuracy GPS. may consume more battery.", Icons.Outlined.MyLocation, settings.preciseLocation) { onUpdateSettings(settings.copy(preciseLocation = it)) }
                            }
                        }
                    }
                }
                "Appearance" -> {
                    item {
                        SettingsGroup {
                            SettingsItemOverlay("Theme", settings.theme.name.lowercase(), Icons.Outlined.DarkMode) { onOpenOverlay(OverlayType.Theme) }
                            SettingsItemOverlay("Customize Quote", "Quote for the weather", Icons.Outlined.FormatQuote) { onOpenOverlay(OverlayType.Quote) }
                            SettingsItemOverlay("Customize Front page", "Quote for the front page", Icons.Outlined.Dashboard) { onOpenOverlay(OverlayType.Header) }
                            SettingsItemOverlay("Icons", "select icons that will be used for the app", Icons.Outlined.AppShortcut) { onOpenOverlay(OverlayType.Icons) }
                            SettingsItemOverlay("Customize Weather Widget", "Widget for the front page", Icons.Outlined.Widgets) {}
                            SettingsSwitch("Monet Engine", "dynamic color based on your wallpaper", Icons.Outlined.Palette, settings.monet) { onUpdateSettings(settings.copy(monet = it)) }
                            SettingsSwitch("Haptics", "haptics for the app", Icons.Outlined.Vibration, settings.haptics) { onUpdateSettings(settings.copy(haptics = it)) }
                            SettingsSwitch("Blur", "wide blur effects across the app", Icons.Outlined.BlurOn, settings.blur) { onUpdateSettings(settings.copy(blur = it)) }
                            SettingsSwitch("Animation", "animation across the app", Icons.Outlined.Animation, settings.animation) { onUpdateSettings(settings.copy(animation = it)) }
                            SettingsSwitch("FX", "FX across the app", Icons.Outlined.AutoAwesome, settings.fx) { onUpdateSettings(settings.copy(fx = it)) }
                        }
                    }
                }
                "DebugMenu" -> {
                    item {
                        SettingsGroup {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onSelectWeather(WeatherType.entries.toTypedArray().random()) }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Cycle Random Weather", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            SettingsSwitch("Enable Clouds", "Volumetric procedural cloud layer", Icons.Outlined.FilterDrama, settings.enableClouds) { onUpdateSettings(settings.copy(enableClouds = it)) }
                            SettingsSwitch("Rotate Wind Arrow", "Continously rotate the arrow on wind speed", Icons.AutoMirrored.Outlined.RotateRight, settings.debugRotateWindSpeed) { onUpdateSettings(settings.copy(debugRotateWindSpeed = it)) }
                            SettingsSwitch("Demo Mode", "Force static placeholder data for UI testing", Icons.Outlined.Science, settings.demoMode) { onUpdateSettings(settings.copy(demoMode = it)) }
                            SettingsSwitch("Vulkan Rendering", "Experimental Vulkan/WGPU background engine", Icons.Outlined.Speed, settings.vulkan) { onUpdateSettings(settings.copy(vulkan = it)) }
                        }
                    }
                    item { Text("Experimental Options", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) }
                    item {
                        SettingsGroup {
                            SettingsItemOverlay("Experimental Menu", "Modify weather and time visual states", Icons.Default.Science) { onOpenOverlay(OverlayType.Experimental) }
                            SettingsItemOverlay("Odometer Test", "Test the scrolling number", Icons.Outlined.Animation) { onOpenOverlay(OverlayType.Odometer) }
                        }
                    }
                    item {
                        SettingsGroup {
                            WeatherType.entries.forEach { type ->
                                SettingsItemOverlay(type.title, "force state", Icons.Outlined.Cloud) { onSelectWeather(type); handleBack(SubNavType.Pop) }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeScrollState = when (currentMenu) {
        "General" -> generalListState; "Appearance" -> appearanceListState; "DebugMenu" -> debugListState; else -> mainListState
    }

    val scrollOffset by remember(currentMenu) { derivedStateOf { if (activeScrollState.firstVisibleItemIndex == 0) activeScrollState.firstVisibleItemScrollOffset.toFloat() else 150f } }

    val isMain = currentMenu == "Main"
    val headerProgress = (scrollOffset / 150f).coerceIn(0f, 1f)

    val physicsProgress by animateFloatAsState(
        targetValue = headerProgress,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f), label = "liquid"
    )

    val titleScale = 1f - 0.4f * physicsProgress
    val titleX = (if (isMain) 0f else 88f * physicsProgress).dp
    val titleY = (
            if (isMain) {
                (56f * (1f - physicsProgress) + 4f * physicsProgress)
            } else {
                (100f * (1f - physicsProgress) + 4f * physicsProgress)
            }
            ).dp

    val iconX = (48f * physicsProgress).dp
    val iconY = (56f * (1f - physicsProgress)).dp
    val iconScale = 2.7f - 1.7f * physicsProgress

    val metaballModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            val blur = android.graphics.RenderEffect.createBlurEffect(12f, 12f, android.graphics.Shader.TileMode.DECAL)
            val matrix = android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 45f, -4500f
            ))
            renderEffect = android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(matrix), blur).asComposeRenderEffect()
            clip = false
        }
    } else Modifier

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        val internalGlassState = remember { GlassState() }

        if (swipeOffset.value > 0f && swipeBgMenu != null) {
            val parallaxX = (swipeOffset.value - screenWidthPx) * 0.3f
            val internalBgGlassState = remember { GlassState() }

            Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = parallaxX }) {
                val bgScrollState = when (swipeBgMenu) {
                    "General" -> generalListState; "Appearance" -> appearanceListState; "DebugMenu" -> debugListState; else -> mainListState
                }

                Box(modifier = Modifier.fillMaxSize().glassRoot(internalBgGlassState)) {
                    MenuList(swipeBgMenu!!, bgScrollState, scrollable = false)
                }

                val bgIsMain = swipeBgMenu == "Main"
                val bgScrollOffset = if (bgScrollState.firstVisibleItemIndex == 0) bgScrollState.firstVisibleItemScrollOffset.toFloat() else 150f
                val bgHeaderProgress = (bgScrollOffset / 150f).coerceIn(0f, 1f)

                val bgPhysicsProgress by animateFloatAsState(
                    targetValue = bgHeaderProgress,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f), label = "bg_liquid"
                )

                val bgExpandedHeight = if (bgIsMain) 160f else 240f
                val bgCollapsedHeight = 120f
                val bgCurrentHeight = bgExpandedHeight * (1f - bgPhysicsProgress) + bgCollapsedHeight * bgPhysicsProgress

                val bgTitleScale = 1f - 0.4f * bgPhysicsProgress
                val bgTitleX = (if (bgIsMain) 0f else 88f * bgPhysicsProgress).dp
                val bgTitleY = (
                        if (bgIsMain) {
                            (56f * (1f - bgPhysicsProgress) + 7f * bgPhysicsProgress)
                        } else {
                            (100f * (1f - bgPhysicsProgress) + 7f * bgPhysicsProgress)
                        }
                        ).dp

                val bgIconX = (48f * bgPhysicsProgress).dp
                val bgIconY = (56f * (1f - bgPhysicsProgress) + 0f * bgPhysicsProgress).dp
                val bgIconScale = 2.7f - 1.7f * bgPhysicsProgress

                Box(modifier = Modifier.fillMaxWidth().height(bgCurrentHeight.dp)) {
                    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        // Apply dynamically fading blur alpha
                        val bgBlurAlpha = bgPhysicsProgress.coerceIn(0f, 1f)
                        if (settings.blur && bgBlurAlpha > 0.01f) {
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = bgBlurAlpha)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    GlassPillBackground(state = internalBgGlassState, blurRadius = 25f, modifier = Modifier.fillMaxWidth().height((bgCurrentHeight * 0.3f).dp))
                                    GlassPillBackground(state = internalBgGlassState, blurRadius = 10f, modifier = Modifier.fillMaxWidth().height((bgCurrentHeight * 0.35f).dp))
                                    GlassPillBackground(state = internalBgGlassState, blurRadius = 5f, modifier = Modifier.fillMaxWidth().height((bgCurrentHeight * 0.35f).dp))
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to bgColor.copy(alpha = if (settings.blur) 0.95f else 0.95f),
                                0.5f to bgColor.copy(alpha = if (settings.blur) 0.90f else 0.90f),
                                0.8f to bgColor.copy(alpha = if (settings.blur) 0.40f else 0.8f),
                                1f to Color.Transparent
                            )
                        ))
                    }

                    Box(modifier = Modifier.matchParentSize().padding(top = 64.dp, start = 24.dp, end = 24.dp)) {
                        val bgMenuIcon = when (swipeBgMenu) {
                            "General" -> Icons.Outlined.Settings; "Appearance" -> Icons.Outlined.Palette; "DebugMenu" -> Icons.Outlined.BugReport; else -> null
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = swipeBgMenu != "Main",
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            val blobColor = MaterialTheme.colorScheme.onBackground
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = 0.15f }
                                    .then(metaballModifier)
                            ) {
                                val halfBox = 18.dp.toPx()
                                val radius  = 18.dp.toPx()
                                val backCX = halfBox
                                val backCY = halfBox
                                drawCircle(color = blobColor, radius = radius, center = Offset(backCX, backCY))
                                if (bgMenuIcon != null) {
                                    val icCX = bgIconX.toPx() + halfBox
                                    val icCY = bgIconY.toPx() + halfBox
                                    val dynamicRadius = radius * bgPhysicsProgress.coerceIn(0f, 1f)
                                    drawCircle(color = blobColor, radius = dynamicRadius, center = Offset(icCX, icCY))
                                    val bridgeStroke = 36.dp.toPx() * bgPhysicsProgress
                                    if (bridgeStroke > 0f && dynamicRadius > 0f) {
                                        drawLine(
                                            color = blobColor,
                                            start = Offset(backCX, backCY),
                                            end = Offset(icCX, icCY),
                                            strokeWidth = bridgeStroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = swipeBgMenu != "Main",
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = 40.dp.toPx()
                                        translationY = 10.dp.toPx()
                                        alpha = ((bgPhysicsProgress - 0.7f) * 3.33f).coerceIn(0f, 1f)
                                    }
                                    .width(2.dp).height(16.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                            )
                        }

                        if (bgMenuIcon != null) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = swipeBgMenu != "Main",
                                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationX = bgIconX.toPx()
                                            translationY = bgIconY.toPx()
                                        }
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(bgMenuIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp).graphicsLayer { scaleX = bgIconScale; scaleY = bgIconScale })
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = swipeBgMenu != "Main",
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
                            }
                        }

                        Text(
                            text = when(swipeBgMenu) { "General" -> "General"; "Appearance" -> "Appearance"; "DebugMenu" -> "Debug Menus"; else -> "Settings" },
                            color = MaterialTheme.colorScheme.onBackground, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                translationX = bgTitleX.toPx(); translationY = bgTitleY.toPx()
                                scaleX = bgTitleScale; scaleY = bgTitleScale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                        )
                    }
                }

                val shadowAlpha = 0.6f * (1f - (swipeOffset.value / screenWidthPx).coerceIn(0f, 1f))
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = shadowAlpha)))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                .background(bgColor)
                .pointerInput(currentMenu) {
                    if (currentMenu == "Main") return@pointerInput

                    var dragAccumulator = 0f
                    var isEdgeSwipe = false
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragAccumulator = 0f
                            isEdgeSwipe = offset.x < 200f
                            if (isEdgeSwipe) swipeBgMenu = "Main"
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (isEdgeSwipe) {
                                change.consume()
                                dragAccumulator += dragAmount
                                if (dragAccumulator > 0f) {
                                    coroutineScope.launch { swipeOffset.snapTo(dragAccumulator) }
                                }
                            }
                        },
                        onDragEnd = {
                            if (isEdgeSwipe) {
                                coroutineScope.launch {
                                    if (dragAccumulator > 150f) {
                                        swipeOffset.animateTo(screenWidthPx, tween(250, easing = LinearEasing))
                                        navType = SubNavType.Instant
                                        onMenuChange("Main")
                                        delay(32)
                                        swipeOffset.snapTo(0f)
                                        swipeBgMenu = null
                                    } else {
                                        swipeOffset.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 250f))
                                        swipeBgMenu = null
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                swipeOffset.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 250f))
                                swipeBgMenu = null
                            }
                        }
                    )
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().glassRoot(internalGlassState)) {
                AnimatedContent(
                    targetState = currentMenu,
                    label       = "SettingsMenuBase",
                    modifier    = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val slideCurve = spring<IntOffset>(dampingRatio = 0.82f, stiffness = 220f)
                        val fadeCurve = tween<Float>(durationMillis = 350)
                        val scaleCurve = spring<Float>(dampingRatio = 0.82f, stiffness = 220f)

                        when (navType) {
                            SubNavType.Push -> (slideInVertically(slideCurve) { it } + fadeIn(fadeCurve)) togetherWith
                                    (scaleOut(targetScale = 0.94f, animationSpec = scaleCurve) + fadeOut(fadeCurve))
                            SubNavType.Pop  -> (scaleIn(initialScale = 0.94f, animationSpec = scaleCurve) + fadeIn(fadeCurve)) togetherWith
                                    (slideOutVertically(slideCurve) { it } + fadeOut(fadeCurve))
                            SubNavType.Instant -> EnterTransition.None togetherWith ExitTransition.None
                        }
                    }
                ) { menuState ->
                    val state = when(menuState) { "General" -> generalListState; "Appearance" -> appearanceListState; "DebugMenu" -> debugListState; else -> mainListState }
                    MenuList(menuState, state, scrollable = true)
                }
            }

            val expandedHeight = if (isMain) 160f else 240f
            val collapsedHeight = 120f
            val currentHeight = expandedHeight * (1f - physicsProgress) + collapsedHeight * physicsProgress

            Box(modifier = Modifier.fillMaxWidth().height(currentHeight.dp)) {
                Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    // Apply dynamically fading blur alpha to foreground
                    val blurAlpha = physicsProgress.coerceIn(0f, 1f)
                    if (settings.blur && blurAlpha > 0.01f) {
                        Box(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = blurAlpha)) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                GlassPillBackground(state = internalGlassState, blurRadius = 25f, modifier = Modifier.fillMaxWidth().height((currentHeight * 0.3f).dp))
                                GlassPillBackground(state = internalGlassState, blurRadius = 10f, modifier = Modifier.fillMaxWidth().height((currentHeight * 0.35f).dp))
                                GlassPillBackground(state = internalGlassState, blurRadius = 5f, modifier = Modifier.fillMaxWidth().height((currentHeight * 0.35f).dp))
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to bgColor.copy(alpha = if (settings.blur) 0.95f else 0.95f),
                            0.5f to bgColor.copy(alpha = if (settings.blur) 0.90f else 0.90f),
                            0.8f to bgColor.copy(alpha = if (settings.blur) 0.40f else 0.8f),
                            1f to Color.Transparent
                        )
                    ))
                }

                Box(modifier = Modifier.matchParentSize().padding(top = 64.dp, start = 24.dp, end = 24.dp)) {
                    val menuIcon = when (currentMenu) {
                        "General" -> Icons.Outlined.Settings; "Appearance" -> Icons.Outlined.Palette; "DebugMenu" -> Icons.Outlined.BugReport; else -> null
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentMenu != "Main",
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        val blobColor = MaterialTheme.colorScheme.onBackground
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.15f }
                                .then(metaballModifier)
                        ) {
                            val halfBox = 18.dp.toPx()   
                            val radius  = 18.dp.toPx()

                            val backCX = halfBox
                            val backCY = halfBox
                            drawCircle(color = blobColor, radius = radius, center = Offset(backCX, backCY))

                            if (menuIcon != null) {
                                val icCX = iconX.toPx() + halfBox
                                val icCY = iconY.toPx() + halfBox
                                val dynamicRadius = radius * physicsProgress.coerceIn(0f, 1f)
                                drawCircle(color = blobColor, radius = dynamicRadius, center = Offset(icCX, icCY))
                                val bridgeStroke = 36.dp.toPx() * physicsProgress
                                if (bridgeStroke > 0f && dynamicRadius > 0f) {
                                    drawLine(
                                        color = blobColor,
                                        start = Offset(backCX, backCY),
                                        end = Offset(icCX, icCY),
                                        strokeWidth = bridgeStroke,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentMenu != "Main",
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = 40.dp.toPx()
                                    translationY = 10.dp.toPx()
                                    alpha = ((physicsProgress - 0.7f) * 3.33f).coerceIn(0f, 1f)
                                }
                                .width(2.dp).height(16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        )
                    }

                    if (menuIcon != null) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = currentMenu != "Main",
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = iconX.toPx()
                                        translationY = iconY.toPx()
                                    }
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    menuIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentMenu != "Main",
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { handleBack(SubNavType.Pop) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    key(navType) {
                        AnimatedContent(
                            targetState = displayedTitle,
                            label = "TitleTransition",
                            modifier = Modifier.graphicsLayer {
                                translationX    = titleX.toPx()
                                translationY    = titleY.toPx()
                                scaleX          = titleScale
                                scaleY          = titleScale
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                            transitionSpec = {
                                if (navType == SubNavType.Instant || navType == SubNavType.Pop) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    (slideInVertically(spring(stiffness = 300f)) { it } + fadeIn(tween(250))) togetherWith
                                            (slideOutVertically(spring(stiffness = 300f)) { -it } + fadeOut(tween(250)))
                                }
                            }
                        ) { title ->
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsItemOverlay(title: String, subtitle: String, icon: ImageVector, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (tint == Color.Unspecified) Color.White else tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsSwitch(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Gray,
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun WidgetPill(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeaderTypeSelectionContent(settings: AppSettings, onSelect: (HeaderType) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.71f)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 60.dp, top = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Headers type", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeaderGridCard("Location", isSelected = settings.headerType == HeaderType.Standard, Modifier.weight(1f)) { onSelect(HeaderType.Standard) }
                HeaderGridCard("weather type", isSelected = settings.headerType == HeaderType.Greeting, Modifier.weight(1f)) { onSelect(HeaderType.Greeting) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeaderGridCard("Feels like", isSelected = settings.headerType == HeaderType.FeelsLike, Modifier.weight(1f)) { onSelect(HeaderType.FeelsLike) }
                HeaderGridCard("disabled", isSelected = settings.headerType == HeaderType.Disabled, Modifier.weight(1f)) { onSelect(HeaderType.Disabled) }
            }
        }
    }
}

@Composable
fun OverlayContent(
    overlayType: OverlayType,
    settings: AppSettings,
    currentWeather: WeatherType = WeatherType.Clear,
    onUpdateSettings: (AppSettings) -> Unit,
    onOpenNested: (NestedOverlay) -> Unit,
    onWeatherSelect: (WeatherType) -> Unit = {},
    onTestOdometer: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.76f)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 60.dp, top = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(32.dp))

            when (overlayType) {
                OverlayType.Theme -> {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
                    Text("Theme", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("select theme that will be used across the app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ThemeCard("Light theme", Icons.Outlined.LightMode, settings.theme == AppTheme.Light) { onUpdateSettings(settings.copy(theme = AppTheme.Light)) }
                        ThemeCard("dark theme", Icons.Outlined.DarkMode, settings.theme == AppTheme.Dark) { onUpdateSettings(settings.copy(theme = AppTheme.Dark)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemeCard("Auto by system", Icons.Outlined.BrightnessAuto, settings.theme == AppTheme.Auto) { onUpdateSettings(settings.copy(theme = AppTheme.Auto)) }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onUpdateSettings(settings.copy(monet = !settings.monet)) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Dynamic colors based on wallpaper", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        androidx.compose.material3.Switch(
                            checked = settings.monet,
                            onCheckedChange = { onUpdateSettings(settings.copy(monet = it)) }
                        )
                    }
                }
                OverlayType.Quote -> {
                    Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
                    Text("Quote", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("The quote will be displayed for the weather.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    QuoteCard("Compact. short", "it feels like 21°C today,\nRaining until 10 PM. beware of flood", Icons.AutoMirrored.Outlined.ShortText, settings.quoteStyle == QuoteStyle.Compact) { onUpdateSettings(settings.copy(quoteStyle = QuoteStyle.Compact)) }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("or", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    QuoteCard("Summary", "its overcast and cloudy. broken clouds \non jakarta. visibility is 10km. the \ntemp feels like 21°C, quite \nhot. today rain probality is high", Icons.AutoMirrored.Outlined.Subject, settings.quoteStyle == QuoteStyle.Summary) { onUpdateSettings(settings.copy(quoteStyle = QuoteStyle.Summary)) }
                }
                OverlayType.Header -> {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Front page customisation", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    val typeName = when(settings.headerType) {
                        HeaderType.Standard -> "Location"
                        HeaderType.Greeting -> "weather type"
                        HeaderType.FeelsLike -> "Feels like"
                        HeaderType.Sunrise -> "Sunrise"
                        HeaderType.Disabled -> "disabled"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.3f), RoundedCornerShape(50))
                            .clickable { onOpenNested(NestedOverlay.HeaderTypeSelection) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(typeName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(60.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurface))
                            Box(modifier = Modifier.width(60.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurface))
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Box(modifier = Modifier.size(48.dp).border(8.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(30.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.5f)))
                        Box(modifier = Modifier.width(100.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.5f)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.2f), RoundedCornerShape(20.dp)).padding(16.dp)) {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetPill(Icons.Outlined.FilterDrama, "AQI")
                                WidgetPill(Icons.Outlined.Air, "km/h")
                                WidgetPill(Icons.Outlined.Visibility, "km")
                                WidgetPill(Icons.Outlined.WaterDrop, "%")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                    Text("+", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Hold the widget for 4s to remove, drag to change position", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                OverlayType.Icons -> {
                    Icon(Icons.Outlined.AppShortcut, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
                    Text("Icons", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("select icons that will be used for the app", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppIconCard("Light", R.drawable.iconslight, settings.appIcon == AppIcon.Day) { onUpdateSettings(settings.copy(appIcon = AppIcon.Day)) }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("or", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppIconCard("Dark", R.drawable.iconsdark, settings.appIcon == AppIcon.NightFullMoon) { onUpdateSettings(settings.copy(appIcon = AppIcon.NightFullMoon)) }
                        AppIconCard("Moon", R.drawable.iconsmoon, settings.appIcon == AppIcon.NightMoon) { onUpdateSettings(settings.copy(appIcon = AppIcon.NightMoon)) }
                    }
                }
                OverlayType.Permissions -> {
                    Icon(Icons.Outlined.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
                    Text("Permission", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("The app need these to work", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3/5", color = MaterialTheme.colorScheme.onSurface, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("permission are granted.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val reqPerm = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }

                    SettingsItemOverlay("Location", "", Icons.Outlined.LocationOn) { reqPerm() }
                    SettingsItemOverlay("background services", "", Icons.Outlined.SettingsSuggest) { reqPerm() }
                    SettingsItemOverlay("Battery Optimisation", "", Icons.Outlined.BatteryAlert) { reqPerm() }
                    SettingsItemOverlay("Networks", "", Icons.Outlined.Wifi) { reqPerm() }
                    SettingsItemOverlay("Notifications", "", Icons.Outlined.Notifications) { reqPerm() }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("We wont collect and store ANY data you given to us. the app only collect location data ( if permitted ) and ip address to display accurate location of the current weather. we will never collect and STORE any data.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 14.sp)
                }
                OverlayType.Credits -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(50.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Weatherify", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer))
                                Spacer(modifier = Modifier.width(20.dp))
                                Column {
                                    Text("BUILD 0.28", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                                    Text("Release-DEV-03", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f).height(180.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Image(
                                        painter = painterResource(id = R.drawable.profile_pic),
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(CircleShape)
                                    )
                                    Text("Made by idkroo\nwith <3", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Box(modifier = Modifier.weight(1f).height(180.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text("this app is\nopen source.\nforever yours.", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp)
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreditLinkItem("@realidkroo", Icons.Outlined.Link)
                            CreditLinkItem("@realidkroo", Icons.Outlined.Code)
                            CreditLinkItem("Visit this app repo", Icons.Outlined.Code)
                        }
                    }
                }
                OverlayType.Experimental -> ExperimentalOverlayContent(
                    settings = settings,
                    currentWeather = currentWeather,
                    onUpdate = onUpdateSettings,
                    onWeatherSelect = onWeatherSelect
                )
                OverlayType.Odometer -> OdometerOverlayContent(
                    settings = settings,
                    onTest = onTestOdometer
                )
                OverlayType.Provider -> {
                    Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
                    Text("Weather Provider", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Select the weather provider for the app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(32.dp))

                    ProviderCard("OpenWeather", "Global coverage", Icons.Outlined.Language, settings.provider == "OpenWeather") {
                        onUpdateSettings(settings.copy(provider = "OpenWeather"))
                        WeatherBackend.setProvider("OpenWeather")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ProviderCard("BMKG", "Indonesia only", Icons.Outlined.Map, settings.provider == "BMKG") {
                        onUpdateSettings(settings.copy(provider = "BMKG"))
                        WeatherBackend.setProvider("BMKG")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ProviderCard("Google Weather API", "Global coverage", Icons.Outlined.CloudCircle, settings.provider == "Google") {
                        onUpdateSettings(settings.copy(provider = "Google"))
                        WeatherBackend.setProvider("Google")
                    }
                }
                OverlayType.RefreshCycle -> RefreshCycleOverlayContent(settings, onUpdateSettings)
                OverlayType.None -> {}
            }
        }
    }
}

@Composable
fun RefreshCycleOverlayContent(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    val minuteOptions = remember { (0..59).map { "${it}m" } }

    var selectedMinute by remember { mutableIntStateOf(settings.refreshIntervalSec / 60) }
    var selectedSecond by remember { mutableIntStateOf(settings.refreshIntervalSec % 60) }

    val secondOptions = remember(selectedMinute) {
        if (selectedMinute == 0) (30..59).map { "${it}s" }
        else (0..59).map { "${it}s" }
    }

    LaunchedEffect(selectedMinute) {
        if (selectedMinute == 0 && selectedSecond < 30) {
            selectedSecond = 30
        }
    }

    LaunchedEffect(selectedMinute, selectedSecond) {
        val totalSec = if (selectedMinute == 0) {
            selectedSecond.coerceIn(30, 59)
        } else {
            selectedMinute * 60 + selectedSecond
        }
        if (totalSec != settings.refreshIntervalSec) {
            onUpdate(settings.copy(refreshIntervalSec = totalSec))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp).padding(bottom = 12.dp))
        Text("Refresh Cycle", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Set how often the weather data refreshes in the background. Min 30s.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("MINUTES", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("SECONDS", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)))

            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    WheelPicker(options = minuteOptions, selectedIndex = selectedMinute, onIndexSelected = { selectedMinute = it })
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).align(Alignment.CenterVertically).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.weight(1f)) {
                    val displayIndex = if (selectedMinute == 0) (selectedSecond - 30).coerceAtLeast(0) else selectedSecond
                    WheelPicker(options = secondOptions, selectedIndex = displayIndex, onIndexSelected = {
                        selectedSecond = if (selectedMinute == 0) it + 30 else it
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppIconCard(title: String, drawableId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp).height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = null,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).align(Alignment.TopStart)
        )
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun ThemeCard(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp).height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).align(Alignment.TopStart), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        }
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun QuoteCard(title: String, preview: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(preview, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
fun HeaderGridCard(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).align(Alignment.TopStart)
        )
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun CreditLinkItem(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { /* Handle click */ }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProviderCard(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}