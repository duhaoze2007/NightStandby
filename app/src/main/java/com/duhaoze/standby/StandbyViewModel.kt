package com.duhaoze.standby

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duhaoze.standby.data.AgendaState
import com.duhaoze.standby.data.CalendarRepository
import com.duhaoze.standby.data.MediaState
import com.duhaoze.standby.data.SettingsStore
import com.duhaoze.standby.data.WeatherRepository
import com.duhaoze.standby.data.WeatherState
import com.duhaoze.standby.media.MediaSessionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StandbyViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsStore(application)
    private val weatherRepository = WeatherRepository(application, settings)
    private val calendarRepository = CalendarRepository(application)

    val weather: StateFlow<WeatherState> = weatherRepository.state
    val media: StateFlow<MediaState> = MediaSessionStore.state

    private val _agenda = MutableStateFlow(AgendaState())
    val agenda: StateFlow<AgendaState> = _agenda.asStateFlow()

    private var weatherJob: Job? = null
    private var agendaJob: Job? = null

    private val _debugEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val debugEvents: SharedFlow<String> = _debugEvents.asSharedFlow()

    val hasLocationPermission: Boolean
        get() = weatherRepository.hasLocationPermission

    init {
        // 隐私同意前不做任何读取或联网；仅恢复本地缓存。
        if (settings.state.value.privacyAccepted) {
            MediaSessionStore.refreshAccess(application)
            refreshCalendar()
            refreshWeather(force = false)
        }
    }

    fun refreshWeather(force: Boolean = false) {
        if (!settings.state.value.privacyAccepted) return
        if (weatherJob?.isActive == true) return
        weatherJob = viewModelScope.launch {
            weatherRepository.refresh(force)
        }
    }

    fun refreshCalendar() {
        if (!settings.state.value.privacyAccepted) return
        if (agendaJob?.isActive == true) return
        agendaJob = viewModelScope.launch {
            _agenda.value = _agenda.value.copy(loading = true, error = null)
            _agenda.value = calendarRepository.load()
        }
    }

    fun refreshAll() {
        if (!settings.state.value.privacyAccepted) return
        MediaSessionStore.refreshAccess(getApplication())
        refreshCalendar()
        refreshWeather(force = false)
    }

    fun hasCalendarPermission(): Boolean = calendarRepository.hasPermission()

    fun relativeUpdateLabel(updatedAt: Long): String =
        weatherRepository.relativeUpdateLabel(updatedAt)

    fun emitDebugEvent(event: String) {
        _debugEvents.tryEmit(event)
    }

    fun clearLocalData() {
        settings.clearAll()
        getApplication<Application>()
            .getSharedPreferences("weather_cache", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        weatherJob?.cancel()
        agendaJob?.cancel()
    }
}
