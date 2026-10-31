package com.duhaoze.standby.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duhaoze.standby.data.AgendaEvent
import com.duhaoze.standby.data.AgendaState
import com.duhaoze.standby.data.MediaState
import com.duhaoze.standby.data.formatTimeOfDay
import com.duhaoze.standby.media.MediaSessionStore
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class WidgetPage { CALENDAR, MUSIC }

@Composable
fun WidgetStack(
    pages: List<WidgetPage>,
    agenda: AgendaState,
    media: MediaState,
    use24Hour: Boolean,
    hasCalendarPermission: Boolean,
    onRequestCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    pageIndex: Int,
    onPageIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    if (pages.isEmpty()) {
        WidgetCard(modifier) {
            CardHeader("小组件", Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Text(
                text = "小组件已全部隐藏",
                color = colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "在设置中重新显示日历或音乐",
                color = colors.tertiary,
                fontSize = 11.sp
            )
        }
        return
    }

    LaunchedEffect(pages) {
        if (pageIndex > pages.lastIndex) onPageIndexChange(0)
    }
    val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safeIndex]

    Box(modifier) {
        var drag by remember { mutableLongStateOf(0L) }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (slideInVertically { it * direction } + fadeIn()) togetherWith
                    (slideOutVertically { -it * direction } + fadeOut())
            },
            label = "widget-stack",
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (pages.size > 1) 18.dp else 0.dp)
                .pointerInput(pages) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> drag += amount.toLong() },
                        onDragEnd = {
                            if (drag < -42) onPageIndexChange((safeIndex + 1) % pages.size)
                            if (drag > 42) onPageIndexChange((safeIndex - 1 + pages.size) % pages.size)
                            drag = 0L
                        },
                        onDragCancel = { drag = 0L }
                    )
                }
        ) { target ->
            when (target) {
                WidgetPage.CALENDAR -> AgendaWidget(
                    agenda = agenda,
                    use24Hour = use24Hour,
                    hasPermission = hasCalendarPermission,
                    onRequestPermission = onRequestCalendar,
                    modifier = Modifier.fillMaxSize()
                )
                WidgetPage.MUSIC -> MusicWidget(
                    media = media,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (pages.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pages.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .size(if (index == safeIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == safeIndex) colors.accent else colors.lineStrong)
                            .clickable {
                                onPageIndexChange(index)
                            }
                            .semantics {
                                contentDescription = when (item) {
                                    WidgetPage.CALENDAR -> "日历小组件"
                                    WidgetPage.MUSIC -> "音乐小组件"
                                }
                                role = Role.Button
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun AgendaWidget(
    agenda: AgendaState,
    use24Hour: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    val today = LocalDate.now()
    val events = if (agenda.today.isNotEmpty()) agenda.today else agenda.upcoming
    WidgetCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CardHeader("日历", Modifier.weight(1f))
            Text(
                text = when {
                    !hasPermission || !agenda.permissionGranted -> "需要授权"
                    agenda.today.isNotEmpty() -> "今天 ${agenda.today.size} 项"
                    else -> "接下来 ${agenda.upcoming.size} 项"
                },
                color = colors.tertiary,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        if (!hasPermission || !agenda.permissionGranted) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "让今天有迹可循",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "只读取系统日历，不写入、不上传。",
                    color = colors.tertiary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(16.dp))
                AccentPill(text = "允许读取日历", onClick = onRequestPermission)
            }
        } else if (events.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "今天没有日程",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "留一点空白给自己。",
                    color = colors.tertiary,
                    fontSize = 11.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                events.take(3).forEach { event ->
                    AgendaRow(event, today, use24Hour)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (agenda.today.isNotEmpty()) "来自系统日历" else "接下来的日程",
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${agenda.today.size + agenda.upcoming.size} 项 · 7 天",
                    color = colors.tertiary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun AgendaRow(event: AgendaEvent, today: LocalDate, use24Hour: Boolean) {
    val colors = LocalStandbyColors.current
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
    val dayLabel = when (start.toLocalDate()) {
        today -> ""
        today.plusDays(1) -> "明天 "
        else -> start.format(DateTimeFormatter.ofPattern("M月d日 ", Locale.SIMPLIFIED_CHINESE))
    }
    val timeLabel = when {
        event.allDay -> "全天"
        else -> "$dayLabel${formatTimeOfDay(event.startMillis, use24Hour)} – ${formatTimeOfDay(event.endMillis, use24Hour)}"
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .width(2.dp)
                .height(36.dp)
                .background(colors.accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeLabel,
                color = colors.secondary,
                fontSize = 10.sp,
            )
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MusicWidget(
    media: MediaState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalStandbyColors.current
    val context = LocalContext.current
    LaunchedEffect(media.accessGranted) {
        MediaSessionStore.refreshAccess(context)
        while (media.accessGranted) {
            delay(5_000)
            MediaSessionStore.refreshFromContext(context)
        }
    }
    WidgetCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CardHeader("正在播放", Modifier.weight(1f))
            if (media.active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(colors.accent))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = appName(media.packageName),
                        color = colors.tertiary,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        when {
            !media.accessGranted -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "此刻在听什么？",
                        color = colors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "开启通知使用权后，守夜只读取媒体会话，不读取任何通知内容。",
                        color = colors.tertiary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    AccentPill("开启音乐控制", onOpenSettings)
                }
            }
            !media.active || !media.hasTrack -> {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MusicNoteGlyph(32.dp, colors.tertiary)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "没有正在播放的音乐",
                                color = colors.primary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "在任意播放器中开始播放，这里会自动出现。",
                                color = colors.tertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            else -> {
                NowPlayingContent(media)
            }
        }
    }
}

@Composable
private fun ColumnScope.NowPlayingContent(media: MediaState) {
    val colors = LocalStandbyColors.current
    var position by remember(media.title, media.updatedAt) {
        mutableLongStateOf(media.positionMs)
    }
    LaunchedEffect(media.isPlaying, media.title, media.updatedAt) {
        while (media.isPlaying) {
            val elapsed = SystemClock.elapsedRealtime() - media.updatedAt
            position = (media.positionMs + (elapsed * media.speed).toLong()).coerceAtLeast(0L)
            delay(1000)
        }
    }
    val duration = media.durationMs.coerceAtLeast(0L)
    val displayPosition = position.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceRaised)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val art = media.artwork
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(0f) }
                    )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.accent.copy(alpha = 0.12f))
                )
            } else {
                MusicNoteGlyph(30.dp, colors.secondary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = media.title,
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = media.artist.ifBlank { "未知艺术家" },
                color = colors.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (media.album.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = media.album,
                    color = colors.tertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(media.title, duration, media.canSeek) {
                if (duration > 0 && media.canSeek) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        com.duhaoze.standby.media.MediaSessionStore.seekTo((duration * ratio).toLong())
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(3.dp)) {
            drawRoundRect(
                color = colors.line,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
            if (duration > 0) {
                val ratio = (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                drawRoundRect(
                    color = colors.accent,
                    size = androidx.compose.ui.geometry.Size(size.width * ratio, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                )
                drawCircle(
                    color = colors.accent,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width * ratio, size.height / 2f)
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(displayPosition),
            color = colors.tertiary,
            fontSize = 9.sp,
        )
        Text(
            text = if (duration > 0) formatDuration(duration) else "--:--",
            color = colors.tertiary,
            fontSize = 9.sp,
        )
    }

    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        RoundIconButton(
            description = "上一首",
            onClick = { com.duhaoze.standby.media.MediaSessionStore.previous() },
            bordered = true,
            enabled = media.canPrevious
        )
        Spacer(Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable {
                    com.duhaoze.standby.media.MediaSessionStore.togglePlayPause()
                }
                .semantics {
                    contentDescription = if (media.isPlaying) "暂停" else "播放"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            if (media.isPlaying) {
                PauseGlyph(20.dp, colors.background)
            } else {
                PlayGlyph(20.dp, colors.background)
            }
        }
        Spacer(Modifier.width(24.dp))
        RoundIconButton(
            description = "下一首",
            onClick = { com.duhaoze.standby.media.MediaSessionStore.next() },
            bordered = true,
            enabled = media.canNext
        )
    }
}

@Composable
fun AccentPill(text: String, onClick: () -> Unit) {
    val colors = LocalStandbyColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.accent.copy(alpha = 0.65f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = colors.accent,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun MusicNoteGlyph(dim: androidx.compose.ui.unit.Dp, color: Color) {
    Canvas(Modifier.size(dim)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.62f, s * 0.14f)
            lineTo(s * 0.62f, s * 0.64f)
            cubicTo(s * 0.62f, s * 0.73f, s * 0.54f, s * 0.80f, s * 0.44f, s * 0.80f)
            cubicTo(s * 0.34f, s * 0.80f, s * 0.27f, s * 0.73f, s * 0.27f, s * 0.65f)
            cubicTo(s * 0.27f, s * 0.56f, s * 0.36f, s * 0.50f, s * 0.46f, s * 0.53f)
            lineTo(s * 0.46f, s * 0.26f)
            lineTo(s * 0.82f, s * 0.18f)
            lineTo(s * 0.82f, s * 0.58f)
            lineTo(s * 0.74f, s * 0.60f)
            lineTo(s * 0.74f, s * 0.28f)
            close()
        }
        drawPath(path, color)
        drawLine(
            color = color,
            start = Offset(s * 0.46f, s * 0.26f),
            end = Offset(s * 0.82f, s * 0.18f),
            strokeWidth = s * 0.05f,
            cap = StrokeCap.Round
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun appName(packageName: String): String = when (packageName) {
    "com.netease.cloudmusic" -> "网易云音乐"
    "com.miui.player" -> "小米音乐"
    "com.spotify.music" -> "Spotify"
    "com.google.android.youtube" -> "YouTube"
    "com.apple.android.music" -> "Apple Music"
    "com.kugou.android" -> "酷狗音乐"
    "com.tencent.qqmusic" -> "QQ 音乐"
    "com.duhaoze.standby.debug" -> "本地测试"
    else -> packageName.substringAfterLast('.').take(14)
}
