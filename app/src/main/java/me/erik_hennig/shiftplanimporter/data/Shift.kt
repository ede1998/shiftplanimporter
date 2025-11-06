package me.erik_hennig.shiftplanimporter.data

import kotlinx.datetime.LocalDate

// TODO: Turn this into configurable list
enum class Shift(val displayName: String) {
    MORNING("Frühdienst"),
    EVENING("Spätdienst"),
    NIGHT("Nachtdienst"),
    DAY("Sonderamt"),
}

data class ShiftEvent(val kind: Shift, val date: LocalDate)
