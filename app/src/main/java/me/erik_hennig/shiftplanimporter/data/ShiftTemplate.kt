package me.erik_hennig.shiftplanimporter.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class ShiftTemplate(
    val id: String,
    val summary: String,
    val description: String = "",
    val times: ShiftTimes? = null,
    val calendarId: Long? = null
)

@Serializable
data class ShiftTimes(
    val start: LocalTime,
    val end: LocalTime,
)

data class ShiftEvent(val template: ShiftTemplate, val date: LocalDate)


val templateExamples = listOf(
    ShiftTemplate(
        id = "1",
        summary = "Morning shift",
        times = ShiftTimes(start = LocalTime(5, 30), end = LocalTime(15, 0))
    ), ShiftTemplate(
        id = "2",
        summary = "Evening shift",
        times = ShiftTimes(start = LocalTime(14, 30), end = LocalTime(22, 0))
    ), ShiftTemplate(
        id = "3",
        summary = "Night shift",
        times = ShiftTimes(start = LocalTime(21, 30), end = LocalTime(6, 0))
    ), ShiftTemplate(
        id = "4",
        summary = "Day shift",
        times = ShiftTimes(start = LocalTime(8, 30), end = LocalTime(17, 0))
    )
)