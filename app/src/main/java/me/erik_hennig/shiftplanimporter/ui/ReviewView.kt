package me.erik_hennig.shiftplanimporter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.datetime.plus
import me.erik_hennig.shiftplanimporter.data.Shift
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import me.erik_hennig.shiftplanimporter.extensions.format
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReviewView(
    modifier: Modifier = Modifier,
    shiftEvents: List<ShiftEvent>,
    onEdit: (Int) -> Unit,
    onDiscardAll: () -> Unit,
    onImportAll: () -> Unit,
    onExportAll: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Review Shifts", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 2.dp,
            color = Color.Gray
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(shiftEvents) { index, event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onEdit(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val weekDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val date = event.date.format(dateFormat)
                    val weekDay = event.date.format(weekDayFormat)
                    Text(text = weekDay, modifier = Modifier.weight(1f))
                    Text(text = date, modifier = Modifier.weight(1.5f))
                    Text(text = event.kind.displayName, modifier = Modifier.weight(1f))
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 2.dp,
            color = Color.Gray
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onDiscardAll) {
                Text(text = "Discard All")
            }
            Button(onClick = onImportAll) {
                Text(text = "Import All")
            }
        }
        Button(onClick = onExportAll) {
            Text(text = "Export as ICS")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewViewPreview() {
    val shiftEvents = listOf(
        ShiftEvent(Shift.MORNING, LocalDate.today()),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(1, DateTimeUnit.DAY)),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(2, DateTimeUnit.DAY)),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(3, DateTimeUnit.DAY)),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(4, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(6, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(7, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(8, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(9, DateTimeUnit.DAY)),
        ShiftEvent(Shift.MORNING, LocalDate.today().plus(10, DateTimeUnit.DAY)),
        ShiftEvent(Shift.DAY, LocalDate.today().plus(11, DateTimeUnit.DAY)),
        ShiftEvent(Shift.MORNING, LocalDate.today().plus(12, DateTimeUnit.DAY)),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(13, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(14, DateTimeUnit.DAY)),
        ShiftEvent(Shift.DAY, LocalDate.today().plus(15, DateTimeUnit.DAY)),
        ShiftEvent(Shift.MORNING, LocalDate.today().plus(16, DateTimeUnit.DAY)),
        ShiftEvent(Shift.EVENING, LocalDate.today().plus(17, DateTimeUnit.DAY)),
        ShiftEvent(Shift.NIGHT, LocalDate.today().plus(18, DateTimeUnit.DAY)),
        ShiftEvent(Shift.DAY, LocalDate.today().plus(19, DateTimeUnit.DAY)),
        ShiftEvent(Shift.MORNING, LocalDate.today().plus(20, DateTimeUnit.DAY))
    )
    ShiftPlanImporterTheme {
        ReviewView(
            shiftEvents = shiftEvents,
            onEdit = {},
            onDiscardAll = {},
            onImportAll = {},
            onExportAll = {}
        )
    }
}
