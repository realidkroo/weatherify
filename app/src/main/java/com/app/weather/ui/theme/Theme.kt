package com.app.weather.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.app.weather.R

val OpenRundeFontFamily = FontFamily(
    Font(R.font.openrunde_regular, FontWeight.Normal),
    Font(R.font.openrunde_medium, FontWeight.Medium),
    Font(R.font.openrunde_semibold, FontWeight.SemiBold),
    Font(R.font.openrunde_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()
val AppTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = OpenRundeFontFamily, letterSpacing = (-0.2).sp)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.LightGray,
    tertiary = Color.Gray,
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF1E1E1E),
    onSecondaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color.DarkGray,
    tertiary = Color.Gray,
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFFE0E0E0),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color.Black
)

@Composable
fun WeatherAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
