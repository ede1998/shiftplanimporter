package me.erik_hennig.shiftplanimporter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.calendar.importShiftsToCalendar
import me.erik_hennig.shiftplanimporter.data.SettingsRepository
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.state.EnteringState
import me.erik_hennig.shiftplanimporter.state.EnteringState.EditingShift
import me.erik_hennig.shiftplanimporter.state.EnteringState.EnteringShifts
import me.erik_hennig.shiftplanimporter.state.EnteringState.Reviewing
import me.erik_hennig.shiftplanimporter.state.EnteringState.SelectingDateRange
import me.erik_hennig.shiftplanimporter.state.ModifyShift
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.ReviewView
import me.erik_hennig.shiftplanimporter.ui.TimeFrameView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme

private const val TAG = "MainActivity"

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
        val context = LocalContext.current
        val settings = remember { SettingsRepository(context) }
        val coroutineScope = rememberCoroutineScope()
        var currentEnteringState by remember { mutableStateOf<EnteringState>(SelectingDateRange) }

        val templates by settings.templates.collectAsState(
            emptyList(), coroutineScope.coroutineContext
        )

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (val enteringState = currentEnteringState) {
                is SelectingDateRange -> {
                    // TODO: disable all buttons if no templates available
                    TimeFrameScreen(
                        modifier = Modifier.padding(innerPadding), onStateChange = {
                            @Suppress("AssignedValueIsNeverRead") // reason: False positive
                            currentEnteringState = it
                        })
                }

                is EnteringShifts, is EditingShift -> {
                    ModifyShiftScreen(
                        modifier = Modifier.padding(innerPadding),
                        enteringState = enteringState,
                        templates = templates,
                        onStateChange = {
                            @Suppress("AssignedValueIsNeverRead") // reason: False positive
                            currentEnteringState = it
                        })
                }

                is Reviewing -> {
                    ReviewScreen(
                        modifier = Modifier.padding(innerPadding),
                        enteringState = enteringState,
                        onStateChange = {
                            @Suppress("AssignedValueIsNeverRead") // reason: False positive
                            currentEnteringState = it
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeFrameScreen(modifier: Modifier = Modifier, onStateChange: (EnteringState) -> Unit) {
    val context = LocalContext.current
    val upcomingMonths = remember {
        val start = LocalDate.today().yearMonth
        val end = start.plus(4, DateTimeUnit.MONTH)
        start..end
    }
    TimeFrameView(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        upcomingMonths = upcomingMonths,
        onTimeFrameSelected = { timeFrame ->
            Log.i(TAG, "Selected date range: $timeFrame")
            onStateChange(EnteringShifts(timeFrame))
        },
        onConfigureTemplates = {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        })
}

@Composable
private fun ModifyShiftScreen(
    modifier: Modifier = Modifier,
    enteringState: ModifyShift,
    templates: List<ShiftTemplate>,
    onStateChange: (EnteringState) -> Unit
) {
    val onShiftSelected: (ShiftTemplate) -> Unit = {
        val newState = enteringState.enter(it)
        onStateChange(newState)
        Log.i(TAG, "Added new shift: ${newState.shifts.last()}")
    }

    val onSkip: () -> Unit = {
        onStateChange(enteringState.enter(null))
        Log.i(TAG, "Skipped day ${enteringState.currentDate}")
    }

    val onUndo: () -> Unit = {
        val newState = enteringState.undo()
        onStateChange(newState)
        if (newState is EnteringShifts) {
            val previousDay = newState.currentDate
            Log.i(TAG, "Returning to previous day $previousDay")
        } else {
            Log.i(TAG, "Returning to previous screen")
        }
    }

    EnterShiftView(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        date = enteringState.currentDate,
        templates = templates,
        onShiftSelected = onShiftSelected,
        onUndo = onUndo,
        onSkip = onSkip
    )
}

@Composable
private fun ReviewScreen(
    modifier: Modifier = Modifier,
    enteringState: Reviewing,
    onStateChange: (EnteringState) -> Unit,
) {
    val context = LocalContext.current

    val launchCalendarImport = rememberCalendarPermissionLauncher({
        importShiftsToCalendar(context, enteringState.enteredShifts)
        onStateChange(SelectingDateRange)
    }, "Calendar permissions are required to import shifts.")

    ReviewView(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        shiftEvents = enteringState.enteredShifts,
        onEdit = {
            Log.i(TAG, "Editing shift: ${enteringState.enteredShifts[it]}")
            onStateChange(enteringState.editShift(it))
        },
        onDiscardAll = {
            Log.i(TAG, "Discarding shift selection")
            // TODO: warn
            onStateChange(SelectingDateRange)
        },
        onImportAll = {
            Log.i(TAG, "Importing shift selection")
            launchCalendarImport()
        },
        onExportAll = {
            // TODO: Implement export
            Log.i(TAG, "Exporting shift selection")
            onStateChange(SelectingDateRange)
        })
}
