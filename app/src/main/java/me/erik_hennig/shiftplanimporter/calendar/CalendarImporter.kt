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

data class CalendarInfo(val id: Long, val name: String, val isPrimary: Boolean)

/**
 * Returns a list of available calendars.
 * Note: This function requires the READ_CALENDAR permission.
 */
fun getCalendars(context: Context): List<CalendarInfo> {
    val calendars = mutableListOf<CalendarInfo>()
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.IS_PRIMARY
    )
    val selection =
        "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
    val selectionArgs = arrayOf("1", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

    context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, selection, selectionArgs, null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val isPrimaryColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val isPrimary = cursor.getInt(isPrimaryColumn) == 1
            calendars.add(CalendarInfo(id, name, isPrimary))
        }
    }
    calendars.sortWith(compareByDescending { it.isPrimary })

    Log.d(TAG, "Loaded calendars: $calendars")

    return calendars
}

/**
 * Imports the given list of shift events into the user's primary calendar.
 * Note: This function requires the WRITE_CALENDAR and READ_CALENDAR permissions.
 */
@OptIn(ExperimentalTime::class)
fun importShiftsToCalendar(context: Context, shifts: List<ShiftEvent>) {
    try {
        val zone = TimeZone.currentSystemDefault()

        for (shiftEvent in shifts) {
            shiftEvent.template?.let {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.TITLE, it.summary)
                    put(CalendarContract.Events.DESCRIPTION, it.description)
                    put(CalendarContract.Events.CALENDAR_ID, it.calendarId)
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
                            val overnight = when (it.times.start < it.times.end) {
                                false -> 0
                                true -> 1
                            }
                            val end = LocalDateTime(
                                shiftEvent.date.plus(overnight, DateTimeUnit.DAY), it.times.end
                            )

                            val startMillis = start.toInstant(zone).toEpochMilliseconds()
                            val endMillis = end.toInstant(zone).toEpochMilliseconds()
                            put(CalendarContract.Events.DTSTART, startMillis)
                            put(CalendarContract.Events.DTEND, endMillis)
                        }
                    }

                }
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
        }
        Log.i(TAG, "Successfully imported ${shifts.size} shifts.")
        Toast.makeText(context, "Successfully imported ${shifts.size} shifts", Toast.LENGTH_SHORT)
            .show()
    } catch (e: SecurityException) {
        Log.e(TAG, "Permission denied for calendar access.", e)
        Toast.makeText(context, "Calendar permission denied", Toast.LENGTH_LONG).show()
    }
}
