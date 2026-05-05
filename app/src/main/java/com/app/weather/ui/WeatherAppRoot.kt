package com.app.weather.ui

import android.Manifest
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.app.weather.ui.theme.WeatherAppTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Destination { Weather, Search, Settings } 
enum class AppTheme { Light, Dark, Auto }
enum class QuoteStyle { Compact, Summary }
enum class HeaderType { Greeting, FeelsLike, Sunrise, Disabled, Standard }
enum class AppIcon { Day, NightFullMoon, NightMoon }
enum class OverlayType { None, Theme, Quote, Header, Icons, Permissions, Credits, Provider, Experimental, Odometer, RefreshCycle }
enum class NestedOverlay { None, HeaderTypeSelection }
enum class NavType { Tab, Push, Pop, Instant }

enum class VisualState(val title: String) {
    Automatic("Automatic"),
    Sunrise("Sunrise"),
    Day("Day"),
    Afternoon("Afternoon"),
    Sunset("Sunset"),
    Evening("Evening"),
    Night("Night")
}

data class AppSettings(
    val theme:        AppTheme   = AppTheme.Dark,
    val monet:        Boolean    = false,
    val haptics:      Boolean    = true,
    val blur:         Boolean    = true,
    val animation:    Boolean    = true,
    val fx:           Boolean    = true,
    val quoteStyle:   QuoteStyle = QuoteStyle.Compact,
    val headerType:   HeaderType = HeaderType.Standard,
    val appIcon:      AppIcon    = AppIcon.Day,
    val enableClouds: Boolean    = false,
    val debugRotateWindSpeed: Boolean = false,
    val provider:     String     = "OpenWeather",
    val locationBasedWeather: Boolean = true,
    val preciseLocation: Boolean = true,
    val demoMode:     Boolean    = false,
    val vulkan:       Boolean    = false,
    val visualStateOverride: VisualState = VisualState.Automatic,
    val refreshIntervalSec: Int = 30
)

val LocalAppSettings = compositionLocalOf { AppSettings() }

