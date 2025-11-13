package me.erik_hennig.shiftplanimporter.calendar

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
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
            CalendarContract.Calendars.CONTENT_URI, projection, selection, arrayOf("1", "1"), null
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
            val values = ContentValues().apply {
                shiftEvent.template.let {
                    put(CalendarContract.Events.TITLE, it.summary)
                    put(CalendarContract.Events.DESCRIPTION, it.description)
                    // TODO: use correct calendar
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)

                    when (it.times) {
                        null -> {
                            val day = shiftEvent.date.atStartOfDayIn(zone)
                            // TODO: fix properly even with different time zones
                            // ugly workaround to make all day event show in correct day
                            val halfADay = 12 * 3600 * 1000
                            val dayMillis = day.toEpochMilliseconds() + halfADay

                            Log.i(TAG, "Start of day $day")

                            put(CalendarContract.Events.ALL_DAY, 1)
                            put(CalendarContract.Events.DTSTART, dayMillis)
                            put(CalendarContract.Events.DTEND, dayMillis)
                        }
                        else -> {
                            val start = LocalDateTime(shiftEvent.date, it.times.start)
                            val overnight = if (it.times.start < it.times.end) { 0 } else { 1 }
                            val end = LocalDateTime(
                                shiftEvent.date.plus(overnight, DateTimeUnit.DAY),
                                it.times.end
                            )

                            val startMillis = start.toInstant(zone).toEpochMilliseconds()
                            val endMillis = end.toInstant(zone).toEpochMilliseconds()
                            put(CalendarContract.Events.DTSTART, startMillis)
                            put(CalendarContract.Events.DTEND, endMillis)
                        }
                    }
                }
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
