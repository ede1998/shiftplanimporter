package me.erik_hennig.shiftplanimporter.state

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import me.erik_hennig.shiftplanimporter.data.ShiftEvent

// TODO: Add welcome screen
// TODO: Add settings
sealed class EnteringState {
    object SelectingDateRange : EnteringState()
    data class EnteringShifts(
        val dateRange: LocalDateRange,
        val enteredShifts: List<ShiftEvent> = emptyList(),
    ) : EnteringState() {
        val currentDate: LocalDate
            get() = this.dateRange.start
    }

    data class Review(val enteredShifts: List<ShiftEvent>) : EnteringState()
}
