package me.erik_hennig.shiftplanimporter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.EnteringState.*
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.TimeFrameView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.toJavaLocalDate
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
    val date = java.util.Date.from(this.toJavaLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
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
// TODO: Export events to calendar
sealed class EnteringState {
    object SelectingDateRange : EnteringState()
    data class EnteringShifts(
        val dateRange: LocalDateRange,
        val enteredShifts: List<ShiftEvent> = emptyList(),
    ) : EnteringState() {
        val currentDate: LocalDate
            get() = this.dateRange.start
    }

    // TODO: make review screen
    data class Review(val enteredShifts: List<ShiftEvent>) : EnteringState()
}

@OptIn(ExperimentalTime::class)
fun LocalDate.Companion.today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
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
                            Log.i(TAG, "Shifts: ${enteringState.enteredShifts}")
                            currentEnteringState = SelectingDateRange
                        }
                    }
                }
            }
        }
    }
}
