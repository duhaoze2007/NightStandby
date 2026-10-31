package com.duhaoze.standby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duhaoze.standby.BuildConfig
import com.duhaoze.standby.StandbyViewModel
import com.duhaoze.standby.data.AgendaState
import com.duhaoze.standby.data.MediaState
import com.duhaoze.standby.data.Settings
import com.duhaoze.standby.data.WeatherState
import kotlin.math.abs

@Composable
fun SettingsScreen(
    viewModel: StandbyViewModel,
    agenda: AgendaState,
    weatherState: WeatherState,
    media: MediaState,
    calendarGranted: Boolean,
    locationGranted: Boolean,
    notificationGranted: Boolean,
    onBack: () -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestLocation: () -> Unit,
    onNotificationAccess: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onExit: () -> Unit
) {
    val settings by viewModel.settings.state.collectAsState()
    val colors = LocalStandbyColors.current
    var city by remember(settings.cityQuery) { mutableStateOf(settings.cityQuery) }
    var confirmClear by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTap(description = "返回", onClick = onBack) {
                    ChevronLeftGlyph(20.dp, colors.primary)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "设置",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "仅天气与定位联网",
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
            ThinDivider(Modifier.padding(horizontal = 20.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { SettingsSection("显示") }
                item {
                    ToggleRow(
                        title = "24 小时制",
                        subtitle = "大时钟与所有时间显示",
                        checked = settings.use24Hour,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(use24Hour = value) }
                        }
                    )
                }
                item {
                    ToggleRow(
                        title = "显示秒",
                        subtitle = "关闭时对齐分钟刷新，更省电",
                        checked = settings.showSeconds,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(showSeconds = value) }
                        }
                    )
                }
                item {
                    ToggleRow(
                        title = "限制为 30Hz",
                        subtitle = "降低屏幕刷新率，适合长时间充电摆放",
                        checked = settings.limitRefreshRate,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(limitRefreshRate = value) }
                        }
                    )
                }
                item {
                    ToggleRow(
                        title = "防烧屏微位移",
                        subtitle = "每三分钟轻移 2dp，避免 OLED 残影",
                        checked = settings.burnInShift,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(burnInShift = value) }
                        }
                    )
                }

                item { SettingsSection("夜间红光") }
                item {
                    ToggleRow(
                        title = "弱光自动转红",
                        subtitle = "光线低于阈值时，界面只保留暗红色像素",
                        checked = settings.autoRed,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(autoRed = value) }
                        }
                    )
                }
                item {
                    ToggleRow(
                        title = "手动红光",
                        subtitle = "强制使用红光配色，适合睡前",
                        checked = settings.forceRed,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(forceRed = value) }
                        }
                    )
                }
                item {
                    SettingBlock(title = "红光触发阈值", subtitle = "环境光低于该值时自动切换") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2f, 5f, 8f, 15f, 30f).forEach { lux ->
                                ChoiceChip(
                                    text = "$lux lx",
                                    selected = abs(settings.redThresholdLux - lux) < 0.1f,
                                    onClick = {
                                        viewModel.settings.update { it.copy(redThresholdLux = lux) }
                                    }
                                )
                            }
                        }
                    }
                }

                item { SettingsSection("自动变黑") }
                item {
                    SettingBlock(
                        title = "无操作后变黑",
                        subtitle = "屏幕保持点亮，只把像素变为纯黑；轻触或震动即唤醒"
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15 to "15 秒", 30 to "30 秒", 60 to "1 分钟", 120 to "2 分钟", 300 to "5 分钟")
                                .forEach { (sec, label) ->
                                    ChoiceChip(
                                        text = label,
                                        selected = settings.dimDelaySec == sec,
                                        onClick = {
                                            viewModel.settings.update { it.copy(dimDelaySec = sec) }
                                        }
                                    )
                                }
                        }
                    }
                }

                item { SettingsSection("天气与定位") }
                item {
                    SettingBlock(
                        title = "城市",
                        subtitle = "留空时使用系统最后已知位置；定位失败则按时区选择城市"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = city,
                                onValueChange = { city = it.take(24) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.primary,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, colors.lineStrong, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (city.isBlank()) {
                                            Text(
                                                text = "例如：吉隆坡",
                                                color = colors.tertiary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            AccentPill("保存") {
                                viewModel.settings.update { it.copy(cityQuery = city.trim()) }
                                viewModel.refreshWeather(force = true)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                text = if (locationGranted) "使用当前位置" else "启用定位",
                                selected = false,
                                onClick = onRequestLocation
                            )
                            ChoiceChip(
                                text = "刷新天气",
                                selected = false,
                                onClick = { viewModel.refreshWeather(force = true) }
                            )
                        }
                    }
                }
                item {
                    val data = weatherState.data
                    InfoRow(
                        title = if (data != null) {
                            "${data.city} · ${data.temperatureText} ${data.kind.label}"
                        } else if (weatherState.loading) {
                            "正在获取天气…"
                        } else {
                            "天气尚未获取"
                        },
                        subtitle = when {
                            data == null && weatherState.error != null -> weatherState.error ?: ""
                            data == null -> "连接 Open-Meteo 获取天气；不需要账号。"
                            data.stale -> "离线缓存 · ${viewModel.relativeUpdateLabel(data.updatedAt)}"
                            else -> "Open-Meteo · ${viewModel.relativeUpdateLabel(data.updatedAt)}"
                        }
                    )
                }
                item {
                    PermissionRow(
                        title = "定位权限",
                        subtitle = if (locationGranted) "仅用于天气；可选择精确或大致位置" else "未授权，使用城市或时区回退",
                        granted = locationGranted,
                        action = if (locationGranted) "已开启" else "开启",
                        onAction = onRequestLocation
                    )
                }

                item { SettingsSection("小组件") }
                item {
                    ToggleRow(
                        title = "日历小组件",
                        subtitle = "读取系统日历中的今天与未来 7 天日程",
                        checked = settings.widgetCalendar,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(widgetCalendar = value) }
                        }
                    )
                }
                item {
                    ToggleRow(
                        title = "音乐小组件",
                        subtitle = "显示并控制当前媒体会话",
                        checked = settings.widgetMusic,
                        onToggle = { value ->
                            viewModel.settings.update { it.copy(widgetMusic = value) }
                        }
                    )
                }
                item {
                    Text(
                        text = "在主页面向上下滑动小组件，可在日历与音乐之间切换。",
                        color = colors.tertiary,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item { SettingsSection("权限") }
                item {
                    PermissionRow(
                        title = "日历",
                        subtitle = if (calendarGranted) {
                            "已授权，只读 ${agenda.today.size + agenda.upcoming.size} 项日程"
                        } else {
                            "未授权，日程小组件将显示引导"
                        },
                        granted = calendarGranted,
                        action = if (calendarGranted) "已开启" else "授权",
                        onAction = onRequestCalendar
                    )
                }
                item {
                    PermissionRow(
                        title = "通知使用权 · 音乐",
                        subtitle = if (notificationGranted) {
                            "已授权；只读取媒体会话，不读取通知内容"
                        } else {
                            "用于读取当前播放器并显示控制按钮"
                        },
                        granted = notificationGranted,
                        action = if (notificationGranted) "已开启" else "开启",
                        onAction = onNotificationAccess
                    )
                }

                item { SettingsSection("隐私与数据") }
                item {
                    ActionRow(
                        title = "隐私政策",
                        subtitle = "联网范围、权限说明与本机数据",
                        action = "查看",
                        onAction = onOpenPrivacy
                    )
                }
                item {
                    InfoRow(
                        title = "无账号 · 无广告 · 无统计分析",
                        subtitle = "日历、媒体与传感器数据从不离开本机。天气与定位请求只发送粗略坐标或城市名，不含设备标识。"
                    )
                }
                item {
                    SettingBlock(
                        title = "清除本地数据",
                        subtitle = "删除设置、天气缓存与授权记录；系统权限需在系统设置中撤回"
                    ) {
                        AccentPill(
                            text = if (confirmClear) "再次点击确认清除" else "清除并退出",
                            onClick = {
                                if (confirmClear) {
                                    viewModel.clearLocalData()
                                    onExit()
                                } else {
                                    confirmClear = true
                                }
                            }
                        )
                    }
                }

                item { SettingsSection("关于") }
                item {
                    InfoRow(
                        title = "守夜 Standby  ${BuildConfig.VERSION_NAME}",
                        subtitle = "为 Xiaomi 17 Pro · HyperOS 4 的 OLED 屏幕调校。竖屏与横屏都可用。"
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    AccentPill("退出守夜", onExit)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    val colors = LocalStandbyColors.current
    Spacer(Modifier.height(26.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(colors.accent))
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.2.sp
        )
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalStandbyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, color = colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, color = colors.tertiary, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                checkedBorderColor = colors.accent,
                uncheckedThumbColor = colors.tertiary,
                uncheckedTrackColor = colors.surfaceRaised,
                uncheckedBorderColor = colors.lineStrong
            )
        )
    }
    ThinDivider()
}

