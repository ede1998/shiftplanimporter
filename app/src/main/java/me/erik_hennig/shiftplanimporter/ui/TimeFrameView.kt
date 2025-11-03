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
import me.erik_hennig.shiftplanimporter.DateRange
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimeFrameView(
    modifier: Modifier = Modifier,
    upcomingMonths: List<Date>,
    onMonthSelected: (DateRange) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Select Time Frame", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 2.dp,
            color = Color.Gray
        )

        upcomingMonths.forEach { date ->
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val endDate = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }.time
                    val dateRange = DateRange(start = date, end = endDate)
                    onMonthSelected(dateRange)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = monthFormat.format(date))
            }
        }

        Button(
            onClick = { /* TODO: Handle custom time frame selection */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Select Custom Range (Not implemented yet)")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeFrameViewPreview() {
    val upcomingMonths = mutableListOf<Date>()
    val calendar = Calendar.getInstance()

    for (i in 0..3) {
        upcomingMonths.add(calendar.time)
        calendar.add(Calendar.MONTH, 1)
    }
    ShiftPlanImporterTheme {
        TimeFrameView(upcomingMonths = upcomingMonths, onMonthSelected = {})
    }
}
