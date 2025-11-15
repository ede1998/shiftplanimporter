package me.erik_hennig.shiftplanimporter.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import me.erik_hennig.shiftplanimporter.calendar.CalendarInfo
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.data.ShiftTimes
import me.erik_hennig.shiftplanimporter.extensions.formatDefault
import me.erik_hennig.shiftplanimporter.extensions.now
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.util.UUID

const val TAG: String = "TemplateConfigurationView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateConfigurationView(
    modifier: Modifier = Modifier,
    initialTemplate: ShiftTemplate?,
    calendars: List<CalendarInfo>,
    onSave: (ShiftTemplate) -> Unit,
    onCancel: () -> Unit
) {
    var isAllDay by remember {
        mutableStateOf(initialTemplate.let { (it != null) && (it.times == null) })
    }
    var summary by remember { mutableStateOf(initialTemplate?.summary ?: "") }
    var description by remember { mutableStateOf(initialTemplate?.description ?: "") }
    var startTime by remember { mutableStateOf(initialTemplate?.times?.start) }
    var endTime by remember { mutableStateOf(initialTemplate?.times?.end) }
    var selectedCalendar by remember { mutableStateOf(calendars.find { it.id == initialTemplate?.calendarId }) }

    var isCalendarDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Summary") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isAllDay, onCheckedChange = { isAllDay = it })
            Text(text = "All day")
            Spacer(modifier = Modifier.width(16.dp))
            Row(modifier = Modifier.weight(1f)) {
                TimeInput(
                    label = "Start",
                    enabled = !isAllDay,
                    initialTime = startTime,
                    onSave = { startTime = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TimeInput(
                    label = "End",
                    enabled = !isAllDay,
                    initialTime = endTime,
                    onSave = { endTime = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = isCalendarDropdownExpanded,
            onExpandedChange = { isCalendarDropdownExpanded = !isCalendarDropdownExpanded }) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedCalendar?.name ?: "",
                onValueChange = {},
                label = { Text("Calendar") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCalendarDropdownExpanded) },
            )
            ExposedDropdownMenu(
                expanded = isCalendarDropdownExpanded,
                onDismissRequest = { isCalendarDropdownExpanded = false }) {
                calendars.forEach { calendar ->
                    DropdownMenuItem(text = { Text(calendar.name) }, onClick = {
                        selectedCalendar = calendar
                        isCalendarDropdownExpanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
        Row {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                if (summary == "") {
                    Log.e(TAG, "Summary cannot be empty")
                    return@Button
                }

                if (!isAllDay && (startTime == null || endTime == null)) {
                    Log.e(TAG, "Non-all day event must have start and end time")
                    return@Button
                }

                val calendar = selectedCalendar
                if (calendar == null) {
                    Log.e(TAG, "Calendar must be selected")
                    return@Button
                }

                val shiftTemplate = ShiftTemplate(
                    id = initialTemplate?.id ?: UUID.randomUUID().toString(),
                    summary = summary,
                    description = description,
                    times = if (isAllDay) null else ShiftTimes(startTime!!, endTime!!),
                    calendarId = calendar.id,
                )

                onSave(shiftTemplate)
            }) {
                Text("Save")
            }
        }
    }
}


@Composable
private fun TimeInput(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    initialTime: LocalTime?,
    onSave: (LocalTime) -> Unit
) {
    val context = LocalContext.current

    val startTimeSource = remember {
        MutableInteractionSource()
    }

    val initialDialogTime = initialTime ?: LocalTime.now()

    val startTimePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            onSave(LocalTime(hour, minute))
        },
        initialDialogTime.hour,
        initialDialogTime.minute,
        DateFormat.is24HourFormat(context),
    )

    if (startTimeSource.collectIsPressedAsState().value) {
        startTimePickerDialog.show()
    }

    OutlinedTextField(
        value = initialTime?.formatDefault() ?: "",
        onValueChange = { },
        label = { Text(label) },
        enabled = enabled,
        readOnly = true,
        interactionSource = startTimeSource,
        modifier = modifier
    )
}

@Preview
@Composable
private fun TemplateConfigurationViewPreview() {
    ShiftPlanImporterTheme {
        TemplateConfigurationView(
            calendars = listOf(
                CalendarInfo(1, "Personal", isPrimary = false),
                CalendarInfo(2, "Work", isPrimary = false)
            ),
            onSave = { }, onCancel = { },
            initialTemplate = null,
        )
    }
}
