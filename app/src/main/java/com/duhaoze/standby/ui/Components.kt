package com.duhaoze.standby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duhaoze.standby.data.WeatherKind
import com.duhaoze.standby.data.formatClock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BrandMark(compact: Boolean = false) {
    val colors = LocalStandbyColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(9.dp)) {
            drawCircle(color = colors.accent, radius = size.minDimension / 2f)
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "守夜",
            color = colors.primary,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp
        )
        if (!compact) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "STANDBY",
                color = colors.tertiary,
                fontSize = 9.sp,
                letterSpacing = 2.4.sp
            )
        }
    }
}

@Composable
fun ClockPanel(
    now: LocalDateTime,
    use24Hour: Boolean,
    showSeconds: Boolean,
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    val hour = now.hour
    val minute = now.minute
    val second = now.second
    val main = when {
        use24Hour -> "%02d:%02d".format(hour, minute)
        hour == 0 -> "12:%02d".format(minute)
        hour > 12 -> "%d:%02d".format(hour - 12, minute)
        else -> "%d:%02d".format(hour, minute)
    }
    val amPm = when {
        hour < 12 -> "AM"
        else -> "PM"
    }
    val date = now.format(DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE))
    val weekday = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE))
    val year = now.format(DateTimeFormatter.ofPattern("yyyy", Locale.US))

    BoxWithConstraints(modifier) {
        val width = maxWidth.value
        val height = maxHeight.value
        val rough = if (landscape) {
            minOf(width / 2.95f, height * 0.50f)
        } else {
            minOf(width / 2.9f, height * 0.56f)
        }
        val fontSize = rough.coerceIn(64f, 210f).sp
        val sideSize = (fontSize.value * 0.19f).coerceIn(14f, 38f).sp
        val dateSize = (fontSize.value * 0.115f).coerceIn(12f, 20f).sp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$year  $date",
                    color = colors.secondary,
                    fontSize = dateSize,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                Canvas(Modifier.size(3.dp)) {
                    drawCircle(colors.tertiary)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = weekday,
                    color = colors.secondary,
                    fontSize = dateSize,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(if (landscape) 8.dp else 6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = main,
                    color = colors.primary,
                    style = TextStyle(
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 0.82f).sp,
                        fontWeight = FontWeight.Thin,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-fontSize.value * 0.028f).sp,
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                if (showSeconds || !use24Hour) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp, bottom = (fontSize.value * 0.10f).dp)
                    ) {
                        if (!use24Hour) {
                            Text(
                                text = amPm,
                                color = colors.accent,
                                fontSize = sideSize,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                        if (showSeconds) {
                            Text(
                                text = ":%02d".format(second),
                                color = colors.secondary,
                                fontSize = sideSize,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (landscape) 12.dp else 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(if (landscape) 34.dp else 26.dp)
                        .height(1.dp)
                        .background(colors.accent)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (use24Hour) "24 小时制" else "12 小时制",
                    color = colors.tertiary,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

@Composable
fun WidgetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalStandbyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(30.dp))
            .padding(22.dp),
        content = content
    )
}

@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = LocalStandbyColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(5.dp)) {
                drawCircle(colors.accent)
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = title,
                color = colors.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.2.sp
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun Label(text: String, color: Color? = null) {
    val colors = LocalStandbyColors.current
    Text(
        text = text,
        color = color ?: colors.secondary,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp
    )
}

@Composable
fun StatText(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalStandbyColors.current
    Column(modifier) {
        Text(
            text = value,
            color = colors.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = colors.tertiary, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    val colors = LocalStandbyColors.current
    Box(modifier.height(1.dp).background(colors.line))
}

@Composable
fun IconTap(
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun RoundIconButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    enabled: Boolean = true
) {
    val colors = LocalStandbyColors.current
    val base = modifier.size(48.dp).clip(CircleShape)
    val decorated = if (bordered) {
        base.border(1.dp, if (enabled) colors.lineStrong else colors.line, CircleShape)
    } else base
    Box(
        modifier = decorated
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        when (description) {
            "播放" -> PlayGlyph(18.dp, if (enabled) colors.primary else colors.tertiary)
            "暂停" -> PauseGlyph(18.dp, if (enabled) colors.primary else colors.tertiary)
            "上一首" -> PreviousGlyph(18.dp, if (enabled) colors.primary else colors.tertiary)
            "下一首" -> NextGlyph(18.dp, if (enabled) colors.primary else colors.tertiary)
            else -> PlayGlyph(18.dp, colors.primary)
        }
    }
}

@Composable
fun PlayGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.28f, s * 0.14f)
            lineTo(s * 0.84f, s * 0.5f)
            lineTo(s * 0.28f, s * 0.86f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun PauseGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        val w = s * 0.17f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.24f, s * 0.16f),
            size = Size(w, s * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.59f, s * 0.16f),
            size = Size(w, s * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
        )
    }
}

@Composable
fun NextGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.18f, s * 0.16f)
            lineTo(s * 0.68f, s * 0.5f)
            lineTo(s * 0.18f, s * 0.84f)
            close()
        }
        drawPath(path, color)
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.72f, s * 0.16f),
            size = Size(s * 0.13f, s * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
        )
    }
}

