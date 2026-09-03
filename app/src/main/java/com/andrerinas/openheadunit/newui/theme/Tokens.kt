package com.andrerinas.openheadunit.newui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens ported 1:1 from the v4 handoff (openhead-UI/design_handoff_headunit_v4/README.md,
 * "Design tokens" section). Accent is constant across themes; foreground on accent is always
 * [HuColors.onAccent].
 */
data class HuColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val soft: Color,
    val good: Color,
    val onAccent: Color,
    val warning: Color,
    val danger: Color,
    val missedCall: Color,
    val isDark: Boolean,
)

val HuNightColors = HuColors(
    bg = Color(0xFF08090B),
    surface = Color(0xFF131519),
    surface2 = Color(0xFF1C1F25),
    surface3 = Color(0xFF262A31),
    line = Color.White.copy(alpha = 0.10f),
    line2 = Color.White.copy(alpha = 0.055f),
    text = Color(0xFFF3F4F6),
    muted = Color(0xFF8C919B),
    faint = Color(0xFF5A5F69),
    accent = Color(0xFFFF7A18),
    soft = Color(0xFFFF7A18).copy(alpha = 0.15f),
    good = Color(0xFF3FD07A),
    onAccent = Color(0xFF12130F),
    warning = Color(0xFFFFC53D),
    danger = Color(0xFFFF4D3D),
    missedCall = Color(0xFFFF6B5A),
    isDark = true,
)

val HuDayColors = HuColors(
    bg = Color(0xFFE8E9EC),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF1F2F5),
    surface3 = Color(0xFFE2E4E9),
    line = Color(0xFF0F1219).copy(alpha = 0.12f),
    line2 = Color(0xFF0F1219).copy(alpha = 0.07f),
    text = Color(0xFF14171C),
    muted = Color(0xFF666C77),
    faint = Color(0xFF989EA9),
    accent = Color(0xFFFF7A18),
    soft = Color(0xFFFF7A18).copy(alpha = 0.14f),
    good = Color(0xFF3FD07A),
    onAccent = Color(0xFF12130F),
    warning = Color(0xFFFFC53D),
    danger = Color(0xFFFF4D3D),
    missedCall = Color(0xFFFF6B5A),
    isDark = false,
)

/** Geometry scale: radii and spacing steps from the README "Geometry" section. */
object HuRadii {
    val xs = 13
    val sm = 14
    val md = 16
    val ml = 17
    val lg = 18
    val xl = 20
    val xxl = 22
    val xxxl = 24
    val huge = 28
    const val pill = 999
}
