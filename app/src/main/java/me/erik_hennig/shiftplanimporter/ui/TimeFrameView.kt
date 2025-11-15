package me.erik_hennig.shiftplanimporter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonthProgression
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.R
import me.erik_hennig.shiftplanimporter.extensions.format
import me.erik_hennig.shiftplanimporter.extensions.monthOnlyFormat
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("AssignedValueIsNeverRead") // reason: False positive
@Composable
fun TimeFrameView(
    modifier: Modifier = Modifier,
    upcomingMonths: YearMonthProgression,
    timeFrameSelectionEnabled: Boolean = true,
    onTimeFrameSelected: (LocalDateRange) -> Unit,
    onConfigureTemplates: () -> Unit
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    if (showDateRangePicker) {
        CustomDateRangePicker(
            onDismiss = { showDateRangePicker = false }, onConfirm = onTimeFrameSelected
        )
    }
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.select_time_frame),
            style = MaterialTheme.typography.headlineMedium
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )

        upcomingMonths.forEach { month ->
            Button(
                onClick = { onTimeFrameSelected(month.firstDay..month.lastDay) },
                enabled = timeFrameSelectionEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = month.firstDay.format(monthOnlyFormat()))
            }
        }

        Button(
            onClick = { showDateRangePicker = true },
            enabled = timeFrameSelectionEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.select_custom_range))
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )

        Button(
            onClick = onConfigureTemplates, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.configure_templates))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun CustomDateRangePicker(onDismiss: () -> Unit, onConfirm: (LocalDateRange) -> Unit) {
    val dateRangePickerState = rememberDateRangePickerState()
    val okEnabled =
        dateRangePickerState.run { selectedEndDateMillis != null && selectedStartDateMillis != null }
    val onClickOk = {
        fun msToDate(ms: Long) =
            Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date

        val start = dateRangePickerState.selectedStartDateMillis?.let { msToDate(it) }
        val end = dateRangePickerState.selectedEndDateMillis?.let { msToDate(it) }
        if (start != null && end != null) {
            onConfirm(start..end)
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onClickOk,
                enabled = okEnabled
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DateRangePicker(
            state = dateRangePickerState
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimeFrameViewPreview() {
    val start = LocalDate.today().yearMonth
    val end = start.plus(4, DateTimeUnit.MONTH)
    val upcomingMonths = start..end

    ShiftPlanImporterTheme {
        TimeFrameView(
            upcomingMonths = upcomingMonths,
            onTimeFrameSelected = {},
            onConfigureTemplates = {})
    }
}
