package me.erik_hennig.shiftplanimporter.extensions

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun LocalTime.Companion.now(): LocalTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

fun LocalTime.formatDefault(): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return this.format(formatter)
}

fun LocalTime.format(renderFormat: java.time.format.DateTimeFormatter): String {
    return renderFormat.format(this.toJavaLocalTime())
}

fun LocalDateRange.withAdjustedStart(offset: Int): LocalDateRange {
    if (this.isEmpty() && offset >= 0) {
        return this
    }

    val newStart = this.start.plus(offset, DateTimeUnit.DAY)
    return LocalDateRange(newStart, this.endInclusive)
}

fun LocalDateRange.withLaterStart(): LocalDateRange = this.withAdjustedStart(1)
fun LocalDateRange.withEarlierStart(): LocalDateRange = this.withAdjustedStart(-1)


fun dateOnlyFormat() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
fun weekDayOnlyFormat() = SimpleDateFormat("EEEE", Locale.getDefault())
fun dateAndWeekDayFormat() = SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault())
fun monthOnlyFormat() = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

fun LocalDate.format(renderFormat: java.text.DateFormat): String {
    val date = java.util.Date.from(
        this.toJavaLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
    )
    return renderFormat.format(date)
}

@OptIn(ExperimentalTime::class)
fun LocalDate.Companion.today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
