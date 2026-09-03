package com.andrerinas.openheadunit.newui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

enum class HuThemeMode { NIGHT, DAY }

val LocalHuColors = staticCompositionLocalOf { HuNightColors }
val LocalHuThemeMode = compositionLocalOf { HuThemeMode.NIGHT }

/**
 * Root theme wrapper for the whole new head unit UI. Not Material-shaped (the design's palette
 * doesn't map cleanly onto Material3 roles, see ANDROID_IMPLEMENTATION.md "Theming"), so screens
 * read colors from [LocalHuColors] / [HuTheme.colors] rather than MaterialTheme.colorScheme.
 */
@Composable
fun HeadUnitTheme(mode: HuThemeMode, content: @Composable () -> Unit) {
    val colors = if (mode == HuThemeMode.NIGHT) HuNightColors else HuDayColors
    CompositionLocalProvider(
        LocalHuColors provides colors,
        LocalHuThemeMode provides mode,
    ) {
        content()
    }
}

object HuTheme {
    val colors: HuColors
        @Composable get() = LocalHuColors.current
    val mode: HuThemeMode
        @Composable get() = LocalHuThemeMode.current
}

/** Best-effort initial theme mode from the system's current night/day state. */
@Composable
fun systemHuThemeMode(): HuThemeMode = if (isSystemInDarkTheme()) HuThemeMode.NIGHT else HuThemeMode.DAY
