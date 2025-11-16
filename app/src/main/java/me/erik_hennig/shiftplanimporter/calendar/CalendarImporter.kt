package me.erik_hennig.shiftplanimporter.calendar

import android.content.ContentUris
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
import me.erik_hennig.shiftplanimporter.R
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import kotlin.time.ExperimentalTime

private const val TAG = "CalendarImporter"

data class EventColor(val key: String, val color: Int)
data class CalendarInfo(
    val id: Long,
    val name: String,
    val isPrimary: Boolean,
    val availableColors: Set<EventColor> = emptySet()
)

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
            val colors = getAvailableEventColors(context, id)
            calendars.add(CalendarInfo(id, name, isPrimary, colors))
        }
    }
    calendars.sortWith(compareByDescending { it.isPrimary })

    Log.d(TAG, "Loaded calendars: $calendars")

    return calendars
}

private fun getAvailableEventColors(context: Context, calendarId: Long): Set<EventColor> {
    var accountName: String? = null
    var accountType: String? = null

    context.contentResolver.query(
        ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId),
        arrayOf(CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            accountName = cursor.getString(0)
            accountType = cursor.getString(1)
        }
    }

    if (accountName == null || accountType == null) {
        Log.w(
            TAG,
            "Failed to load event colors for calendar $calendarId has no account name $accountName or account type $accountType."
        )
        return emptySet()
    }

    val colors = mutableSetOf<EventColor>()

    val uri = CalendarContract.Colors.CONTENT_URI
    context.contentResolver.query(
        uri,
        arrayOf(CalendarContract.Colors.COLOR_KEY, CalendarContract.Colors.COLOR),
        "${CalendarContract.Colors.ACCOUNT_NAME} = ? AND ${CalendarContract.Colors.ACCOUNT_TYPE} = ? AND ${CalendarContract.Colors.COLOR_TYPE} = ?",
        arrayOf(accountName, accountType, CalendarContract.Colors.TYPE_EVENT.toString()),
        null
    )?.use { cursor ->
        val keyColumn = cursor.getColumnIndexOrThrow(CalendarContract.Colors.COLOR_KEY)
        val colorColumn = cursor.getColumnIndexOrThrow(CalendarContract.Colors.COLOR)
        while (cursor.moveToNext()) {
            val key = cursor.getString(keyColumn)
            val color = cursor.getInt(colorColumn)
            colors.add(EventColor(key, color))
        }
    }

    Log.d(TAG, "Available event colors for calendar $calendarId: $colors")
    return colors
}


/**
 * Imports the given list of shift events into the user's primary calendar.
 * Note: This function requires the WRITE_CALENDAR and READ_CALENDAR permissions.
 */
@OptIn(ExperimentalTime::class)
fun importShiftsToCalendar(context: Context, shifts: List<ShiftEvent>) {
    var imported = 0
    try {
        val zone = TimeZone.currentSystemDefault()

        for (shiftEvent in shifts) {
            shiftEvent.template?.let {
                val calendarId = it.calendarId

                val values = ContentValues().apply {
                    put(CalendarContract.Events.TITLE, it.summary)
                    put(CalendarContract.Events.DESCRIPTION, it.description)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                    put(CalendarContract.Events.EVENT_COLOR_KEY, it.eventColorKey)

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
                                false -> 1
                                true -> 0
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
                Log.i(TAG, "Inserting event $values")
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                imported++
            }
        }
        Log.i(TAG, "Successfully imported ${shifts.size} shifts.")
        val toastText = context.resources.getQuantityString(
            R.plurals.successfully_imported_shifts, shifts.size, shifts.size
        )
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e(TAG, "Permission denied for calendar access.", e)
        Toast.makeText(context, R.string.calendar_permission_denied, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        val failed = shifts.size - imported
        Log.e(TAG, "Failed to import $failed shifts due to an exception.", e)
        val toastText = context.resources.getQuantityString(
            R.plurals.failed_to_import_shifts, failed, failed, shifts.size
        )
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
    }
}
