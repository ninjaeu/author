package com.example.util.simpletimetracker.domain.billing.interactor

import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingReport
import com.example.util.simpletimetracker.domain.billing.model.BillingReport.Companion.MILLIS_IN_HOUR
import com.example.util.simpletimetracker.domain.billing.model.BillingType
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Pure calculation of paid, client-billable and non-billable time for a range.
 * Records are clipped to [rangeStart, rangeEnd). Income entries count in full
 * when their date falls inside the range.
 */
class BillingReportCalculator @Inject constructor() {

    fun calculate(
        entries: List<BillingEntry>,
        income: List<IncomeEntry> = emptyList(),
        rangeStart: Long = Long.MIN_VALUE,
        rangeEnd: Long = Long.MAX_VALUE,
    ): BillingReport {
        val total = Acc()
        val byProject = mutableMapOf<String, Acc>()
        val byActivity = mutableMapOf<String, Acc>()

        entries.forEach { e ->
            val millis = minOf(e.timeEnded, rangeEnd) - maxOf(e.timeStarted, rangeStart)
            if (millis <= 0L) return@forEach
            val amount = if (e.type == BillingType.NON_BILLABLE) 0L else amountMinor(millis, e.hourlyRateMinor)
            total.add(e.type, millis, amount)
            byProject.getOrPut(e.project) { Acc() }.add(e.type, millis, amount)
            byActivity.getOrPut(e.activityName) { Acc() }.add(e.type, millis, amount)
        }

        var incomeTotal = 0L
        income.filter { it.date >= rangeStart && it.date < rangeEnd }.forEach { i ->
            incomeTotal += i.amountMinor
            byProject.getOrPut(i.project) { Acc() }.incomeMinor += i.amountMinor
        }

        return BillingReport(
            total = total.split(),
            byProject = byProject.toLines(),
            byActivity = byActivity.toLines(),
            incomeMinor = incomeTotal,
        )
    }

    private fun amountMinor(millis: Long, hourlyRateMinor: Long): Long {
        return BigDecimal(millis)
            .multiply(BigDecimal(hourlyRateMinor))
            .divide(BigDecimal(MILLIS_IN_HOUR), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun Map<String, Acc>.toLines(): List<BillingReport.Line> {
        return map { (name, acc) -> BillingReport.Line(name, acc.split(), acc.incomeMinor) }
            .sortedWith(
                compareByDescending<BillingReport.Line> { it.split.totalMillis }
                    .thenByDescending { it.incomeMinor }
                    .thenBy { it.name },
            )
    }

    private class Acc {
        var paid = 0L
        var client = 0L
        var non = 0L
        var amount = 0L
        var incomeMinor = 0L

        fun add(type: BillingType, millis: Long, amountMinor: Long) {
            when (type) {
                BillingType.PAID -> paid += millis
                BillingType.CLIENT_BILLABLE -> client += millis
                BillingType.NON_BILLABLE -> non += millis
            }
            amount += amountMinor
        }

        fun split() = BillingReport.Split(paid, client, non, amount)
    }
}
