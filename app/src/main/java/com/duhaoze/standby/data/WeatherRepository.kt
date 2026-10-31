package com.duhaoze.standby.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 天气是应用里唯一会主动联网的业务模块（定位同样仅为天气服务）。
 * 它只向 Open-Meteo 发送一个粗略坐标和一个城市名，不发送任何设备标识、账号或使用记录。
 * 所有其他模块——时钟、日历、媒体、传感器、设置——都不曾发起网络请求。
 */
class WeatherRepository(
    private val context: Context,
    private val settingsStore: SettingsStore
) {
    private val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(WeatherState(data = loadCache()))
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(force: Boolean = false): WeatherState = mutex.withLock {
        val current = _state.value
        val fresh = current.data != null && !current.data.stale &&
            System.currentTimeMillis() - current.data.updatedAt < 20 * 60_000L
        if (!force && fresh) return@withLock current
        if (current.loading) return@withLock current

        _state.value = current.copy(loading = true, error = null)
        return@withLock try {
            val target = resolveTarget()
            val raw = fetchWeather(target.latitude, target.longitude)
            val data = parseWeather(
                raw = raw,
                label = target.label,
                region = target.region,
                latitude = target.latitude,
                longitude = target.longitude,
                updatedAt = System.currentTimeMillis(),
                stale = false,
                usingDeviceLocation = target.fromDevice
            )
            saveCache(raw, target, data.updatedAt, data.usingDeviceLocation)
            _state.value = WeatherState(data = data, loading = false, error = null)
            _state.value
        } catch (t: Throwable) {
            Log.w(TAG, "weather refresh failed: ${t.javaClass.simpleName}")
            val cached = loadCache()?.copy(stale = true)
            _state.value = WeatherState(
                data = cached ?: current.data?.copy(stale = true),
                loading = false,
                error = friendlyError(t)
            )
            _state.value
        }
    }

    private data class Target(
        val latitude: Double,
        val longitude: Double,
        val label: String,
        val region: String,
        val fromDevice: Boolean
    )

    private suspend fun resolveTarget(): Target {
        val configured = settingsStore.state.value.cityQuery.trim()
        if (configured.isNotEmpty()) {
            val geo = geocode(configured) ?: throw IOException("city_not_found")
            return Target(
                latitude = geo.first,
                longitude = geo.second,
                label = configured,
                region = geo.third,
                fromDevice = false
            )
        }

        if (hasLocationPermission) {
            val location = lastKnownLocation()
            if (location != null) {
                return Target(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    label = "当前位置",
                    region = "",
                    fromDevice = true
                )
            }
        }

        val fallback = fallbackCityForTimeZone()
        val geo = geocode(fallback) ?: knownCoordinates(fallback)
            ?: throw IOException("city_not_found")
        return Target(
            latitude = geo.first,
            longitude = geo.second,
            label = fallback,
            region = geo.third,
            fromDevice = false
        )
    }

    private fun lastKnownLocation(): Location? {
        if (!hasLocationPermission) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { it.time > 0 }
            .maxByOrNull { it.time }
            ?.takeIf { System.currentTimeMillis() - it.time < 7 * 24 * 60 * 60 * 1000L }
    }

    private suspend fun geocode(query: String): Triple<Double, Double, String>? {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$encoded&count=1&language=zh&format=json"
        return try {
            val raw = httpGet(url)
            val results = JSONObject(raw).optJSONArray("results")
            val first = results?.optJSONObject(0) ?: return null
            val country = first.optString("country", "")
            val region = country.takeIf { it.isNotBlank() } ?: ""
            Triple(first.getDouble("latitude"), first.getDouble("longitude"), region)
        } catch (t: Throwable) {
            null
        }
    }

    private fun knownCoordinates(query: String): Triple<Double, Double, String>? = when (query) {
        "北京" -> Triple(39.9042, 116.4074, "中国")
        "上海" -> Triple(31.2304, 121.4737, "中国")
        "广州" -> Triple(23.1291, 113.2644, "中国")
        "深圳" -> Triple(22.5431, 114.0579, "中国")
        "杭州" -> Triple(30.2741, 120.1551, "中国")
        "成都" -> Triple(30.5728, 104.0668, "中国")
        "吉隆坡" -> Triple(3.1390, 101.6869, "马来西亚")
        "新加坡" -> Triple(1.3521, 103.8198, "新加坡")
        "东京" -> Triple(35.6762, 139.6503, "日本")
        "伦敦" -> Triple(51.5074, -0.1278, "英国")
        "纽约" -> Triple(40.7128, -74.0060, "美国")
        else -> null
    }

    private fun fallbackCityForTimeZone(): String {
        val zone = ZoneId.systemDefault().id
        return when {
            zone == "Asia/Kuala_Lumpur" -> "吉隆坡"
            zone == "Asia/Singapore" -> "新加坡"
            zone == "Asia/Shanghai" || zone == "Asia/Chongqing" -> "上海"
            zone == "Asia/Hong_Kong" -> "香港"
            zone == "Asia/Taipei" -> "台北"
            zone == "Asia/Tokyo" -> "东京"
            zone == "Europe/London" -> "伦敦"
            zone == "America/New_York" -> "纽约"
            else -> "吉隆坡"
        }
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double): String {
        val lat = String.format(Locale.US, "%.3f", latitude)
        val lon = String.format(Locale.US, "%.3f", longitude)
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
            "precipitation,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=auto&forecast_days=5"
        return httpGet(url)
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Standby/1.0 (Android; offline-first)")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            if (code !in 200..299) throw IOException("HTTP $code")
            body ?: throw IOException("empty response")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeather(
        raw: String,
        label: String,
        region: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long,
        stale: Boolean,
        usingDeviceLocation: Boolean
    ): WeatherData {
        val root = JSONObject(raw)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val hourly = root.getJSONObject("hourly")

        val dailyHighs = daily.getJSONArray("temperature_2m_max")
        val dailyLows = daily.getJSONArray("temperature_2m_min")
        val dailyCodes = daily.getJSONArray("weather_code")
        val dailyDates = daily.getJSONArray("time")
        val sunrises = daily.optJSONArray("sunrise")
        val sunsets = daily.optJSONArray("sunset")

        val todayHigh = dailyHighs.optDouble(0, current.optDouble("temperature_2m", 0.0))
        val todayLow = dailyLows.optDouble(0, current.optDouble("temperature_2m", 0.0))
        val code = current.optInt("weather_code", dailyCodes.optInt(0, 3))

        val days = buildList {
            for (i in 0 until minOf(5, dailyDates.length())) {
                val date = runCatching { LocalDate.parse(dailyDates.getString(i)) }.getOrNull() ?: continue
                add(
                    DailyPoint(
                        date = date,
                        kind = kindForCode(dailyCodes.optInt(i, 3)),
                        high = Math.round(dailyHighs.optDouble(i, 0.0)).toInt(),
                        low = Math.round(dailyLows.optDouble(i, 0.0)).toInt()
                    )
                )
            }
        }

        val hourlyTimes = hourly.getJSONArray("time")
        val hourlyTemps = hourly.getJSONArray("temperature_2m")
        val hourlyCodes = hourly.getJSONArray("weather_code")
        val hourlyPrecip = hourly.optJSONArray("precipitation_probability")
        val now = LocalDateTime.now()
        val allHours = buildList {
            for (i in 0 until hourlyTimes.length()) {
                val time = runCatching { LocalDateTime.parse(hourlyTimes.getString(i)) }.getOrNull() ?: continue
                add(time to i)
            }
        }
        val start = allHours.indexOfFirst { it.first >= now.withMinute(0).withSecond(0).withNano(0) }
            .let { if (it >= 0) it else 0 }
        val hourlyPoints = buildList {
            for (k in start until minOf(start + 6, allHours.size)) {
                val (time, index) = allHours[k]
                add(
                    HourlyPoint(
                        time = time.toLocalTime(),
                        temperature = Math.round(hourlyTemps.optDouble(index, 0.0)).toInt(),
                        kind = kindForCode(hourlyCodes.optInt(index, code)),
                        precipitationProbability = hourlyPrecip?.optInt(index, 0) ?: 0
                    )
                )
            }
        }

        val sunrise = sunrises?.optString(0, "")?.let { it.takeLast(5) } ?: ""
        val sunset = sunsets?.optString(0, "")?.let { it.takeLast(5) } ?: ""

        return WeatherData(
            city = label,
            region = region,
            latitude = latitude,
            longitude = longitude,
            temperature = current.optDouble("temperature_2m", 0.0),
            feelsLike = current.optDouble("apparent_temperature", 0.0),
            humidity = current.optInt("relative_humidity_2m", 0),
            windSpeed = current.optDouble("wind_speed_10m", 0.0),
            precipitation = current.optDouble("precipitation", 0.0),
            kind = kindForCode(code),
            isDay = current.optInt("is_day", 1) == 1,
            high = Math.round(todayHigh).toInt(),
            low = Math.round(todayLow).toInt(),
            sunrise = sunrise,
            sunset = sunset,
            hourly = hourlyPoints,
            daily = days,
            updatedAt = updatedAt,
            stale = stale,
            usingDeviceLocation = usingDeviceLocation
        )
    }

    private fun kindForCode(code: Int): WeatherKind = when (code) {
        0 -> WeatherKind.CLEAR
        1, 2 -> WeatherKind.PARTLY_CLOUDY
        3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.STORM
        else -> WeatherKind.CLOUDY
    }

    private fun friendlyError(t: Throwable): String = when {
        t is IOException && t.message == "city_not_found" -> "无法确认城市，显示上次缓存"
        t is IOException -> "天气暂时无法连接，显示上次缓存"
        else -> "天气数据暂时不可用"
    }

    private fun saveCache(raw: String, target: Target, updatedAt: Long, fromDevice: Boolean) {
        prefs.edit()
            .putString("raw", raw)
            .putString("label", target.label)
            .putString("region", target.region)
            .putString("lat", target.latitude.toString())
            .putString("lon", target.longitude.toString())
            .putLong("updated", updatedAt)
            .putBoolean("from_device", fromDevice)
            .apply()
    }

    private fun loadCache(): WeatherData? {
        val raw = prefs.getString("raw", null) ?: return null
        val updated = prefs.getLong("updated", 0L)
        if (updated <= 0L) return null
        return runCatching {
            parseWeather(
                raw = raw,
                label = prefs.getString("label", "天气") ?: "天气",
                region = prefs.getString("region", "") ?: "",
                latitude = prefs.getString("lat", "0")?.toDoubleOrNull() ?: 0.0,
                longitude = prefs.getString("lon", "0")?.toDoubleOrNull() ?: 0.0,
                updatedAt = updated,
                stale = true,
                usingDeviceLocation = prefs.getBoolean("from_device", false)
            )
        }.getOrNull()
    }

    fun relativeUpdateLabel(updatedAt: Long): String {
        if (updatedAt <= 0L) return "尚未更新"
        val minutes = (System.currentTimeMillis() - updatedAt) / 60_000L
        return when {
            minutes < 1 -> "刚刚更新"
            minutes < 60 -> "${minutes} 分钟前"
            minutes < 24 * 60 -> "${minutes / 60} 小时前"
            else -> Instant.ofEpochMilli(updatedAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        }
    }

    companion object {
        private const val TAG = "StandbyWeather"
    }
}