@Composable
fun PreviousGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.15f, s * 0.16f),
            size = Size(s * 0.13f, s * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
        )
        val path = Path().apply {
            moveTo(s * 0.82f, s * 0.16f)
            lineTo(s * 0.32f, s * 0.5f)
            lineTo(s * 0.82f, s * 0.84f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun GearGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        val teeth = 8
        repeat(teeth * 2) { i ->
            val angle = (Math.PI * 2.0 * i / (teeth * 2.0)) - Math.PI / 2.0
            val radius = if (i % 2 == 0) r * 0.94f else r * 0.72f
            val x = c.x + (cos(angle) * radius).toFloat()
            val y = c.y + (sin(angle) * radius).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
        drawCircle(Color.Black, r * 0.29f, c)
    }
}

@Composable
fun ChevronLeftGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w * 0.64f, h * 0.20f), Offset(w * 0.34f, h * 0.5f), w * 0.10f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.64f, h * 0.80f), w * 0.10f, StrokeCap.Round)
    }
}

@Composable
fun ChevronDownGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w * 0.20f, h * 0.38f), Offset(w * 0.5f, h * 0.68f), w * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.68f), Offset(w * 0.80f, h * 0.38f), w * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun BoltGlyph(dim: Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.56f, 0f)
            lineTo(s * 0.18f, s * 0.56f)
            lineTo(s * 0.47f, s * 0.56f)
            lineTo(s * 0.40f, s)
            lineTo(s * 0.84f, s * 0.40f)
            lineTo(s * 0.54f, s * 0.40f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun BatteryPill(level: Int, charging: Boolean) {
    val colors = LocalStandbyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.line, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (charging) {
            BoltGlyph(11.dp, colors.accent)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = "$level%",
            color = if (charging) colors.accent else colors.secondary,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun WeatherGlyph(kind: WeatherKind, isDay: Boolean, size: Dp) {
    val colors = LocalStandbyColors.current
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2f, s / 2f)
        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) {
                    drawCircle(colors.accent, s * 0.20f, c)
                    repeat(8) { i ->
                        rotate(i * 45f, pivot = c) {
                            drawLine(
                                colors.accent,
                                Offset(c.x, c.y - s * 0.30f),
                                Offset(c.x, c.y - s * 0.43f),
                                strokeWidth = s * 0.045f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                } else {
                    drawCircle(colors.primary, s * 0.27f, c)
                    drawCircle(Color.Black, s * 0.25f, Offset(c.x + s * 0.13f, c.y - s * 0.11f))
                }
            }
            WeatherKind.PARTLY_CLOUDY -> {
                drawCircle(
                    if (isDay) colors.accent else colors.primary,
                    s * 0.16f,
                    Offset(c.x - s * 0.12f, c.y - s * 0.16f)
                )
                drawCloud(colors.primary, colors.background, s, c, shiftY = s * 0.06f)
            }
            WeatherKind.CLOUDY -> drawCloud(colors.primary, colors.background, s, c)
            WeatherKind.FOG -> {
                repeat(3) { i ->
                    drawLine(
                        colors.secondary,
                        Offset(s * 0.18f, s * (0.34f + i * 0.16f)),
                        Offset(s * 0.82f, s * (0.34f + i * 0.16f)),
                        strokeWidth = s * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherKind.RAIN, WeatherKind.SNOW, WeatherKind.STORM -> {
                drawCloud(colors.primary, colors.background, s, c, shiftY = -s * 0.08f)
                when (kind) {
                    WeatherKind.RAIN -> repeat(3) { i ->
                        drawLine(
                            colors.accent,
                            Offset(s * (0.30f + i * 0.18f), s * 0.64f),
                            Offset(s * (0.24f + i * 0.18f), s * 0.82f),
                            strokeWidth = s * 0.045f,
                            cap = StrokeCap.Round
                        )
                    }
                    WeatherKind.SNOW -> repeat(3) { i ->
                        drawCircle(colors.primary, s * 0.025f, Offset(s * (0.30f + i * 0.18f), s * 0.72f))
                    }
                    WeatherKind.STORM -> {
                        val path = Path().apply {
                            moveTo(s * 0.54f, s * 0.58f)
                            lineTo(s * 0.38f, s * 0.80f)
                            lineTo(s * 0.50f, s * 0.80f)
                            lineTo(s * 0.42f, s * 0.98f)
                            lineTo(s * 0.66f, s * 0.72f)
                            lineTo(s * 0.53f, s * 0.72f)
                            close()
                        }
                        drawPath(path, colors.accent)
                    }
                    else -> Unit
                }
            }
        }
    }
}

private fun DrawScope.drawCloud(
    color: Color,
    cutout: Color,
    s: Float,
    c: Offset,
    shiftY: Float = 0f
) {
    val y = c.y + shiftY
    drawCircle(color, s * 0.16f, Offset(s * 0.40f, y - s * 0.02f))
    drawCircle(color, s * 0.21f, Offset(s * 0.56f, y - s * 0.10f))
    drawCircle(color, s * 0.14f, Offset(s * 0.71f, y + s * 0.01f))
    drawRoundRect(
        color = color,
        topLeft = Offset(s * 0.27f, y + s * 0.02f),
        size = Size(s * 0.52f, s * 0.18f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.09f, s * 0.09f)
    )
}

@Composable
fun TimeText(text: String, color: Color, size: TextUnit = 12.sp) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Light
    )
}

@Composable
fun EllipsisText(text: String, style: TextStyle, maxLines: Int = 1, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SolarArc(sunrise: String, sunset: String, now: LocalDateTime, modifier: Modifier = Modifier) {
    val colors = LocalStandbyColors.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.06f, h * 0.82f)
            cubicTo(w * 0.25f, h * 0.08f, w * 0.75f, h * 0.08f, w * 0.94f, h * 0.82f)
        }
        drawPath(path, colors.line, style = Stroke(width = 1.dp.toPx()))
        val minutes = now.hour * 60 + now.minute
        val rise = parseMinutes(sunrise) ?: (6 * 60)
        val set = parseMinutes(sunset) ?: (18 * 60)
        val progress = ((minutes - rise).toFloat() / (set - rise).coerceAtLeast(1)).coerceIn(0f, 1f)
        val t = progress
        val x = (1 - t) * (1 - t) * (1 - t) * (w * 0.06f) +
            3 * (1 - t) * (1 - t) * t * (w * 0.25f) +
            3 * (1 - t) * t * t * (w * 0.75f) +
            t * t * t * (w * 0.94f)
        val y = (1 - t) * (1 - t) * (1 - t) * (h * 0.82f) +
            3 * (1 - t) * (1 - t) * t * (h * 0.08f) +
            3 * (1 - t) * t * t * (h * 0.08f) +
            t * t * t * (h * 0.82f)
        drawCircle(colors.accent, 3.5.dp.toPx(), Offset(x, y))
    }
}

private fun parseMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

