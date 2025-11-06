package me.erik_hennig.shiftplanimporter.calendar

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import me.erik_hennig.shiftplanimporter.data.Shift
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import kotlin.time.ExperimentalTime

private const val TAG = "CalendarImporter"

/**
 * Imports the given list of shift events into the user's primary calendar.
 * Note: This function requires the WRITE_CALENDAR and READ_CALENDAR permissions.
 */
@OptIn(ExperimentalTime::class)
fun importShiftsToCalendar(context: Context, shifts: List<ShiftEvent>) {
    try {
        // Get primary calendar ID
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection =
            "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.IS_PRIMARY} = ?"
        var calendarId: Long? = null
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            arrayOf("1", "1"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                calendarId = cursor.getLong(0)
            }
        }

        // Fallback to first visible calendar if no primary is found
        if (calendarId == null) {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = ?",
                arrayOf("1"),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    calendarId = cursor.getLong(0)
                }
            }
        }

        if (calendarId == null) {
            Log.e(TAG, "No writable calendar found.")
            Toast.makeText(context, "No calendar found to import shifts into", Toast.LENGTH_LONG)
                .show()
            return
        }

        val zone = TimeZone.currentSystemDefault()
        for (shiftEvent in shifts) {
            val (startTime, endTime) = when (shiftEvent.kind) {
                Shift.MORNING -> LocalTime(6, 0) to LocalTime(14, 0)
                Shift.EVENING -> LocalTime(14, 0) to LocalTime(22, 0)
                Shift.NIGHT -> LocalTime(22, 0) to LocalTime(6, 0)
                Shift.DAY -> LocalTime(9, 0) to LocalTime(17, 0)
            }

            val startLocalDateTime = LocalDateTime(shiftEvent.date, startTime)
            val endLocalDateTime = if (shiftEvent.kind == Shift.NIGHT) {
                LocalDateTime(shiftEvent.date.plus(1, DateTimeUnit.DAY), endTime)
            } else {
                LocalDateTime(shiftEvent.date, endTime)
            }

            val startMillis = startLocalDateTime.toInstant(zone).toEpochMilliseconds()
            val endMillis = endLocalDateTime.toInstant(zone).toEpochMilliseconds()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, shiftEvent.kind.displayName)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }
        Log.i(TAG, "Successfully imported ${shifts.size} shifts.")
        Toast.makeText(context, "Successfully imported ${shifts.size} shifts", Toast.LENGTH_SHORT)
            .show()
    } catch (e: SecurityException) {
        Log.e(TAG, "Permission denied for calendar access.", e)
        Toast.makeText(context, "Calendar permission denied", Toast.LENGTH_LONG).show()
    }
}
