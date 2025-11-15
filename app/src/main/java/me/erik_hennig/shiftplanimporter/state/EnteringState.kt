package me.erik_hennig.shiftplanimporter.state

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import me.erik_hennig.shiftplanimporter.data.ShiftEvent
import me.erik_hennig.shiftplanimporter.data.ShiftTemplate
import me.erik_hennig.shiftplanimporter.extensions.withEarlierStart
import me.erik_hennig.shiftplanimporter.extensions.withLaterStart


interface ModifyShift {
    fun enter(template: ShiftTemplate?): EnteringState
    fun undo(): EnteringState
    val currentDate: LocalDate
}

sealed class EnteringState {
    object SelectingDateRange : EnteringState()
    data class EnteringShifts(
        val dateRange: LocalDateRange,
        val enteredShifts: List<ShiftEvent> = emptyList(),
    ) : EnteringState(), ModifyShift {
        override val currentDate: LocalDate
            get() = this.dateRange.start

        override fun enter(template: ShiftTemplate?): EnteringState {
            val newEvent = ShiftEvent(template, currentDate)
            val remainingDays = dateRange.withLaterStart()
            val enteredShifts = (enteredShifts + newEvent).sortedBy { it.date }

            return if (remainingDays.isEmpty()) {
                Reviewing(enteredShifts)
            } else {
                EnteringShifts(remainingDays, enteredShifts)
            }
        }

        override fun undo(): EnteringState {
            return if (enteredShifts.isEmpty()) {
                SelectingDateRange
            } else {
                val remainingDays = dateRange.withEarlierStart()
                EnteringShifts(remainingDays, enteredShifts.dropLast(1))
            }
        }
    }

    data class EditingShift(val toEdit: ShiftEvent, val enteredShifts: List<ShiftEvent>) :
        EnteringState(), ModifyShift {

        override val currentDate: LocalDate
            get() = toEdit.date

        override fun enter(template: ShiftTemplate?): Reviewing {
            val newEvent = ShiftEvent(template, currentDate)
            val enteredShifts = (enteredShifts + newEvent).sortedBy { it.date }

            return Reviewing(enteredShifts)
        }

        override fun undo(): Reviewing {
            val enteredShifts = (enteredShifts + toEdit).sortedBy { it.date }
            return Reviewing(enteredShifts)
        }
    }

    data class Reviewing(val enteredShifts: List<ShiftEvent>) : EnteringState() {
        fun editShift(index: Int): EditingShift {
            val mutable = enteredShifts.toMutableList()
            val toEdit = mutable.removeAt(index)
            return EditingShift(toEdit, mutable)
        }
    }

    val shifts: List<ShiftEvent>
        get() = when (this) {
            is EnteringShifts -> enteredShifts
            is Reviewing -> enteredShifts
            is EditingShift -> enteredShifts
            SelectingDateRange -> emptyList()
        }
}

