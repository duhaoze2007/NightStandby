package com.duhaoze.standby.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("standby_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        _state.value = next
        save(next)
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        update { it.copy(privacyAccepted = accepted) }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _state.value = Settings()
    }

    private fun load(): Settings = Settings(
        privacyAccepted = prefs.getBoolean("privacy_accepted", false),
        clockStyle = when (prefs.getString("clock_style", "DIGITAL")) {
            "ANALOG" -> ClockStyle.ANALOG
            else -> ClockStyle.DIGITAL
        },
        use24Hour = prefs.getBoolean("use_24_hour", true),
        showSeconds = prefs.getBoolean("show_seconds", false),
        analogShowSeconds = prefs.getBoolean("analog_show_seconds", false),
        dimDelaySec = prefs.getInt("dim_delay_sec", 60),
        autoRed = prefs.getBoolean("auto_red", true),
        forceRed = prefs.getBoolean("force_red", false),
        redThresholdLux = prefs.getFloat("red_threshold_lux", 8f),
        burnInShift = prefs.getBoolean("burn_in_shift", true),
        limitRefreshRate = prefs.getBoolean("limit_refresh_rate", true),
        widgetCalendar = prefs.getBoolean("widget_calendar", true),
        widgetMusic = prefs.getBoolean("widget_music", true),
        cityQuery = prefs.getString("city_query", "") ?: ""
    )

    private fun save(s: Settings) {
        prefs.edit()
            .putBoolean("privacy_accepted", s.privacyAccepted)
            .putString("clock_style", s.clockStyle.name)
            .putBoolean("use_24_hour", s.use24Hour)
            .putBoolean("show_seconds", s.showSeconds)
            .putBoolean("analog_show_seconds", s.analogShowSeconds)
            .putInt("dim_delay_sec", s.dimDelaySec)
            .putBoolean("auto_red", s.autoRed)
            .putBoolean("force_red", s.forceRed)
            .putFloat("red_threshold_lux", s.redThresholdLux)
            .putBoolean("burn_in_shift", s.burnInShift)
            .putBoolean("limit_refresh_rate", s.limitRefreshRate)
            .putBoolean("widget_calendar", s.widgetCalendar)
            .putBoolean("widget_music", s.widgetMusic)
            .putString("city_query", s.cityQuery)
            .apply()
    }
}
