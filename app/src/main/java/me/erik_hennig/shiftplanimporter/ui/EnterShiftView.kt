package me.erik_hennig.shiftplanimporter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.data.templateExamples
import me.erik_hennig.shiftplanimporter.extensions.dateAndWeekDayFormat
import me.erik_hennig.shiftplanimporter.extensions.format
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme

@Composable
fun EnterShiftView(
    modifier: Modifier = Modifier,
    date: LocalDate,
    templates: List<ShiftTemplate>,
    onShiftSelected: (ShiftTemplate) -> Unit,
    onUndo: () -> Unit,
    onSkip: () -> Unit
) {
    val currentDate = date.format(dateAndWeekDayFormat())

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = currentDate, fontSize = 24.sp)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )
        templates.forEach { shift ->
            Button(
                onClick = { onShiftSelected(shift) }, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = shift.summary)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onUndo) {
                Text(text = "Undo")
            }
            Button(onClick = onSkip) {
                Text(text = "Skip")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnterShiftViewPreview() {
    ShiftPlanImporterTheme {
        EnterShiftView(
            date = LocalDate.today(),
            templates = templateExamples,
            onShiftSelected = {},
            onUndo = {},
            onSkip = {})
    }
}
