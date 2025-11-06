package me.erik_hennig.shiftplanimporter

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.EnteringState.EnteringShifts
import me.erik_hennig.shiftplanimporter.EnteringState.Review
import me.erik_hennig.shiftplanimporter.EnteringState.SelectingDateRange
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.ReviewView
import me.erik_hennig.shiftplanimporter.ui.TimeFrameView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun LocalDateRange.withAdjustedStart(offset: Int): LocalDateRange {
    if (this.isEmpty() && offset >= 0) {
        return this
    }

    val newStart = this.start.plus(offset, DateTimeUnit.DAY)
    return LocalDateRange(newStart, this.endInclusive)
}

fun LocalDateRange.withLaterStart(): LocalDateRange = this.withAdjustedStart(1)
fun LocalDateRange.withEarlierStart(): LocalDateRange = this.withAdjustedStart(-1)

fun LocalDate.format(renderFormat: java.text.DateFormat): String {
    val date = java.util.Date.from(
        this.toJavaLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
    )
    return renderFormat.format(date)
}

// TODO: Turn this into configurable list
enum class Shift(val displayName: String) {
    MORNING("Frühdienst"),
    EVENING("Spätdienst"),
    NIGHT("Nachtdienst"),
    DAY("Sonderamt"),
}

data class ShiftEvent(val kind: Shift, val date: LocalDate)

// TODO: Add welcome screen
// TODO: Add settings
sealed class EnteringState {
    object SelectingDateRange : EnteringState()
    data class EnteringShifts(
        val dateRange: LocalDateRange,
        val enteredShifts: List<ShiftEvent> = emptyList(),
    ) : EnteringState() {
        val currentDate: LocalDate
            get() = this.dateRange.start
    }

    data class Review(val enteredShifts: List<ShiftEvent>) : EnteringState()
}

@OptIn(ExperimentalTime::class)
fun LocalDate.Companion.today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val calendarPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterTheme {
                var currentEnteringState by remember {
                    mutableStateOf<EnteringState>(
                        SelectingDateRange
                    )
                }

                val requestPermissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        Log.i(TAG, "Permission request result: $permissions")
                        if (permissions.values.all { it }) {
                            (currentEnteringState as? Review)?.enteredShifts?.let {
                                importShiftsToCalendar(it)
                                currentEnteringState = SelectingDateRange
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Calendar permissions are required to import shifts.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val enteringState = currentEnteringState) {
                        is SelectingDateRange -> {
                            val upcomingMonths = remember {
                                val start = LocalDate.today().yearMonth
                                val end = start.plus(4, DateTimeUnit.MONTH)
                                start..end
                            }
                            TimeFrameView(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                upcomingMonths = upcomingMonths,
                                onTimeFrameSelected = { timeFrame ->
                                    Log.i(TAG, "Selected date range: $timeFrame")
                                    currentEnteringState = EnteringShifts(
                                        dateRange = timeFrame
                                    )
                                }
                            )
                        }

                        is EnteringShifts -> {
                            val onShiftSelected: (Shift) -> Unit = { shift: Shift ->
                                val newEvent =
                                    ShiftEvent(kind = shift, date = enteringState.currentDate)
                                val remainingDays = enteringState.dateRange.withLaterStart()
                                val enteredShifts = enteringState.enteredShifts + newEvent

                                currentEnteringState = if (remainingDays.isEmpty()) {
                                    Review(
                                        enteredShifts = enteredShifts
                                    )
                                } else {
                                    EnteringShifts(
                                        dateRange = remainingDays,
                                        enteredShifts = enteredShifts
                                    )
                                }

                                Log.i(TAG, "Added new shift: $newEvent")
                            }

                            val onSkip: () -> Unit = {
                                val remainingDays = enteringState.dateRange.withLaterStart()

                                currentEnteringState = if (remainingDays.isEmpty()) {
                                    Review(
                                        enteredShifts = enteringState.enteredShifts
                                    )
                                } else {
                                    enteringState.copy(dateRange = remainingDays)
                                }

                                Log.i(TAG, "Skipped day ${enteringState.currentDate}")
                            }

                            val onUndo: () -> Unit = {
                                currentEnteringState = if (enteringState.enteredShifts.isEmpty()) {
                                    SelectingDateRange
                                } else {
                                    val remainingDays = enteringState.dateRange.withEarlierStart()
                                    EnteringShifts(
                                        dateRange = remainingDays,
                                        enteredShifts = enteringState.enteredShifts.dropLast(1),
                                    )
                                }
                                Log.i(
                                    TAG,
                                    "Returning to previous day from day ${enteringState.currentDate}"
                                )
                            }

                            EnterShiftView(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                date = enteringState.currentDate,
                                shiftOptions = Shift.entries,
                                onShiftSelected = onShiftSelected,
                                onUndo = onUndo,
                                onSkip = onSkip
                            )
                        }

                        is Review -> {
                            ReviewView(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                shiftEvents = enteringState.enteredShifts,
                                onEdit = {
                                    // TODO: Implement edit
                                    Log.i(TAG, "Editing shift: ${enteringState.enteredShifts[it]}")
                                },
                                onDiscardAll = {
                                    Log.i(TAG, "Discarding shift selection")
                                    currentEnteringState = SelectingDateRange
                                },
                                onImportAll = {
                                    Log.i(TAG, "Importing shift selection")
                                    val allPermissionsGranted = calendarPermissions.all {
                                        ContextCompat.checkSelfPermission(
                                            this,
                                            it
                                        ) == PackageManager.PERMISSION_GRANTED
                                    }
                                    if (allPermissionsGranted) {
                                        importShiftsToCalendar(enteringState.enteredShifts)
                                        currentEnteringState = SelectingDateRange
                                    } else {
                                        Log.d(TAG, "Requesting missing permissions")
                                        requestPermissionLauncher.launch(calendarPermissions)
                                    }
                                },
                                onExportAll = {
                                    // TODO: Implement export
                                    Log.i(TAG, "Exporting shift selection")
                                    currentEnteringState = SelectingDateRange
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Imports the given list of shift events into the user's primary calendar.
     * Note: This function requires the WRITE_CALENDAR and READ_CALENDAR permissions.
     */
    @OptIn(ExperimentalTime::class)
    private fun importShiftsToCalendar(shifts: List<ShiftEvent>) {
        try {
            // Get primary calendar ID
            val projection = arrayOf(CalendarContract.Calendars._ID)
            val selection =
                "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.IS_PRIMARY} = ?"
            var calendarId: Long? = null
            contentResolver.query(
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
                contentResolver.query(
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
                Toast.makeText(this, "No calendar found to import shifts into", Toast.LENGTH_LONG)
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
                contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
            Log.i(TAG, "Successfully imported ${shifts.size} shifts.")
            Toast.makeText(this, "Successfully imported ${shifts.size} shifts", Toast.LENGTH_SHORT)
                .show()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for calendar access.", e)
            Toast.makeText(this, "Calendar permission denied", Toast.LENGTH_LONG).show()
        }
    }
}
