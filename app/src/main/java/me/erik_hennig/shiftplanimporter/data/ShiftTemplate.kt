package me.erik_hennig.shiftplanimporter.data

import kotlinx.datetime.LocalTime

data class ShiftTemplate(
    val id: String,
    val summary: String,
    val description: String,
    val times: ShiftTimes? = null,
    val calendarId: Long? = null
)

data class ShiftTimes(
    val start: LocalTime,
    val end: LocalTime,
)
