package com.andrerinas.openheadunit.newui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType

/** Touch-first replacement for the design's CSS `:hover` states: a pressed surface, since this is a touch panel, not a pointer. */
@Composable
fun Modifier.huPressable(
    shape: Shape,
    normal: Color = Color.Transparent,
    pressed: Color,
    onClick: (() -> Unit)? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var m = this.clip(shape).background(if (isPressed) pressed else normal, shape)
    if (onClick != null) {
        m = m.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    }
    return m
}

@Composable
fun EyebrowLabel(text: String, color: Color = HuTheme.colors.muted, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = HuType.eyebrow, color = color, modifier = modifier)
}

@Composable
fun HuCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    border: Boolean = true,
    padding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(radius)
    var base: Modifier = modifier.clip(shape).background(colors.surface, shape)
    if (border) base = base.border(1.dp, colors.line2, shape)
    if (onClick != null) {
        base = base.huPressable(shape = shape, normal = Color.Transparent, pressed = colors.surface2, onClick = onClick)
    }
    Box(modifier = base.padding(padding)) { content() }
}

@Composable
fun HuPillButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    filled: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(50)
    val bg = if (filled) colors.accent else Color.Transparent
    val fg = if (filled) colors.onAccent else colors.text
    Box(
        modifier = modifier
            .height(height)
            .huPressable(
                shape = shape,
                normal = bg,
                pressed = if (filled) colors.accent else colors.surface2,
                onClick = onClick,
            )
            .then(if (!filled) Modifier.border(1.dp, colors.line, shape) else Modifier)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Box(Modifier.width(11.dp))
            }
            Text(text, color = fg, style = HuType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
        }
    }
}

@Composable
fun HuIconCircleButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    diameter: Dp = 64.dp,
    background: Color = HuTheme.colors.surface2,
    tint: Color = HuTheme.colors.text,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(diameter)
            .huPressable(shape = shape, normal = background, pressed = HuTheme.colors.surface3, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(diameter * 0.36f))
    }
}

@Composable
fun HuSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    val trackColor = if (checked) colors.accent else colors.surface3
    Box(
        modifier = modifier
            .width(64.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun SignalBars(level: Int, modifier: Modifier = Modifier, activeColor: Color = HuTheme.colors.accent, inactiveColor: Color = HuTheme.colors.faint) {
    val heights = listOf(6.dp, 9.dp, 12.dp, 16.dp)
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        heights.forEachIndexed { index, h ->
            if (index > 0) Box(Modifier.width(3.dp))
            Box(
                Modifier
                    .width(4.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < level) activeColor else inactiveColor),
            )
        }
    }
}

@Composable
fun EllipsisText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}
