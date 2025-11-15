package me.erik_hennig.shiftplanimporter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.datetime.plus
import me.erik_hennig.shiftplanimporter.R
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import me.erik_hennig.shiftplanimporter.data.templateExamples
import me.erik_hennig.shiftplanimporter.extensions.dateOnlyFormat
import me.erik_hennig.shiftplanimporter.extensions.format
import me.erik_hennig.shiftplanimporter.extensions.today
import me.erik_hennig.shiftplanimporter.extensions.weekDayOnlyFormat
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme


@Composable
@Suppress("AssignedValueIsNeverRead") // reason: False positive
fun ReviewView(
    modifier: Modifier = Modifier,
    shiftEvents: List<ShiftEvent>,
    onEdit: (Int) -> Unit,
    onDiscardAll: () -> Unit,
    onImportAll: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_all_question)) },
            text = { Text(stringResource(R.string.discard_all_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscardAll()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            })
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.review_shifts),
            style = MaterialTheme.typography.headlineMedium
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )
        LazyColumn(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(shiftEvents) { index, event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = with(MaterialTheme.colorScheme) {
                                if (event.template == null) surface else surfaceVariant
                            }, shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onEdit(index) }
                        .padding(
                            horizontal = 16.dp, vertical = 12.dp
                        ), verticalAlignment = Alignment.CenterVertically) {

                    val date = event.date.format(dateOnlyFormat())
                    val weekDay = event.date.format(weekDayOnlyFormat())
                    Text(text = weekDay, modifier = Modifier.weight(1f))
                    Text(text = date, modifier = Modifier.weight(1.5f))
                    Text(
                        text = event.template?.summary ?: "—", modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Gray
        )
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { showDiscardDialog = true }) {
                Text(text = stringResource(R.string.discard_all))
            }
            Button(onClick = onImportAll) {
                Text(text = stringResource(R.string.import_all))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewViewPreview() {
    val shiftEvents = listOf(
        ShiftEvent(templateExamples[0], LocalDate.today()),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(1, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(2, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(3, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(4, DateTimeUnit.DAY)),
        ShiftEvent(null, LocalDate.today().plus(5, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(6, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(7, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(8, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(9, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[0], LocalDate.today().plus(10, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[3], LocalDate.today().plus(11, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[0], LocalDate.today().plus(12, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(13, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(14, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[3], LocalDate.today().plus(15, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[0], LocalDate.today().plus(16, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[1], LocalDate.today().plus(17, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[2], LocalDate.today().plus(18, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[3], LocalDate.today().plus(19, DateTimeUnit.DAY)),
        ShiftEvent(templateExamples[0], LocalDate.today().plus(20, DateTimeUnit.DAY))
    )
    ShiftPlanImporterTheme {
        ReviewView(shiftEvents = shiftEvents, onEdit = {}, onDiscardAll = {}, onImportAll = {})
    }
}
