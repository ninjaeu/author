package com.example.util.simpletimetracker.domain.scheduledReminder.interactor

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.extension.isValidTimeOfDay
import com.example.util.simpletimetracker.domain.extension.toDomainDayOfWeek
import com.example.util.simpletimetracker.domain.extension.toLocalDateTime
import com.example.util.simpletimetracker.domain.utils.LocalDateMapper
import java.time.LocalDate
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetDoNotDisturbHandledScheduleInteractor @Inject constructor(
    private val localDateMapper: LocalDateMapper,
) {

    fun execute(
        reminderDurationSeconds: Long,
        dndStart: Long,
        dndEnd: Long,
        activeDaysOfWeek: Set<DayOfWeek>,
        nowTimestamp: Long,
    ): Long? {
        if (reminderDurationSeconds <= 0L) return null

        return execute(
            timestamp = reminderDurationSeconds * 1000L + nowTimestamp,
            dndStart = dndStart,
            dndEnd = dndEnd,
            activeDaysOfWeek = activeDaysOfWeek,
            timeZone = TimeZone.getDefault(),
        )
    }

    fun execute(
        timestamp: Long,
        dndStart: Long,
        dndEnd: Long,
        activeDaysOfWeek: Set<DayOfWeek>,
        timeZone: TimeZone,
    ): Long? {
        if (activeDaysOfWeek.isEmpty()) return null
        if (!dndStart.isValidTimeOfDay() || !dndEnd.isValidTimeOfDay()) return null

        val dndHandledTimestamp = applyDoNotDisturb(timestamp, dndStart, dndEnd, timeZone)
            ?: return null
        val dndHandledDate = dndHandledTimestamp.toLocalDateTime(timeZone).toLocalDate()
        val candidateDayOfWeek = dndHandledDate.getDomainDayOfWeek()

        if (candidateDayOfWeek in activeDaysOfWeek) return dndHandledTimestamp

        var nextSelectedDay = dndHandledDate
        repeat(7) {
            nextSelectedDay = nextSelectedDay.plusDays(1)
            if (nextSelectedDay.getDomainDayOfWeek() in activeDaysOfWeek) {
                val startOfDay = localDateMapper.resolveDateTime(
                    date = nextSelectedDay,
                    timeOfDayMillis = 0L,
                    timeZone = timeZone,
                ) ?: return null
                return applyDoNotDisturb(startOfDay, dndStart, dndEnd, timeZone)
            }
        }

        return null
    }

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    fun applyDoNotDisturb(
        timestamp: Long,
        dndStart: Long,
        dndEnd: Long,
        timeZone: TimeZone,
    ): Long? {
        if (dndStart == dndEnd) return timestamp
        val dateTime = timestamp.toLocalDateTime(timeZone)
        val timeOfDay = TimeUnit.NANOSECONDS.toMillis(dateTime.toLocalTime().toNanoOfDay())

        val endDate = when {
            // If ex. dnd is between 01:00 and 09:00 on the current day - set to 09:00
            dndStart < dndEnd && timeOfDay in dndStart until dndEnd -> dateTime.toLocalDate()
            // If ex. dnd is between 22:00 and 06:00:
            // Between 00:00 and 06:00 - set to 06:00.
            dndStart > dndEnd && timeOfDay < dndEnd -> dateTime.toLocalDate()
            // Between 22:00 and 24:00 - set to 06:00 next day.
            dndStart > dndEnd && timeOfDay >= dndStart -> dateTime.toLocalDate().plusDays(1)
            else -> return timestamp
        }

        return localDateMapper.resolveDateTime(
            date = endDate,
            timeOfDayMillis = dndEnd,
            timeZone = timeZone,
        )
    }

    private fun LocalDate.getDomainDayOfWeek(): DayOfWeek {
        return this.dayOfWeek.toDomainDayOfWeek()
    }
}