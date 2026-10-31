package com.duhaoze.standby.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun load(): AgendaState = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext AgendaState(permissionGranted = false)
        }
        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, start)
            ContentUris.appendId(builder, end)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME
            )

            val events = mutableListOf<AgendaEvent>()
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val locationIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val calendarIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val begin = cursor.getLong(beginIdx)
                    val eventEnd = cursor.getLong(endIdx).let { if (it <= begin) begin + 30 * 60_000L else it }
                    events += AgendaEvent(
                        id = cursor.getLong(idIdx),
                        title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() } ?: "未命名日程",
                        startMillis = begin,
                        endMillis = eventEnd,
                        allDay = cursor.getInt(allDayIdx) == 1,
                        location = cursor.getString(locationIdx)?.takeIf { it.isNotBlank() },
                        calendarName = cursor.getString(calendarIdx)
                    )
                }
            }

            val todayEvents = events.filter {
                val day = Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate()
                day == today || (it.allDay && it.endMillis > start)
            }
            val todayKeys = todayEvents.map { it.id to it.startMillis }.toSet()
            val upcoming = events.filter { (it.id to it.startMillis) !in todayKeys }
                .distinctBy { it.id to it.startMillis }
                .take(12)

            AgendaState(
                permissionGranted = true,
                today = todayEvents.distinctBy { it.id to it.startMillis }.take(12),
                upcoming = upcoming,
                loading = false,
                updatedAt = System.currentTimeMillis()
            )
        } catch (t: Throwable) {
            AgendaState(
                permissionGranted = hasPermission(),
                loading = false,
                error = "无法读取系统日历",
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
