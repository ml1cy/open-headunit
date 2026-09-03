package com.andrerinas.openheadunit.newui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 24px-grid line icon set matching the design handoff (openhead-UI design/Open Headunit v4.dc.html)
 * exactly — every path's "d" data below is copied verbatim from that file's inline SVGs so the
 * icon shapes are pixel-accurate. Built once with a placeholder color and always drawn through
 * Icon(tint = ...) at the call site, which recolors via ColorFilter — that's the "currentColor"
 * behaviour from the CSS design.
 */
private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M ${cx - r},$cy a $r,$r 0 1,0 ${2 * r},0 a $r,$r 0 1,0 ${-2 * r},0 Z"

private fun roundedRectPath(x: Float, y: Float, w: Float, h: Float, rx: Float): String {
    val x2 = x + w
    val y2 = y + h
    val xr = x + rx
    val xw = x2 - rx
    val yr = y + rx
    val yh = y2 - rx
    return "M $xr,$y H $xw A $rx,$rx 0 0 1 $x2,$yr V $yh A $rx,$rx 0 0 1 $xw,$y2 H $xr " +
        "A $rx,$rx 0 0 1 $x,$yh V $yr A $rx,$rx 0 0 1 $xr,$y Z"
}

private fun buildIcon(
    strokePaths: List<String> = emptyList(),
    fillPaths: List<String> = emptyList(),
    strokeWidth: Float = 1.7f,
    viewport: Float = 24f,
): ImageVector {
    val builder = ImageVector.Builder(
        name = "hu_icon_${strokePaths.size}_${fillPaths.size}_${strokePaths.hashCode()}",
        defaultWidth = viewport.dp,
        defaultHeight = viewport.dp,
        viewportWidth = viewport,
        viewportHeight = viewport,
    )
    strokePaths.forEach { d ->
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    fillPaths.forEach { d ->
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black),
            stroke = null,
        )
    }
    return builder.build()
}

