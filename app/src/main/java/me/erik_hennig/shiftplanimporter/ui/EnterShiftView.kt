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
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EnterShiftView(
    modifier: Modifier = Modifier,
    date: Date,
    shifts: List<String>
) {
    val dateFormat = SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(date)

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = currentDate, fontSize = 24.sp)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 2.dp,
            color = Color.Gray
        )
        shifts.forEach { shift ->
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = shift)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { /* TODO */ }) {
                Text(text = "Undo")
            }
            Button(onClick = { /* TODO */ }) {
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
            date = Date(),
            shifts = listOf("Frühdienst", "Spätdienst", "Nachtdienst", "Sonderamt")
        )
    }
}
