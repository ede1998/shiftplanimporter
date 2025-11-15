package me.erik_hennig.shiftplanimporter

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val TAG = "CalendarPermission"

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
)

@Composable
fun rememberCalendarPermissionLauncher(
    onPermissionGranted: () -> Unit, errorText: String
): () -> Unit {
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.i(TAG, "Permission request result: $permissions")
        if (permissions.values.all { it }) {
            onPermissionGranted()
        } else {
            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
        }
    }

    return {
        val allPermissionsGranted = CALENDAR_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            onPermissionGranted()
        } else {
            Log.d(TAG, "Requesting missing permissions")
            requestPermissionLauncher.launch(CALENDAR_PERMISSIONS)
        }
    }
}
