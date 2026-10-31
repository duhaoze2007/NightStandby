package com.duhaoze.standby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duhaoze.standby.data.WeatherData
import com.duhaoze.standby.data.WeatherState
import java.time.format.DateTimeFormatter

@Composable
fun WeatherPanel(
    weather: WeatherData?,
    state: WeatherState,
    compact: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    WidgetCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(5.dp)) { drawCircle(colors.accent) }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "天气",
                    color = colors.secondary,
                    fontSize = 11.sp,
                    letterSpacing = 2.2.sp
                )
                if (weather?.stale == true) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "缓存",
                        color = colors.tertiary,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.loading) "更新中" else "刷新",
                    color = if (state.loading) colors.tertiary else colors.accent,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !state.loading, onClick = onRefresh)
                        .semantics {
                            contentDescription = "刷新天气"
                            role = Role.Button
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (weather == null) {
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(if (compact) 38.dp else 54.dp)
                ) {
                    WeatherGlyph(
                        kind = com.duhaoze.standby.data.WeatherKind.CLOUDY,
                        isDay = true,
                        size = if (compact) 38.dp else 54.dp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.loading) "正在获取天气…" else (state.error ?: "天气尚未加载"),
                        color = colors.primary,
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "天气与定位是唯一允许联网的服务",
                        color = colors.tertiary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "设置",
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenSettings)
                        .semantics {
                            contentDescription = "打开天气设置"
                            role = Role.Button
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            return@WidgetCard
        }

        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherGlyph(
                kind = weather.kind,
                isDay = weather.isDay,
                size = if (compact) 46.dp else 68.dp
            )
            Spacer(Modifier.width(if (compact) 12.dp else 18.dp))
            Text(
                text = weather.temperatureText,
                color = colors.primary,
                fontSize = if (compact) 42.sp else 60.sp,
                lineHeight = (if (compact) 42.sp else 58.sp),
                fontWeight = FontWeight.Thin,
                letterSpacing = (-2).sp,
            )
            Spacer(Modifier.width(if (compact) 12.dp else 18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather.kind.label,
                        color = colors.primary,
                        fontSize = if (compact) 14.sp else 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = weather.highLowText,
                        color = colors.secondary,
                        fontSize = if (compact) 10.sp else 11.sp
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(weather.city)
                        if (weather.usingDeviceLocation) append(" · 定位")
                        if (weather.region.isNotBlank()) append(" · ${weather.region}")
                    },
                    color = colors.secondary,
                    fontSize = if (compact) 11.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${if (weather.stale) "离线缓存" else "在线"} · ${relative(weather.updatedAt)} · Open-Meteo",
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!compact) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatText("${Math.round(weather.feelsLike)}°", "体感", Modifier.weight(1f))
                StatText("${weather.humidity}%", "湿度", Modifier.weight(1f))
                StatText("%.1f".format(weather.windSpeed), "风速 km/h", Modifier.weight(1f))
                StatText(weather.sunrise.ifBlank { "--:--" }, "日出", Modifier.weight(1f))
                StatText(weather.sunset.ifBlank { "--:--" }, "日落", Modifier.weight(1f))
            }
            if (weather.hourly.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ThinDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weather.hourly.take(5).forEach { point ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = point.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                color = colors.tertiary,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            WeatherGlyph(point.kind, isDay = true, size = 19.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${point.temperature}°",
                                color = colors.primary,
                                fontSize = 12.sp,
                            )
                            if (point.precipitationProbability > 0) {
                                Text(
                                    text = "${point.precipitationProbability}%",
                                    color = colors.accent,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "体感 ${Math.round(weather.feelsLike)}°",
                    color = colors.secondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "湿度 ${weather.humidity}%",
                    color = colors.secondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "日落 ${weather.sunset.ifBlank { "--:--" }}",
                    color = colors.tertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun relative(updatedAt: Long): String {
    val minutes = (System.currentTimeMillis() - updatedAt) / 60_000L
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        else -> "${minutes / 60}小时前"
    }
}

@Composable
fun WeatherMiniRow(
    weather: WeatherData?,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        WeatherGlyph(
            kind = weather?.kind ?: com.duhaoze.standby.data.WeatherKind.CLOUDY,
            isDay = weather?.isDay ?: true,
            size = 20.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = weather?.let { "${it.temperatureText} ${it.kind.label}" } ?: "天气未加载",
            color = colors.secondary,
            fontSize = 11.sp
        )
        if (weather != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· ${weather.city}",
                color = colors.tertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