@SuppressLint("MissingPermission")
@Composable
fun WeatherAppRoot() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    var backStack by remember { mutableStateOf(listOf(Destination.Weather)) }
    var forwardStack by remember { mutableStateOf(listOf<Destination>()) }
    val currentDestination = backStack.last()
    var navType by remember { mutableStateOf(NavType.Tab) }

    var settingsMenu by remember { mutableStateOf("Main") }

    var weatherData by remember {
        mutableStateOf(
            WeatherCache.load(context) ?: WeatherData.Default
        )
    }
    var settings by remember { mutableStateOf(SettingsPersistence.load(context)) }

    LaunchedEffect(settings) {
        SettingsPersistence.save(context, settings)
    }

    var activeOverlay by remember { mutableStateOf(OverlayType.None) }
    var displayedOverlay by remember { mutableStateOf(OverlayType.None) }
    val overlayProgress = remember { Animatable(0f) }
    var primaryOverlayHeightPx by remember { mutableStateOf(0f) }

    var activeNestedOverlay by remember { mutableStateOf(NestedOverlay.None) }
    val stackedOverlayProgress = remember { Animatable(0f) }
    var secondaryOverlayHeightPx by remember { mutableStateOf(0f) }

    val swipeOffset = remember { Animatable(0f) }
    var swipeBgDest by remember { mutableStateOf<Destination?>(null) }

    val glassState = remember { GlassState() }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // ─── Odometer Test Engine Variables ───
    var testOdometerFrom by remember { mutableStateOf<Int?>(null) }
    var testOdometerTarget by remember { mutableStateOf<Int?>(null) }
    var testOdometerTrigger by remember { mutableIntStateOf(0) }

    fun loadWeatherForLocation(lat: Double?, lon: Double?) {
        coroutineScope.launch {
            val res = if (lat != null && lon != null)
                WeatherBackend.fetchWeatherByLocation(lat, lon)
            else
                WeatherBackend.fetchWeather("Jakarta")
            weatherData = res
            WeatherCache.save(context, res)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            val priority = if (settings.preciseLocation) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            fusedLocationClient.getCurrentLocation(priority, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) loadWeatherForLocation(loc.latitude, loc.longitude)
                    else loadWeatherForLocation(null, null)
                }
                .addOnFailureListener { loadWeatherForLocation(null, null) }
        } else loadWeatherForLocation(null, null)
    }

    fun refreshWeather() {
        if (settings.demoMode) {
            weatherData = WeatherData.Default.copy(
                location = "Demo City",
                description = "Demo Mode Active",
                temp = 24,
                feelsLike = 26,
                humidity = "65%",
                wind = "12 km/h",
                rainProb = "Low",
                lastUpdated = "Just now",
                lastUpdatedMs = System.currentTimeMillis(),
                type = WeatherType.Clear
            )
            return
        }
        if (settings.locationBasedWeather) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val priority = if (settings.preciseLocation) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                fusedLocationClient.getCurrentLocation(
                    priority,
                    null
                )
                    .addOnSuccessListener { loc ->
                        if (loc != null) loadWeatherForLocation(loc.latitude, loc.longitude)
                        else loadWeatherForLocation(null, null)
                    }
                    .addOnFailureListener { loadWeatherForLocation(null, null) }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            loadWeatherForLocation(null, null)
        }
    }

    LaunchedEffect(
        settings.locationBasedWeather,
        settings.demoMode,
        settings.provider
    ) { refreshWeather() }

    LaunchedEffect(settings.refreshIntervalSec) {
        while(isActive) {
            val delaySecs = settings.refreshIntervalSec.coerceAtLeast(5)
            delay(delaySecs * 1000L)
            refreshWeather()
        }
    }

    val dynamicBarBg by animateColorAsState(
        targetValue = if (currentDestination != Destination.Weather) {
            Color.Black.copy(alpha = 0.45f)
        } else when (weatherData.type) {
            WeatherType.Clear, WeatherType.Clouds, WeatherType.Snow -> Color.Black.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(500), label = ""
    )

    fun handleBack(requestedNavType: NavType = NavType.Pop) {
        if (activeNestedOverlay != NestedOverlay.None) {
            activeNestedOverlay = NestedOverlay.None
        } else if (activeOverlay != OverlayType.None) {
            activeOverlay = OverlayType.None
        } else if (currentDestination == Destination.Settings && settingsMenu != "Main") {
        } else if (backStack.size > 1) {
            navType = requestedNavType
            forwardStack = listOf(backStack.last()) + forwardStack
            backStack = backStack.dropLast(1)
        } else {
            activity?.finish()
        }
    }

    BackHandler(enabled = backStack.size > 1 || activeOverlay != OverlayType.None) {
        handleBack(
            NavType.Pop
        )
    }

    LaunchedEffect(activeOverlay, activeNestedOverlay) {
        val smoothSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 220f)
        
        when {
            activeNestedOverlay != NestedOverlay.None -> stackedOverlayProgress.animateTo(
                1f,
                smoothSpring
            )

            activeOverlay != OverlayType.None -> {
                if (stackedOverlayProgress.value > 0f) stackedOverlayProgress.animateTo(
                    0f,
                    smoothSpring
                )
                displayedOverlay = activeOverlay
                overlayProgress.animateTo(1f, smoothSpring)
            }

            else -> {
                launch {
                    stackedOverlayProgress.animateTo(
                        0f,
                        smoothSpring
                    )
                }
                overlayProgress.animateTo(0f, smoothSpring)
                displayedOverlay = OverlayType.None
            }
        }
    }

    val renderDestination: @Composable (Destination) -> Unit = { dest ->
        when (dest) {
            Destination.Weather -> MainWeatherScreen(
                data = weatherData,
                settings = settings,
                onRefresh = { refreshWeather() },
                testOdometerFrom = testOdometerFrom,
                testOdometerTarget = testOdometerTarget,
                testOdometerTrigger = testOdometerTrigger
            )

            Destination.Search -> SearchScreen(onBack = { handleBack(NavType.Pop) })
            Destination.Settings -> SettingsScreen(
                settings = settings, currentMenu = settingsMenu,
                onSelectWeather = {
                    weatherData =
                        weatherData.copy(type = it, description = "forced ${it.title.lowercase()}")
                },
                onMenuChange = { settingsMenu = it }, onUpdateSettings = { settings = it },
                onOpenOverlay = { activeOverlay = it }, onBack = { handleBack(NavType.Pop) },
                onTestOdometer = { from, target ->
                    // Set test parameters
                    testOdometerFrom = from
                    testOdometerTarget = target
                    testOdometerTrigger++

                    // Route back to Main Screen instantly
                    navType = NavType.Tab
                    backStack = listOf(Destination.Weather)
                    forwardStack = emptyList()
                    settingsMenu = "Main"
                    activeOverlay = OverlayType.None
                }
            )
        }
    }

    val darkTheme = when (settings.theme) {
        AppTheme.Light -> false; AppTheme.Dark -> true; AppTheme.Auto -> isSystemInDarkTheme()
    }

    val isSwiping by remember { derivedStateOf { swipeOffset.value != 0f && swipeBgDest != null } }
    val isPrimaryOpen by remember { derivedStateOf { displayedOverlay != OverlayType.None || overlayProgress.value > 0f } }
    val isStackedOpen by remember { derivedStateOf { activeNestedOverlay != NestedOverlay.None || stackedOverlayProgress.value > 0f } }

    WeatherAppTheme(darkTheme = darkTheme, dynamicColor = settings.monet) {
        CompositionLocalProvider(LocalAppSettings provides settings) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val oProg = overlayProgress.value
                            val sProg = stackedOverlayProgress.value
                            val scale = 1f - 0.08f * oProg - 0.05f * sProg
                            scaleX = scale
                            scaleY = scale

                            clip = true
                            // 1. SAFEGUARD: Coerce shape radius to be at least 0f to prevent crash
                            val safeRadius = (32f * oProg + 16f * sProg).coerceAtLeast(0f)
                            shape = RoundedCornerShape(safeRadius.dp.toPx())

                            if (settings.blur) {
                                val blurPx = (12f * oProg + 8f * sProg).dp.toPx()
                                // 2. SAFEGUARD: Ensure blur radius is strictly positive
                                if (blurPx > 0.1f) {
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        blurPx,
                                        blurPx,
                                        android.graphics.Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                } else renderEffect = null
                            }
                        }
                ) {
                    if (isSwiping) {
                        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                            val offset = swipeOffset.value
                            translationX =
                                if (offset > 0f) (offset - screenWidthPx) * 0.3f else (offset + screenWidthPx) * 0.3f
                        }) {
                            renderDestination(swipeBgDest!!)
                            Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                                val progress =
                                    (kotlin.math.abs(swipeOffset.value) / screenWidthPx).coerceIn(
                                        0f,
                                        1f
                                    )
                                alpha = 0.6f * (1f - progress)
                            }.background(Color.Black))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = swipeOffset.value }
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(currentDestination, settingsMenu) {
                                var dragAccumulator = 0f
                                var isEdgeSwipe = false
                                var swipeDirection = 0

                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        dragAccumulator = 0f
                                        val canRootSwipeBack =
                                            backStack.size > 1 && !(currentDestination == Destination.Settings && settingsMenu != "Main")
                                        when {
                                            offset.x < 200f && canRootSwipeBack -> {
                                                isEdgeSwipe = true; swipeDirection =
                                                    1; swipeBgDest =
                                                    backStack[backStack.lastIndex - 1]
                                            }

                                            offset.x > size.width - 200f && forwardStack.isNotEmpty() && currentDestination != Destination.Weather -> {
                                                isEdgeSwipe = true; swipeDirection =
                                                    -1; swipeBgDest = forwardStack.first()
                                            }

                                            else -> isEdgeSwipe = false
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        if (isEdgeSwipe) {
                                            dragAccumulator += dragAmount
                                            if ((swipeDirection == 1 && dragAccumulator > 0) || (swipeDirection == -1 && dragAccumulator < 0)) coroutineScope.launch {
                                                swipeOffset.snapTo(
                                                    dragAccumulator
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isEdgeSwipe) {
                                            coroutineScope.launch {
                                                if (swipeDirection == 1 && dragAccumulator > 150f) {
                                                    swipeOffset.animateTo(
                                                        screenWidthPx,
                                                        tween(200, easing = LinearEasing)
                                                    )
                                                    handleBack(NavType.Instant); delay(32); swipeOffset.snapTo(
                                                        0f
                                                    ); swipeBgDest = null
                                                } else if (swipeDirection == -1 && dragAccumulator < -150f) {
                                                    swipeOffset.animateTo(
                                                        -screenWidthPx,
                                                        tween(200, easing = LinearEasing)
                                                    )
                                                    navType = NavType.Instant;
                                                    val next = forwardStack.first(); forwardStack =
                                                        forwardStack.drop(1); backStack =
                                                        backStack + next
                                                    delay(32); swipeOffset.snapTo(0f); swipeBgDest =
                                                        null
                                                } else {
                                                    swipeOffset.animateTo(
                                                        0f,
                                                        spring(stiffness = 300f)
                                                    ); swipeBgDest = null
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            swipeOffset.animateTo(
                                                0f,
                                                spring(stiffness = 300f)
                                            ); swipeBgDest = null
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().glassRoot(glassState)) {
                            AnimatedContent(
                                targetState = currentDestination,
                                label = "AppNavigation",
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    if (navType == NavType.Tab) {
                                        (fadeIn(
                                            animationSpec = tween(
                                                400,
                                                delayMillis = 50
                                            )
                                        ) + scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = 300f
                                            )
                                        )) togetherWith
                                                (fadeOut(animationSpec = tween(300)) + scaleOut(
                                                    targetScale = 1.08f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = 300f
                                                    )
                                                ))
                                    } else {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    }
                                }
                            ) { destination -> renderDestination(destination) }
                        }
                    }
                }

                LiquidGlassNavBar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                    barBackground = dynamicBarBg,
                    weatherType = weatherData.type,
                    activeDestination = currentDestination,
                    glassState = glassState,
                    onWeatherCycle = {},
                    onNavigate = { dest ->
                        if (dest != currentDestination) {
                            navType = NavType.Tab; forwardStack = emptyList(); settingsMenu = "Main"
                            backStack =
                                if (dest == Destination.Weather) listOf(Destination.Weather) else backStack + dest
                        }
                    }
                )

                if (isStackedOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 3. SAFEGUARD: Coerce alpha values directly
                            .graphicsLayer { alpha = stackedOverlayProgress.value.coerceIn(0f, 1f) }
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }

                if (isPrimaryOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 4. SAFEGUARD: Coerce alpha values directly
                            .graphicsLayer { alpha = overlayProgress.value.coerceIn(0f, 1f) }
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { handleBack() }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onGloballyPositioned {
                                primaryOverlayHeightPx = it.size.height.toFloat()
                            }
                            .graphicsLayer {
                                val oProg = overlayProgress.value
                                val sProg = stackedOverlayProgress.value.coerceIn(0f, 1f)
                                // 5. SAFEGUARD: Apply safeguards to container scales and alphas
                                scaleX = 1f - 0.06f * sProg
                                scaleY = 1f - 0.06f * sProg
                                translationY = (1f - oProg) * primaryOverlayHeightPx
                                alpha = (1f - 0.6f * sProg).coerceIn(0f, 1f)
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (overlayProgress.value < 0.8f) handleBack() else coroutineScope.launch {
                                            overlayProgress.animateTo(
                                                1f,
                                                spring(dampingRatio = 0.82f, stiffness = 220f)
                                            )
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaProgress =
                                            dragAmount / (primaryOverlayHeightPx.takeIf { it > 0 }
                                                ?: 2000f)
                                        val newProgress =
                                            (overlayProgress.value - deltaProgress).coerceIn(0f, 1f)
                                        coroutineScope.launch { overlayProgress.snapTo(newProgress) }
                                    }
                                )
                            }
                    ) {
                        OverlayContent(
                            overlayType = displayedOverlay,
                            settings = settings,
                            currentWeather = weatherData.type,
                            onWeatherSelect = { 
                                weatherData = weatherData.copy(type = it, description = "forced ${it.title.lowercase()}")
                            },
                            onUpdateSettings = { settings = it },
                            onOpenNested = { activeNestedOverlay = it },
                            onTestOdometer = { f, t ->
                                testOdometerFrom = f
                                testOdometerTarget = t
                                testOdometerTrigger++
                                navType = NavType.Tab
                                backStack = listOf(Destination.Weather)
                                forwardStack = emptyList()
                                settingsMenu = "Main"
                                activeOverlay = OverlayType.None
                            })
                    }
                }

                if (isStackedOpen) {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { activeNestedOverlay = NestedOverlay.None })

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onGloballyPositioned {
                                secondaryOverlayHeightPx = it.size.height.toFloat()
                            }
                            .graphicsLayer {
                                translationY =
                                    (1f - stackedOverlayProgress.value.coerceIn(0f, 1f)) * secondaryOverlayHeightPx
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (stackedOverlayProgress.value < 0.8f) activeNestedOverlay =
                                            NestedOverlay.None else coroutineScope.launch {
                                            stackedOverlayProgress.animateTo(
                                                1f,
                                                spring(dampingRatio = 0.82f, stiffness = 220f)
                                            )
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaProgress =
                                            dragAmount / (secondaryOverlayHeightPx.takeIf { it > 0 }
                                                ?: 2000f)
                                        val newProgress =
                                            (stackedOverlayProgress.value - deltaProgress).coerceIn(
                                                0f,
                                                1f
                                            )
                                        coroutineScope.launch {
                                            stackedOverlayProgress.snapTo(
                                                newProgress
                                            )
                                        }
                                    }
                                )
                            }
                    ) {
                        HeaderTypeSelectionContent(
                            settings = settings,
                            onSelect = { type ->
                                settings = settings.copy(headerType = type); activeNestedOverlay =
                                NestedOverlay.None
                            })
                    }
                }
            }
        }
    }

}