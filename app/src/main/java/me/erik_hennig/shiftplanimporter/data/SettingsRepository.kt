package me.erik_hennig.shiftplanimporter.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream


@Serializable
data class Settings(
    val shiftTemplates: List<ShiftTemplate>,
)

object SettingsSerializer : Serializer<Settings> {

    override val defaultValue: Settings = Settings(emptyList())

    override suspend fun readFrom(input: InputStream): Settings = try {
        Json.decodeFromString<Settings>(
            input.readBytes().decodeToString()
        )
    } catch (serialization: SerializationException) {
        throw CorruptionException("Unable to read settings", serialization)
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        output.write(
            Json.encodeToString(t).encodeToByteArray()
        )
    }
}

private val Context.dataStore: DataStore<Settings> by dataStore(
    fileName = "settings.json", serializer = SettingsSerializer
)

private const val TAG: String = "SettingsRepository"

class SettingsRepository(private val context: Context) {

    val templates: Flow<List<ShiftTemplate>> = context.dataStore.data.map { settings ->
        settings.shiftTemplates
    }

    suspend fun removeTemplate(template: ShiftTemplate) {
        Log.i(TAG, "Removing template: $template")
        context.dataStore.updateData { settings ->
            val index = settings.shiftTemplates.indexOfFirst { it.id == template.id }
            val updated = settings.shiftTemplates.run {
                if (index == -1) this
                else slice(0 until index) + slice(index + 1 until size)
            }
            settings.copy(shiftTemplates = updated)
        }
    }

    suspend fun addOrUpdateTemplate(template: ShiftTemplate) {
        Log.i(TAG, "Saving template: $template")
        context.dataStore.updateData { settings ->
            val index = settings.shiftTemplates.indexOfFirst { it.id == template.id }
            val updated = settings.shiftTemplates.run {
                if (index == -1) this + template
                else slice(0 until index) + template + slice(index + 1 until size)
            }
            settings.copy(shiftTemplates = updated)
        }
    }
}

