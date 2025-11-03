package me.erik_hennig.shiftplanimporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.erik_hennig.shiftplanimporter.ui.EnterShiftView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EnterShiftView(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        date = Date(),
                        shifts = listOf("Frühdienst", "Spätdienst", "Nachtdienst", "Sonderamt")
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    ShiftPlanImporterTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            EnterShiftView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                date = Date(),
                shifts = listOf("Frühdienst", "Spätdienst", "Nachtdienst", "Sonderamt")
            )
        }
    }
}
