package com.duhaoze.standby.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.duhaoze.standby.StandbyViewModel
import com.duhaoze.standby.data.AgendaState
import com.duhaoze.standby.data.BatteryInfo
import com.duhaoze.standby.data.MediaState
import com.duhaoze.standby.data.Settings
import com.duhaoze.standby.data.WeatherData
import com.duhaoze.standby.data.WeatherState
import com.duhaoze.standby.media.MediaListenerService
import com.duhaoze.standby.media.MediaSessionStore
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun StandbyApp(viewModel: StandbyViewModel) {
    val settings by viewModel.settings.state.collectAsState()
    if (!settings.privacyAccepted) {
        val activity = LocalContext.current as? Activity
        StandbyTheme(redMode = false) {
            PrivacyGate(
                onAccept = {
                    viewModel.settings.setPrivacyAccepted(true)
                    viewModel.refreshAll()
                },
                onDecline = { activity?.finishAndRemoveTask() }
            )
        }
    } else {
        StandbyHome(viewModel, settings)
    }
}

@Composable
private fun StandbyHome(viewModel: StandbyViewModel, settings: Settings) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val weatherState by viewModel.weather.collectAsState()
    val agenda by viewModel.agenda.collectAsState()
    val media by viewModel.media.collectAsState()

    var screen by remember { mutableStateOf("main") }
    var showPolicy by remember { mutableStateOf(false) }
    var dimmed by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var lastInteractionAt by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var lux by remember { mutableFloatStateOf(Float.NaN) }
    var battery by remember { mutableStateOf(BatteryInfo()) }
    var calendarGranted by remember { mutableStateOf(viewModel.hasCalendarPermission()) }
    var locationGranted by remember { mutableStateOf(viewModel.hasLocationPermission) }
    var shift by remember { mutableIntStateOf(0) }
    var widgetPage by rememberSaveable { mutableIntStateOf(0) }
    var resumed by remember { mutableStateOf(true) }

    val isNight = now.hour >= 22 || now.hour < 7
    val lightSaysDark = !lux.isNaN() && lux <= settings.redThresholdLux
    val redMode = settings.forceRed || (
        settings.autoRed && if (!lux.isNaN()) lightSaysDark else isNight
        )

    fun wake() {
        dimmed = false
        now = LocalDateTime.now()
        interaction++
        MediaSessionStore.refreshAccess(context)
        viewModel.refreshWeather(force = false)
    }

    fun noteInteraction() {
        val t = SystemClock.elapsedRealtime()
        if (t - lastInteractionAt > 350) {
            lastInteractionAt = t
            interaction++
        }
    }

    fun openSettings() {
        dimmed = false
        screen = "settings"
        noteInteraction()
        MediaSessionStore.refreshAccess(context)
        viewModel.refreshCalendar()
    }

    LaunchedEffect(Unit) {
        viewModel.debugEvents.collect { event ->
            when (event) {
                "accept" -> {
                    viewModel.settings.setPrivacyAccepted(true)
                    viewModel.refreshAll()
                }
                "open_settings" -> openSettings()
                "back" -> screen = "main"
                "privacy" -> showPolicy = true
                "red_on" -> viewModel.settings.update { it.copy(forceRed = true) }
                "red_off" -> viewModel.settings.update { it.copy(forceRed = false) }
                "dim" -> dimmed = true
                "wake" -> wake()
                "delay_15" -> viewModel.settings.update { it.copy(dimDelaySec = 15) }
                "delay_60" -> viewModel.settings.update { it.copy(dimDelaySec = 60) }
            }
        }
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        calendarGranted = granted
        if (granted) viewModel.refreshCalendar()
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) viewModel.refreshWeather(force = true)
    }

    fun requestCalendar() {
        if (viewModel.hasCalendarPermission()) {
            calendarGranted = true
            viewModel.refreshCalendar()
        } else {
            calendarLauncher.launch(android.Manifest.permission.READ_CALENDAR)
        }
    }

    fun requestLocation() {
        if (viewModel.hasLocationPermission) {
            locationGranted = true
            viewModel.refreshWeather(force = true)
        } else {
            locationLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun openNotificationAccess() {
        runCatching {
            context.startActivity(
                Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // 时钟节奏：秒针开启时才逐秒刷新，否则对齐到分钟，减少唤醒。
    LaunchedEffect(screen, dimmed, settings.showSeconds, resumed) {
        if (screen == "main" && !dimmed && resumed) {
            while (true) {
                now = LocalDateTime.now()
                if (settings.showSeconds) {
                    val ms = System.currentTimeMillis()
                    delay((1000 - ms % 1000).coerceAtLeast(120))
                } else {
                    val ms = System.currentTimeMillis()
                    delay((60_000 - ms % 60_000).coerceAtLeast(150))
                }
            }
        }
    }

    // 低功耗自动变黑。
    LaunchedEffect(interaction, settings.dimDelaySec, screen, dimmed, resumed) {
        if (screen == "main" && !dimmed && resumed) {
            delay(settings.dimDelaySec * 1000L)
            dimmed = true
        }
    }

    // 防烧屏微位移，每三分钟移动一次，不做持续动画。
    LaunchedEffect(settings.burnInShift, screen, dimmed) {
        if (settings.burnInShift && screen == "main" && !dimmed) {
            while (true) {
                delay(180_000)
                shift = (shift + 1) % 4
            }
        } else {
            shift = 0
        }
    }

    // 光线传感器：自动红光。1Hz 采样，足够且省电。
    DisposableEffect(settings.autoRed, resumed) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (!settings.autoRed || !resumed || manager == null || sensor == null) {
            lux = Float.NaN
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val value = event.values.firstOrNull() ?: return
                    if (lux.isNaN() || kotlin.math.abs(value - lux) > 0.8f) lux = value
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, 1_000_000, 1_000_000)
            onDispose { manager.unregisterListener(listener) }
        }
    }

    // 变黑后仅在需要唤醒时开启加速度计；亮屏时关闭。
    DisposableEffect(dimmed, resumed) {
        if (!dimmed || !resumed) {
            onDispose { }
        } else {
            val manager = context.getSystemService(SensorManager::class.java)
            val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                ?: manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            if (manager == null || sensor == null) {
                onDispose { }
            } else {
                val started = SystemClock.elapsedRealtime()
                val gravity = FloatArray(3)
                var hasGravity = false
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (SystemClock.elapsedRealtime() - started < 450) return
                        val x = event.values.getOrElse(0) { 0f }
                        val y = event.values.getOrElse(1) { 0f }
                        val z = event.values.getOrElse(2) { 0f }
                        val magnitude: Float
                        if (sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                            magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                        } else {
                            if (!hasGravity) {
                                gravity[0] = x; gravity[1] = y; gravity[2] = z
                                hasGravity = true
                                return
                            }
                            gravity[0] = gravity[0] * 0.82f + x * 0.18f
                            gravity[1] = gravity[1] * 0.82f + y * 0.18f
                            gravity[2] = gravity[2] * 0.82f + z * 0.18f
                            val lx = x - gravity[0]
                            val ly = y - gravity[1]
                            val lz = z - gravity[2]
                            magnitude = kotlin.math.sqrt(lx * lx + ly * ly + lz * lz)
                        }
                        if (magnitude > 0.35f) wake()
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                onDispose { manager.unregisterListener(listener) }
            }
        }
    }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        var previousCharging: Boolean? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                battery = BatteryInfo(
                    level = (level * 100 / scale).coerceIn(0, 100),
                    charging = charging,
                    plugged = plugged != 0,
                    temperatureC = temp
                )
                if (previousCharging == false && charging && dimmed) wake()
                previousCharging = charging
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // 从系统设置返回时刷新权限和媒体会话。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumed = true
                    calendarGranted = viewModel.hasCalendarPermission()
                    locationGranted = viewModel.hasLocationPermission
                    MediaSessionStore.refreshAccess(context)
                    viewModel.refreshAll()
                }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.limitRefreshRate) {
        activity?.window?.let { window ->
            val attrs = window.attributes
            attrs.preferredRefreshRate = if (settings.limitRefreshRate) 30f else 0f
            window.attributes = attrs
        }
    }

    // 长时间停留时，日历与天气做低频刷新；变黑后完全停止。
    LaunchedEffect(screen, dimmed, resumed) {
        if (screen == "main" && !dimmed && resumed) {
            var minutes = 0
            while (true) {
                delay(60_000)
                minutes++
                if (minutes % 5 == 0) viewModel.refreshCalendar()
                if (minutes % 20 == 0) viewModel.refreshWeather(force = false)
            }
        }
    }

    val hasSetupGap = !calendarGranted || !media.accessGranted

    StandbyTheme(redMode = redMode) {
        Box(Modifier.fillMaxSize().background(LocalStandbyColors.current.background)) {
            if (dimmed) {
                BackHandler { wake() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures { wake() }
                        }
                        .semantics {
                            contentDescription = "屏幕已变黑，轻触唤醒"
                        }
                )
            } else {
                BackHandler {
                    if (screen == "main") {
                        // 保持在守夜模式，避免误触返回键退出。
                    } else {
                        screen = "main"
                    }
                }
                if (screen == "settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        agenda = agenda,
                        weatherState = weatherState,
                        media = media,
                        calendarGranted = calendarGranted,
                        locationGranted = locationGranted,
                        notificationGranted = media.accessGranted,
                        onBack = { screen = "main"; noteInteraction() },
                        onRequestCalendar = ::requestCalendar,
                        onRequestLocation = ::requestLocation,
                        onNotificationAccess = ::openNotificationAccess,
                        onOpenPrivacy = { showPolicy = true },
                        onExit = { activity?.finishAndRemoveTask() }
                    )
                } else {
                    MainContent(
                        now = now,
                        settings = settings,
                        weather = weatherState.data,
                        weatherState = weatherState,
                        agenda = agenda,
                        media = media,
                        battery = battery,
                        shift = shift,
                        hasSetupGap = hasSetupGap,
                        widgetPage = widgetPage,
                        onWidgetPageChange = { widgetPage = it },
                        onInteraction = ::noteInteraction,
                        onOpenSettings = ::openSettings,
                        onRefreshWeather = { viewModel.refreshWeather(force = true) },
                        onRequestCalendar = ::requestCalendar
                    )
                }
            }

            if (showPolicy) {
                PolicyScreen(
                    onBack = { showPolicy = false },
                    onRevokeAndClear = {
                        viewModel.clearLocalData()
                        showPolicy = false
                        activity?.finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    now: LocalDateTime,
    settings: Settings,
    weather: WeatherData?,
    weatherState: WeatherState,
    agenda: AgendaState,
    media: MediaState,
    battery: BatteryInfo,
    shift: Int,
    hasSetupGap: Boolean,
    widgetPage: Int,
    onWidgetPageChange: (Int) -> Unit,
    onInteraction: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
    onRequestCalendar: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) onInteraction()
                    }
                }
            }
    ) {
        val actualLandscape = maxWidth > maxHeight
        val colors = LocalStandbyColors.current
        val padH = if (actualLandscape) 26.dp else 20.dp
        val padV = if (actualLandscape) 14.dp else 16.dp
        val shiftX = if (settings.burnInShift) ((shift % 2) * 2).dp else 0.dp
        val shiftY = if (settings.burnInShift) (((shift / 2) % 2) * 2).dp else 0.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV)
                .offset(x = shiftX, y = shiftY)
        ) {
            TopBar(
                battery = battery,
                hasSetupGap = hasSetupGap,
                onOpenSettings = onOpenSettings
            )
            Spacer(Modifier.height(if (actualLandscape) 4.dp else 8.dp))

            if (actualLandscape) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1.13f).fillMaxHeight()) {
                        ClockPanel(
                            now = now,
                            use24Hour = settings.use24Hour,
                            showSeconds = settings.showSeconds,
                            landscape = true,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WeatherMiniRow(weather)
                            Spacer(Modifier.width(12.dp))
                            if (hasSetupGap) {
                                Text(
                                    text = "完善权限",
                                    color = colors.accent,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                        .semantics {
                                            contentDescription = "打开设置完善权限"
                                            role = Role.Button
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures { onOpenSettings() }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(22.dp))
                    WidgetStack(
                        pages = buildList {
                            if (settings.widgetCalendar) add(WidgetPage.CALENDAR)
                            if (settings.widgetMusic) add(WidgetPage.MUSIC)
                        },
                        agenda = agenda,
                        media = media,
                        use24Hour = settings.use24Hour,
                        hasCalendarPermission = agenda.permissionGranted,
                        onRequestCalendar = onRequestCalendar,
                        onOpenSettings = onOpenSettings,
                        pageIndex = widgetPage,
                        onPageIndexChange = onWidgetPageChange,
                        modifier = Modifier.weight(0.87f).fillMaxHeight()
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ClockPanel(
                        now = now,
                        use24Hour = settings.use24Hour,
                        showSeconds = settings.showSeconds,
                        landscape = false,
                        modifier = Modifier.weight(1.0f).fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    WeatherPanel(
                        weather = weather,
                        state = weatherState,
                        compact = true,
                        onRefresh = onRefreshWeather,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    WidgetStack(
                        pages = buildList {
                            if (settings.widgetCalendar) add(WidgetPage.CALENDAR)
                            if (settings.widgetMusic) add(WidgetPage.MUSIC)
                        },
                        agenda = agenda,
                        media = media,
                        use24Hour = settings.use24Hour,
                        hasCalendarPermission = agenda.permissionGranted,
                        onRequestCalendar = onRequestCalendar,
                        onOpenSettings = onOpenSettings,
                        pageIndex = widgetPage,
                        onPageIndexChange = onWidgetPageChange,
                        modifier = Modifier.weight(1.28f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    battery: BatteryInfo,
    hasSetupGap: Boolean,
    onOpenSettings: () -> Unit
) {
    val colors = LocalStandbyColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BrandMark()
        Row(verticalAlignment = Alignment.CenterVertically) {
            BatteryPill(battery.level, battery.charging)
            Spacer(Modifier.width(8.dp))
            Box {
                IconTap(
                    description = "设置",
                    onClick = onOpenSettings
                ) {
                    GearGlyph(19.dp, colors.secondary)
                }
                if (hasSetupGap) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}
