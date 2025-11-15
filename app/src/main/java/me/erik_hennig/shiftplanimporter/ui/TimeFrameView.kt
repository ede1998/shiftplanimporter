package me.erik_hennig.shiftplanimporter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.YearMonthProgression
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import me.erik_hennig.shiftplanimporter.extensions.format
import me.erik_hennig.shiftplanimporter.extensions.monthOnlyFormat
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme

@Composable
fun TimeFrameView(
    modifier: Modifier = Modifier,
    upcomingMonths: YearMonthProgression,
    timeFrameSelectionEnabled: Boolean = true,
    onTimeFrameSelected: (LocalDateRange) -> Unit,
    onConfigureTemplates: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Select Time Frame", style = MaterialTheme.typography.headlineMedium)
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
            onClick = { /* TODO: Handle custom time frame selection */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Select Custom Range (Not implemented yet)")
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )

        Button(
            onClick = onConfigureTemplates, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Configure Templates")
        }
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
