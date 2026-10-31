package com.bengali.voicetyping.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val BengaliGreenPrimary = Color(0xFF006C4C)
val BengaliGreenOnPrimary = Color(0xFFFFFFFF)
val BengaliGreenPrimaryContainer = Color(0xFF8CF8C7)
val BengaliGreenOnPrimaryContainer = Color(0xFF002114)
val BengaliBackgroundLight = Color(0xFFFBFDF9)
val BengaliSurfaceLight = Color(0xFFFBFDF9)

val BengaliGreenPrimaryDark = Color(0xFF6FDBAC)
val BengaliGreenOnPrimaryDark = Color(0xFF003825)
val BengaliBackgroundDark = Color(0xFF111412)
val BengaliSurfaceDark = Color(0xFF191C1A)

private val DarkColorScheme = darkColorScheme(
    primary = BengaliGreenPrimaryDark,
    onPrimary = BengaliGreenOnPrimaryDark,
    background = BengaliBackgroundDark,
    surface = BengaliSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = BengaliGreenPrimary,
    onPrimary = BengaliGreenOnPrimary,
    primaryContainer = BengaliGreenPrimaryContainer,
    background = BengaliBackgroundLight,
    surface = BengaliSurfaceLight
)

@Composable
fun BengaliVoiceTypingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}