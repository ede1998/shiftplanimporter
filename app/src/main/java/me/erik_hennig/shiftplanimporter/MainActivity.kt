package me.erik_hennig.shiftplanimporter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.erik_hennig.shiftplanimporter.EnteringState.*
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.TimeFrameView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.util.Calendar
import java.util.Date


data class DateRange(val start: Date, val end: Date) {
    val empty: Boolean
        get() = start.after(end)

    fun withLaterStart(): DateRange {
        return withAdjustedStart(1)
    }

    fun withEarlierStart(): DateRange {
        return withAdjustedStart(-1)
    }

    fun withAdjustedStart(offset: Int): DateRange {
        if (empty && offset >= 0) {
            return this
        }

        val nextDay = Calendar.getInstance().apply {
            time = start
            add(Calendar.DAY_OF_YEAR, offset)
        }.time

        return DateRange(nextDay, end)
    }
}
// TODO: Turn this into configurable list
enum class Shift(val displayName: String) {
    MORNING("Frühdienst"),
    EVENING("Spätdienst"),
    NIGHT("Nachtdienst"),
    DAY("Sonderamt"),
}

data class ShiftEvent(val kind: Shift, val date: Date)

// TODO: Add welcome screen
// TODO: Add settings
// TODO: Export events to calendar
sealed class EnteringState {
    object SelectingDateRange : EnteringState()
    data class EnteringShifts(
        val dateRange: DateRange,
        val enteredShifts: List<ShiftEvent> = emptyList(),
    ) : EnteringState() {
        val currentDate: Date
            get() = this.dateRange.start
    }

    // TODO: make review screen
    data class Review(val enteredShifts: List<ShiftEvent>) : EnteringState()
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterTheme {
                var currentEnteringState by remember { mutableStateOf<EnteringState>(
                    SelectingDateRange
                ) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val enteringState = currentEnteringState) {
                        is SelectingDateRange -> {
                            val upcomingMonths = remember {
                                mutableListOf<Date>().apply {
                                    val calendar = Calendar.getInstance().apply {
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    repeat(4) {
                                        add(calendar.time)
                                        calendar.add(Calendar.MONTH, 1)
                                    }
                                }
                            }
                            TimeFrameView(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                upcomingMonths = upcomingMonths,
                                onMonthSelected = { dateRange ->
                                    Log.i(TAG, "Selected date range: $dateRange")
                                    currentEnteringState = EnteringShifts(
                                        dateRange = dateRange
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

                                currentEnteringState = if (remainingDays.empty) {
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

                                currentEnteringState = if (remainingDays.empty) {
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
                                Log.i(TAG, "Returning to previous day from day ${enteringState.currentDate}")
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
                            Log.i(TAG, "Shifts: ${enteringState.enteredShifts}")
                            currentEnteringState = SelectingDateRange
                        }
                    }
                }
            }
        }
    }
}
