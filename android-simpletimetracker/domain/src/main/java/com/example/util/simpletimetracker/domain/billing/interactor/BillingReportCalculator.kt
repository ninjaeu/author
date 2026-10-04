package com.example.util.simpletimetracker.domain.billing.interactor

import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingReport
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Pure calculation of billable and non-billable totals for a time range.
 * Records are clipped to [rangeStart, rangeEnd]; a record in several projects
 * counts in each project line and once in the totals.
 */
class BillingReportCalculator @Inject constructor() {

    fun calculate(
        entries: List<BillingEntry>,
        rangeStart: Long = Long.MIN_VALUE,
        rangeEnd: Long = Long.MAX_VALUE,
    ): BillingReport {
        val byProject = mutableMapOf<String, Accumulator>()
        val byActivity = mutableMapOf<String, Accumulator>()
        val total = Accumulator()

        entries.forEach { entry ->
            val millis = clippedMillis(entry, rangeStart, rangeEnd)
            if (millis <= 0L) return@forEach
            val amount = amountMinor(millis, entry.hourlyRateMinor).takeIf { entry.billable } ?: 0L

            total.add(entry.billable, millis, amount)
            byActivity.getOrPut(entry.activityName) { Accumulator() }.add(entry.billable, millis, amount)
            val projects = entry.projects.ifEmpty { listOf(BillingReport.UNASSIGNED_PROJECT) }
            projects.distinct().forEach { project ->
                byProject.getOrPut(project) { Accumulator() }.add(entry.billable, millis, amount)
            }
        }

        return BillingReport(
            totalMillis = total.billableMillis + total.nonBillableMillis,
            billableMillis = total.billableMillis,
            nonBillableMillis = total.nonBillableMillis,
            amountMinor = total.amountMinor,
            byProject = byProject.toLines(),
            byActivity = byActivity.toLines(),
        )
    }

    private fun clippedMillis(entry: BillingEntry, rangeStart: Long, rangeEnd: Long): Long {
        val start = maxOf(entry.timeStarted, rangeStart)
        val end = minOf(entry.timeEnded, rangeEnd)
        return end - start
    }

    private fun amountMinor(millis: Long, hourlyRateMinor: Long): Long {
        return BigDecimal(millis)
            .multiply(BigDecimal(hourlyRateMinor))
            .divide(BigDecimal(MILLIS_IN_HOUR), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun Map<String, Accumulator>.toLines(): List<BillingReport.Line> {
        return map { (name, acc) ->
            BillingReport.Line(name, acc.billableMillis, acc.nonBillableMillis, acc.amountMinor)
        }.sortedWith(compareByDescending<BillingReport.Line> { it.totalMillis }.thenBy { it.name })
    }

    private class Accumulator {
        var billableMillis = 0L
        var nonBillableMillis = 0L
        var amountMinor = 0L

        fun add(billable: Boolean, millis: Long, amount: Long) {
            if (billable) billableMillis += millis else nonBillableMillis += millis
            amountMinor += amount
        }
    }

    companion object {
        private const val MILLIS_IN_HOUR = 3_600_000L
    }
}
