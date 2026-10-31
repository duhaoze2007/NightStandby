package com.duhaoze.standby.data

import androidx.compose.ui.graphics.ImageBitmap
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Settings(
    val privacyAccepted: Boolean = false,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val dimDelaySec: Int = 60,
    val autoRed: Boolean = true,
    val forceRed: Boolean = false,
    val redThresholdLux: Float = 8f,
    val burnInShift: Boolean = true,
    val limitRefreshRate: Boolean = true,
    val widgetCalendar: Boolean = true,
    val widgetMusic: Boolean = true,
    val cityQuery: String = ""
)

enum class WeatherKind {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, SNOW, STORM;

    val label: String
        get() = when (this) {
            CLEAR -> "晴"
            PARTLY_CLOUDY -> "多云"
            CLOUDY -> "阴"
            FOG -> "雾"
            RAIN -> "雨"
            SNOW -> "雪"
            STORM -> "雷雨"
        }
}

data class HourlyPoint(
    val time: LocalTime,
    val temperature: Int,
    val kind: WeatherKind,
    val precipitationProbability: Int
)

data class DailyPoint(
    val date: LocalDate,
    val kind: WeatherKind,
    val high: Int,
    val low: Int
)

data class WeatherData(
    val city: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val kind: WeatherKind,
    val isDay: Boolean,
    val high: Int,
    val low: Int,
    val sunrise: String,
    val sunset: String,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
    val updatedAt: Long,
    val stale: Boolean = false,
    val usingDeviceLocation: Boolean = false
) {
    val temperatureText: String
        get() = "${Math.round(temperature)}°"
    val highLowText: String
        get() = "最高 $high°  最低 $low°"
}

data class WeatherState(
    val data: WeatherData? = null,
    val loading: Boolean = false,
    val error: String? = null
)

data class AgendaEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val location: String?,
    val calendarName: String?
)

data class AgendaState(
    val permissionGranted: Boolean = false,
    val today: List<AgendaEvent> = emptyList(),
    val upcoming: List<AgendaEvent> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAt: Long = 0L
)

data class MediaState(
    val accessGranted: Boolean = false,
    val connected: Boolean = false,
    val active: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val artwork: ImageBitmap? = null,
    val canSeek: Boolean = false,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val updatedAt: Long = 0L
) {
    val hasTrack: Boolean
        get() = title.isNotBlank()
}

data class BatteryInfo(
    val level: Int = 0,
    val charging: Boolean = false,
    val plugged: Boolean = false,
    val temperatureC: Float = 0f
)

data class NowPlayingPosition(
    val positionMs: Long,
    val durationMs: Long
)

fun formatClock(hour: Int, minute: Int, second: Int, use24Hour: Boolean, showSeconds: Boolean): String {
    val base = if (use24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "%d:%02d".format(h, minute)
    }
    return if (showSeconds) "$base:%02d".format(second) else base
}

fun formatTimeOfDay(millis: Long, use24Hour: Boolean): String {
    val dt = java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    return if (use24Hour) {
        "%02d:%02d".format(dt.hour, dt.minute)
    } else {
        val h = when {
            dt.hour == 0 -> 12
            dt.hour > 12 -> dt.hour - 12
            else -> dt.hour
        }
        val suffix = if (dt.hour < 12) "上午" else "下午"
        "$suffix %d:%02d".format(h, dt.minute)
    }
}

fun nowLocal(): LocalDateTime = LocalDateTime.now()
