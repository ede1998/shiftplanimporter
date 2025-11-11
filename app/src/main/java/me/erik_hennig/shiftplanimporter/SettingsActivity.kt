package me.erik_hennig.shiftplanimporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.datetime.LocalTime
import me.erik_hennig.shiftplanimporter.data.CalendarInfo
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.data.ShiftTimes
import me.erik_hennig.shiftplanimporter.extensions.formatDefault
import me.erik_hennig.shiftplanimporter.ui.TemplateConfigurationView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterTheme {
                val navController = rememberNavController()
                val calendars = listOf(CalendarInfo(1, "Personal"), CalendarInfo(2, "Work"))
                NavHost(navController = navController, startDestination = "template_list") {
                    composable("template_list") {
                        TemplateListScreen(
                            onAddTemplate = { navController.navigate("template_configuration") },
                            onEditTemplate = { navController.navigate("template_configuration/$it") })
                    }
                    composable("template_configuration") {
                        TemplateConfigurationView(
                            calendars = calendars,
                            initialTemplate = null,
                            onSave = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() })
                    }
                    composable("template_configuration/{templateId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("templateId")
                        val template = DUMMY_TEMPLATES.find { it.id == id }
                        // In a real app, you would load the template from your data source here.
                        TemplateConfigurationView(
                            calendars = calendars,
                            initialTemplate = template,
                            onSave = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

val DUMMY_TEMPLATES: List<ShiftTemplate> = listOf(
    ShiftTemplate(
        id = UUID.randomUUID().toString(),
        summary = "Early Shift",
        description = "Morning shift from 9 to 5",
        times = ShiftTimes(LocalTime(9, 0), LocalTime(17, 0)),
    ), ShiftTemplate(
        id = UUID.randomUUID().toString(),
        summary = "Late Shift",
        description = "Evening shift from 5 to 1",
        times = ShiftTimes(LocalTime(17, 0), LocalTime(1, 0)),
    ), ShiftTemplate(
        id = UUID.randomUUID().toString(),
        summary = "All Day Event",
        description = "An all day event",
        times = null
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onAddTemplate: () -> Unit, onEditTemplate: (String) -> Unit
) {
    var templates by remember { mutableStateOf(DUMMY_TEMPLATES) }

    Scaffold(topBar = { TopAppBar(title = { Text("Shift Templates") }) }, floatingActionButton = {
        FloatingActionButton(onClick = onAddTemplate) {
            Icon(Icons.Default.Add, contentDescription = "Add new template")
        }
    }) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(templates) { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.summary)
                        if (template.times == null) {
                            Text("All day")
                        } else {
                            with(template.times) {
                                Text("${start.formatDefault()} - ${end.formatDefault()}")
                            }
                        }
                    }
                    IconButton(onClick = { onEditTemplate(template.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit template")
                    }
                    IconButton(onClick = {
                        templates = templates.filter { it.id != template.id }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete template")
                    }
                }
            }
        }
    }
}
