package com.duhaoze.standby.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyGate(onAccept: () -> Unit, onDecline: () -> Unit) {
    val colors = LocalStandbyColors.current
    var policyVisible by remember { mutableStateOf(false) }
    if (policyVisible) {
        PolicyScreen(
            onBack = { policyVisible = false },
            onRevokeAndClear = onDecline
        )
        return
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 620.dp)
            ) {
                Text(
                    text = "守夜之前",
                    color = colors.primary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "一块安静、离线优先的充电床头屏。\n只在天气与定位时联网。",
                    color = colors.secondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(30.dp))
                PrivacyPoint("01", "仅天气与定位联网", "天气请求只发送城市名或粗略坐标；其余功能不访问网络。")
                PrivacyPoint("02", "日历只读，音乐只控制", "日程读取自系统日历；音乐通过媒体会话控制，不读取通知内容。")
                PrivacyPoint("03", "数据留在本机", "无账号、无广告、无统计分析，也不会上传你的使用记录。")
                Spacer(Modifier.height(26.dp))
                Text(
                    text = "查看完整隐私政策",
                    color = colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { policyVisible = true }
                        .semantics {
                            contentDescription = "查看完整隐私政策"
                            role = Role.Button
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth()) {
                BigOutlineButton("同意并开始", onAccept)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "继续即表示你已阅读并同意本隐私政策。守夜完全离线可用，除天气外不需要任何账号。",
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "不同意，退出",
                    color = colors.tertiary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDecline)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyPoint(number: String, title: String, body: String) {
    val colors = LocalStandbyColors.current
    Row(Modifier.padding(bottom = 20.dp)) {
        Text(
            text = number,
            color = colors.accent,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                color = colors.tertiary,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun PolicyScreen(onBack: () -> Unit, onRevokeAndClear: () -> Unit) {
    val colors = LocalStandbyColors.current
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler { onBack() }
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
                    text = "隐私政策",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
            }
            ThinDivider(Modifier.padding(horizontal = 20.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 700.dp).fillMaxWidth()) {
                    Text(
                        text = "守夜 Standby 隐私政策",
                        color = colors.primary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "生效日期：2026 年 10 月 31 日  ·  版本 1.0.0",
                        color = colors.tertiary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "一句话总结：守夜不建账号、不做广告、不统计使用行为。日历、媒体会话和传感器只在本机处理；只有天气和定位会联网。",
                            color = colors.primary,
                            fontSize = 13.sp,
                            lineHeight = 21.sp
                        )
                    }

                    PolicySection(
                        "1. 我们访问哪些数据",
                        "• 天气与定位：当你启用天气时，应用会读取系统最后已知的粗略位置，或使用你在设置里填写的城市名。随后向 Open-Meteo 发送一个保留三位小数的粗略坐标或城市名，以获取当前天气、逐小时与未来几日预报。请求不包含账号、广告标识、通讯录、设备唯一标识或使用记录。\n\n" +
                            "• 系统日历：在你授权 READ_CALENDAR 后，只读访问今天与未来 7 天的日程，用于日历小组件。应用不会创建、修改或删除日程。\n\n" +
                            "• 媒体会话：在你开启“通知使用权”后，应用通过 Android MediaSession 读取当前播放器的曲名、艺术家、专辑、封面、播放状态和进度，用于显示音乐卡片与播放控制。应用不读取、保存或上传任何通知内容。\n\n" +
                            "• 传感器：光线传感器用于判断环境亮度并切换红光；加速度计只在你手动开启“自动变黑”且屏幕已黑时工作，用来检测震动唤醒。传感器数据不会存储，也不会离开设备。\n\n" +
                            "• 设置与缓存：主题、城市、权限状态和最近一次天气数据保存在应用私有目录。应用不启用云备份。"
                    )
                    PolicySection(
                        "2. 联网范围",
                        "守夜只为天气与定位联网。天气使用 Open-Meteo（api.open-meteo.com 与 geocoding-api.open-meteo.com）。定位优先使用系统已缓存的最后位置，不主动持续定位。\n\n" +
                            "时钟、日历、媒体控制、夜间红光、自动变黑与小组件堆放均不发起网络请求。应用没有接入广告、统计、推送或崩溃上报 SDK。\n\n" +
                            "天气与定位服务可能受网络状况、地区可用性和第三方服务条款影响。若你将应用设置为不允许联网，除天气外的全部功能仍可正常使用。"
                    )
                    PolicySection(
                        "3. 数据如何保存与删除",
                        "所有数据保存在设备本机的应用私有存储中。你可以随时在“设置 → 隐私与数据 → 清除本地数据”中删除设置、天气缓存和本机记录。卸载应用会同时删除应用私有数据。\n\n" +
                            "系统级权限（日历、定位、通知使用权）需要在系统设置中单独撤回。撤回后对应的日程或音乐卡片将停止读取数据。"
                    )
                    PolicySection(
                        "4. 我们不做哪些事",
                        "守夜不收集姓名、手机号、通讯录、短信、照片、麦克风或摄像头数据；不追踪跨应用行为；不向第三方出售或共享个人数据；不要求登录；不展示广告。\n\n" +
                            "天气服务收到的粗略坐标仅用于返回当地天气，不用于建立用户画像。"
                    )
                    PolicySection(
                        "5. 第三方组件",
                        "应用使用 AndroidX / Jetpack Compose、Kotlin 与 Android 系统 API 构建。天气数据由 Open-Meteo 提供。上述组件不包含广告或行为分析 SDK。第三方天气服务对其自身处理行为负责，建议你阅读 Open-Meteo 的隐私说明。"
                    )
                    PolicySection(
                        "6. 儿童隐私",
                        "守夜不是面向儿童设计的服务，也不会主动收集儿童的任何个人信息。由于应用没有账号系统，我们无法识别用户年龄。"
                    )
                    PolicySection(
                        "7. 政策更新",
                        "如果未来版本改变联网范围或权限用途，我们会在应用内更新本政策并修改生效日期。重大变化会要求在应用内重新确认。\n\n" +
                            "本版本没有后台联网同步，也没有远程配置。"
                    )
                    PolicySection(
                        "8. 联系与撤回同意",
                        "本应用不运营服务器，也没有在线客服。你可以通过应用的分发渠道联系开发者，或在系统设置中撤回权限。\n\n" +
                            "你可以随时清除本地数据并退出，撤回对隐私政策的同意。撤回后应用将回到首次启动状态。"
                    )

                    Spacer(Modifier.height(12.dp))
                    AccentPill(
                        text = if (confirmClear) "再次点击，清除本地数据并退出" else "清除本地数据并退出",
                        onClick = {
                            if (confirmClear) {
                                onRevokeAndClear()
                            } else {
                                confirmClear = true
                            }
                        }
                    )
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    val colors = LocalStandbyColors.current
    Spacer(Modifier.height(24.dp))
    Text(
        text = title,
        color = colors.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = body,
        color = colors.secondary,
        fontSize = 12.sp,
        lineHeight = 20.sp
    )
}

@Composable
fun BigOutlineButton(text: String, onClick: () -> Unit) {
    val colors = LocalStandbyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.accent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.accent,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp
        )
    }
}