object HuIcons {
    val brandCar: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf("M4 16h16M6.5 16V10l2-4.2h7L17.5 10v6", "M7 19.5h2.4M14.6 19.5H17"),
            fillPaths = listOf(circlePath(8.4f, 16f, 1.5f), circlePath(15.6f, 16f, 1.5f)),
            strokeWidth = 2.1f,
        )
    }

    val home: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M3.5 10.6 12 3.6l8.5 7v9.3a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1z", "M9.5 20.9v-6h5v6"))
    }

    val auto: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                roundedRectPath(2.5f, 4.5f, 19f, 13f, 2.2f),
                "M8 21h8M12 17.5V21",
                "M9.2 13.4 12 8l2.8 5.4M10.2 11.9h3.6",
            ),
        )
    }

    val radio: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                roundedRectPath(2.5f, 8.5f, 19f, 11.5f, 2.2f),
                circlePath(8f, 14.2f, 2.9f),
                "M14.4 12.2h4.4M14.4 16.4h4.4M6.6 8.2 18 3.6",
            ),
        )
    }

    val phone: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M7 3.8 9.4 8 7.6 10.2a12.4 12.4 0 0 0 6.2 6.2L16 14.6l4.2 2.4v3.2a1.4 1.4 0 0 1-1.6 1.4C10.4 20.7 3.3 13.6 2.4 5.4A1.4 1.4 0 0 1 3.8 3.8z"))
    }

    val camera: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                "M3.6 7h4l1.4-2.2h6L16.4 7h4a1.4 1.4 0 0 1 1.4 1.4v9.2a1.4 1.4 0 0 1-1.4 1.4H3.6a1.4 1.4 0 0 1-1.4-1.4V8.4A1.4 1.4 0 0 1 3.6 7z",
                circlePath(12f, 13f, 3.6f),
            ),
        )
    }

    val settingsGear: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                circlePath(12f, 12f, 3.3f),
                "M12 2.4v3M12 18.6v3M2.4 12h3M18.6 12h3M5.2 5.2l2.1 2.1M16.7 16.7l2.1 2.1M18.8 5.2l-2.1 2.1M7.3 16.7l-2.1 2.1",
            ),
        )
    }

    val thermometer: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M12 3.5a2.4 2.4 0 0 1 2.4 2.4v7.3a4.6 4.6 0 1 1-4.8 0V5.9A2.4 2.4 0 0 1 12 3.5z"), strokeWidth = 1.8f)
    }

    val gpsPin: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M12 3 4 20.5l8-3.9 8 3.9z"), strokeWidth = 1.8f)
    }

    val bluetooth: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M8.2 7.4 15 12.6l-3.4 2.6V4.5l3.4 2.6-6.8 5.5"), strokeWidth = 1.8f)
    }

    val wifi: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf("M2.6 9.2a13.6 13.6 0 0 1 18.8 0M5.9 12.9a8.9 8.9 0 0 1 12.2 0M9.2 16.6a4.2 4.2 0 0 1 5.6 0"),
            fillPaths = listOf(circlePath(12f, 19.8f, 1.1f)),
            strokeWidth = 1.8f,
        )
    }

    val chevronDown: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M6 9.5 12 15l6-5.5"), strokeWidth = 2f)
    }

    val chevronRight: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M9 5.5 15.5 12 9 18.5"), strokeWidth = 2f)
    }

    val prevTrack: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M6 5h2.6v14H6zM19 5v14l-9.6-7z"))
    }

    val nextTrack: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M18 5h-2.6v14H18zM5 5v14l9.6-7z"))
    }

    val prevItem: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M4 5h2.4v14H4zM20 5v14l-9.4-7z"))
    }

    val nextItem: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M17.6 5H20v14h-2.4zM4 5v14l9.4-7z"))
    }

    val play: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M8 5.2v13.6l11-6.8z"))
    }

    val pause: ImageVector by lazy {
        buildIcon(fillPaths = listOf(roundedRectPath(7f, 5f, 4f, 14f, 1f), roundedRectPath(13f, 5f, 4f, 14f, 1f)))
    }

    val volume: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf("M16.2 9.4a3.7 3.7 0 0 1 0 5.2M18.8 6.8a7.4 7.4 0 0 1 0 10.4"),
            fillPaths = listOf("M4 9.5h3.4L12 5.4v13.2L7.4 14.5H4z"),
            strokeWidth = 1.8f,
        )
    }

    val star: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M12 3.4 14.6 9l6.1.6-4.6 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.6-4.1L9.4 9z"), strokeWidth = 1.7f)
    }

    val starFilled: ImageVector by lazy {
        buildIcon(fillPaths = listOf("M12 3.4 14.6 9l6.1.6-4.6 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.6-4.1L9.4 9z"))
    }

    val search: ImageVector by lazy {
        buildIcon(strokePaths = listOf(circlePath(11f, 11f, 7f), "M16.2 16.2 21 21"), strokeWidth = 1.9f)
    }

    val turnArrow: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M8 20V10a4 4 0 0 1 4-4h5", "M13.5 2.5 17.5 6 13.5 9.5"), strokeWidth = 2f)
    }

    val backspace: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                "M9 5h11a1.6 1.6 0 0 1 1.6 1.6v10.8A1.6 1.6 0 0 1 20 19H9L2.6 12z",
                "M17 9.5 12.4 14.5M12.4 9.5 17 14.5",
            ),
            strokeWidth = 1.8f,
        )
    }

    val mediaBt: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf("M9 17.6V6.4l10 2.4-14 6", circlePath(6.5f, 17.6f, 2.4f), circlePath(17f, 15.4f, 2.4f)),
        )
    }

    val navigationPin: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M12 2.8 4.4 21 12 17.2 19.6 21z"))
    }

    val save: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M12 3v12M7.5 10.5 12 15l4.5-4.5M4 20h16"), strokeWidth = 1.8f)
    }

    val scan: ImageVector by lazy {
        buildIcon(strokePaths = listOf("M4 12a8 8 0 0 1 8-8M20 12a8 8 0 0 1-8 8"), strokeWidth = 1.9f)
    }

    val info: ImageVector by lazy {
        buildIcon(strokePaths = listOf(circlePath(12f, 12f, 8.5f), "M12 11v5.5M12 8v.1"), strokeWidth = 1.8f)
    }

    val brightness: ImageVector by lazy {
        buildIcon(
            strokePaths = listOf(
                circlePath(12f, 12f, 4.2f),
                "M12 2.4v2.4M12 19.2v2.4M4.4 4.4l1.7 1.7M17.9 17.9l1.7 1.7M2 12h2.4M19.6 12H22M4.4 19.6l1.7-1.7M17.9 6.1l1.7-1.7",
            ),
            strokeWidth = 1.8f,
        )
    }
}
