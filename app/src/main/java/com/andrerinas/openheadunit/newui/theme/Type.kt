package com.andrerinas.openheadunit.newui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Typography scale from the README "Typography" table.
 *
 * The design specifies Archivo, a variable font using a width axis (font-stretch 75-125%) that
 * has no equivalent on a static system font. Real fidelity means dropping the SIL-OFL Archivo
 * variable TTF into res/font and building each style with FontVariation.width(...) (Compose 1.6+
 * supports variable font settings via FontVariation on API 26+). That asset isn't available in
 * this environment, so [HuFontFamily] falls back to the system sans-serif and the width axis is
 * simply dropped — sizes, weights, spacing and tabular-figures below are otherwise exact.
 */
val HuFontFamily: FontFamily = FontFamily.SansSerif

/** `font-variant-numeric: tabular-nums` equivalent so numeric readouts don't jitter. */
private const val TabularNums = "tnum"

private fun style(
    size: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = HuFontFamily,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabular) TabularNums else null,
)

object HuType {
    val frequencyReadout = style(76.sp, FontWeight.ExtraBold, (-2.6).sp, tabular = true)
    val gaugeValue = style(82.sp, FontWeight.ExtraBold, (-3).sp, tabular = true)
    val heroTitle = style(46.sp, FontWeight.Bold, (-1).sp)
    val dialpadReadout = style(46.sp, FontWeight.Bold, 1.sp, tabular = true)
    val tripFigure = style(46.sp, FontWeight.Bold, tabular = true)
    val radioHeroTitle = style(40.sp, FontWeight.Bold)
    val vehicleStripValue = style(40.sp, FontWeight.Bold, (-1).sp, tabular = true)
    val clock = style(36.sp, FontWeight.Bold, (-0.6).sp, tabular = true)
    val screenTitle = style(31.sp, FontWeight.Bold)
    val cardTitle = style(28.sp, FontWeight.Bold, (-0.4).sp)
    val presetPrimary = style(24.sp, FontWeight.Bold, (-0.5).sp, tabular = true)
    val tileTitle = style(22.sp, FontWeight.Bold, (-0.2).sp)
    val listPrimary = style(19.sp, FontWeight.SemiBold)
    val body = style(16.sp, FontWeight.Medium)
    val caption = style(13.5.sp, FontWeight.Medium)
    val eyebrow = style(12.sp, FontWeight.Bold, 1.5.sp)
    val presetIndex = style(11.sp, FontWeight.Bold, 1.4.sp)
}
