package me.erik_hennig.shiftplanimporter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.erik_hennig.shiftplanimporter.calendar.CalendarInfo
import me.erik_hennig.shiftplanimporter.calendar.getCalendars
import me.erik_hennig.shiftplanimporter.data.SettingsRepository
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.extensions.formatDefault
import me.erik_hennig.shiftplanimporter.ui.TemplateConfigurationView
import me.erik_hennig.shiftplanimporter.ui.theme.ShiftPlanImporterTheme

private const val TAG = "SettingsActivity"

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftPlanImporterTheme {
                val context = LocalContext.current
                val settings = remember { SettingsRepository(context) }
                var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
                var missingPermission by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        calendars = getCalendars(context)
                        missingPermission = false
                    } catch (_: SecurityException) {
                        Log.i(TAG, "Missing permission to read calendars")
                        missingPermission = true
                    }
                }

                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val templates by settings.templates.collectAsState(
                    emptyList(), coroutineScope.coroutineContext
                )

                NavHost(
                    navController = navController,
                    startDestination = if (missingPermission) "request_permission" else "template_list"
                ) {
                    composable("request_permission") {
                        RequestCalendarPermissionScreen(onFinish = {
                            calendars = getCalendars(context)
                            navController.navigate("template_list")
                        })
                    }
                    composable("template_list") {
                        TemplateListScreen(
                            templates = templates,
                            onAdd = { navController.navigate("template_configuration") },
                            onEdit = { navController.navigate("template_configuration/$it") },
                            onDelete = { coroutineScope.launch { settings.removeTemplate(it) } },
                        )
                    }
                    composable("template_configuration") {
                        TemplateConfigurationView(
                            calendars = calendars,
                            initialTemplate = null,
                            onSave = {
                                coroutineScope.launch { settings.addOrUpdateTemplate(it) }
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() })
                    }
                    composable("template_configuration/{templateId}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("templateId")
                        TemplateConfigurationView(
                            calendars = calendars,
                            initialTemplate = templates.find { it.id == id },
                            onSave = {
                                coroutineScope.launch { settings.addOrUpdateTemplate(it) }
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestCalendarPermissionScreen(onFinish: () -> Unit) {
    val requestPermission = rememberCalendarPermissionLauncher(
        onFinish, stringResource(R.string.calendar_permissions_required_template)
    )
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.permission_request)) }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = requestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(text = stringResource(R.string.request_calendar_permissions))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    templates: List<ShiftTemplate>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (ShiftTemplate) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.shift_templates)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_new_template)
                )
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
                            Text(stringResource(R.string.all_day))
                        } else {
                            with(template.times) {
                                Text("${start.formatDefault()} - ${end.formatDefault()}")
                            }
                        }
                    }
                    IconButton(onClick = { onEdit(template.id) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_template)
                        )
                    }
                    IconButton(onClick = { onDelete(template) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_template)
                        )
                    }
                }
            }
        }
    }
}
