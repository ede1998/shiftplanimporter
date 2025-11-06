package me.erik_hennig.shiftplanimporter.extensions

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.todayIn
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun LocalDateRange.withAdjustedStart(offset: Int): LocalDateRange {
    if (this.isEmpty() && offset >= 0) {
        return this
    }

    val newStart = this.start.plus(offset, DateTimeUnit.DAY)
    return LocalDateRange(newStart, this.endInclusive)
}

fun LocalDateRange.withLaterStart(): LocalDateRange = this.withAdjustedStart(1)
fun LocalDateRange.withEarlierStart(): LocalDateRange = this.withAdjustedStart(-1)

fun LocalDate.format(renderFormat: java.text.DateFormat): String {
    val date = java.util.Date.from(
        this.toJavaLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
    )
    return renderFormat.format(date)
}

@OptIn(ExperimentalTime::class)
fun LocalDate.Companion.today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
