package me.erik_hennig.shiftplanimporter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.calendar.importShiftsToCalendar
import me.erik_hennig.shiftplanimporter.data.Shift
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.extensions.withEarlierStart
import me.erik_hennig.shiftplanimporter.extensions.withLaterStart
import me.erik_hennig.shiftplanimporter.state.EnteringState
import me.erik_hennig.shiftplanimporter.state.EnteringState.EnteringShifts
import me.erik_hennig.shiftplanimporter.state.EnteringState.Review
import me.erik_hennig.shiftplanimporter.state.EnteringState.SelectingDateRange
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.ReviewView
import me.erik_hennig.shiftplanimporter.ui.TimeFrameView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme

private const val TAG = "MainActivity"

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterApp()
        }
    }
}

@Composable
private fun ShiftPlanImporterApp() {
    ShiftPlanImporterTheme {
        var currentEnteringState by remember {
            mutableStateOf<EnteringState>(
                SelectingDateRange
            )
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (val enteringState = currentEnteringState) {
                is SelectingDateRange -> {
                    TimeFrameScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStateChange = { currentEnteringState = it }
                    )
                }

                is EnteringShifts -> {
                    EnterShiftScreen(
                        modifier = Modifier.padding(innerPadding),
                        enteringState = enteringState,
                        onStateChange = { currentEnteringState = it }
                    )
                }

                is Review -> {
                    ReviewScreen(
                        modifier = Modifier.padding(innerPadding),
                        enteringState = enteringState,
                        onStateChange = { currentEnteringState = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeFrameScreen(modifier: Modifier = Modifier, onStateChange: (EnteringState) -> Unit) {
    val upcomingMonths = remember {
        val start = LocalDate.today().yearMonth
        val end = start.plus(4, DateTimeUnit.MONTH)
        start..end
    }
    TimeFrameView(
        modifier = modifier.fillMaxSize(),
        upcomingMonths = upcomingMonths,
        onTimeFrameSelected = { timeFrame ->
            Log.i(TAG, "Selected date range: $timeFrame")
            onStateChange(
                EnteringShifts(
                    dateRange = timeFrame
                )
            )
        }
    )
}

@Composable
private fun EnterShiftScreen(
    modifier: Modifier = Modifier,
    enteringState: EnteringShifts,
    onStateChange: (EnteringState) -> Unit
) {
    val onShiftSelected: (Shift) -> Unit = { shift: Shift ->
        val newEvent = ShiftEvent(kind = shift, date = enteringState.currentDate)
        val remainingDays = enteringState.dateRange.withLaterStart()
        val enteredShifts = enteringState.enteredShifts + newEvent

        val newState = if (remainingDays.isEmpty()) {
            Review(enteredShifts = enteredShifts)
        } else {
            EnteringShifts(dateRange = remainingDays, enteredShifts = enteredShifts)
        }
        onStateChange(newState)
        Log.i(TAG, "Added new shift: $newEvent")
    }

    val onSkip: () -> Unit = {
        val remainingDays = enteringState.dateRange.withLaterStart()

        val newState = if (remainingDays.isEmpty()) {
            Review(
                enteredShifts = enteringState.enteredShifts
            )
        } else {
            enteringState.copy(dateRange = remainingDays)
        }
        onStateChange(newState)
        Log.i(TAG, "Skipped day ${enteringState.currentDate}")
    }

    val onUndo: () -> Unit = {
        val newState = if (enteringState.enteredShifts.isEmpty()) {
            SelectingDateRange
        } else {
            val remainingDays = enteringState.dateRange.withEarlierStart()
            EnteringShifts(
                dateRange = remainingDays,
                enteredShifts = enteringState.enteredShifts.dropLast(1),
            )
        }
        onStateChange(newState)
        val previousDay = enteringState.currentDate.minus(1, DateTimeUnit.DAY)
        Log.i(
            TAG, "Returning to previous day $previousDay"
        )
    }

    EnterShiftView(
        modifier = modifier.fillMaxSize(),
        date = enteringState.currentDate,
        shiftOptions = Shift.entries,
        onShiftSelected = onShiftSelected,
        onUndo = onUndo,
        onSkip = onSkip
    )
}

@Composable
private fun ReviewScreen(
    modifier: Modifier = Modifier,
    enteringState: Review,
    onStateChange: (EnteringState) -> Unit,
) {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.i(TAG, "Permission request result: $permissions")
        if (permissions.values.all { it }) {
            importShiftsToCalendar(context, enteringState.enteredShifts)
            onStateChange(SelectingDateRange)
        } else {
            Toast.makeText(
                context, "Calendar permissions are required to import shifts.", Toast.LENGTH_LONG
            ).show()
        }
    }

    ReviewView(
        modifier = modifier.fillMaxSize(),
        shiftEvents = enteringState.enteredShifts,
        onEdit = {
            // TODO: Implement edit
            Log.i(TAG, "Editing shift: ${enteringState.enteredShifts[it]}")
        },
        onDiscardAll = {
            Log.i(TAG, "Discarding shift selection")
            onStateChange(SelectingDateRange)
        },
        onImportAll = {
            Log.i(TAG, "Importing shift selection")
            val allPermissionsGranted = CALENDAR_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(
                    context, it
                ) == PackageManager.PERMISSION_GRANTED
            }
            if (allPermissionsGranted) {
                importShiftsToCalendar(context, enteringState.enteredShifts)
                onStateChange(SelectingDateRange)
            } else {
                Log.d(TAG, "Requesting missing permissions")
                requestPermissionLauncher.launch(CALENDAR_PERMISSIONS)
            }
        },
        onExportAll = {
            // TODO: Implement export
            Log.i(TAG, "Exporting shift selection")
            onStateChange(SelectingDateRange)
        }
    )
}