@Composable
private fun SettingBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val colors = LocalStandbyColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(text = title, color = colors.primary, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(text = subtitle, color = colors.tertiary, fontSize = 10.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
    ThinDivider()
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalStandbyColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(
                1.dp,
                if (selected) colors.accent else colors.lineStrong,
                RoundedCornerShape(50)
            )
            .background(if (selected) colors.accentSoft else colors.surface)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) colors.accent else colors.secondary,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    action: String,
    onAction: () -> Unit
) {
    val colors = LocalStandbyColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (granted) colors.accent else colors.tertiary)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, color = colors.primary, fontSize = 15.sp)
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, color = colors.tertiary, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = action,
            color = if (granted) colors.tertiary else colors.accent,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = !granted, onClick = onAction)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
    ThinDivider()
}

@Composable
private fun ActionRow(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    val colors = LocalStandbyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, color = colors.primary, fontSize = 15.sp)
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, color = colors.tertiary, fontSize = 10.sp)
        }
        Text(text = action, color = colors.accent, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        ChevronLeftGlyph(13.dp, colors.tertiary)
    }
    ThinDivider()
}

@Composable
private fun InfoRow(title: String, subtitle: String) {
    val colors = LocalStandbyColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(text = title, color = colors.primary, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Text(text = subtitle, color = colors.tertiary, fontSize = 10.sp, lineHeight = 15.sp)
    }
    ThinDivider()
}
