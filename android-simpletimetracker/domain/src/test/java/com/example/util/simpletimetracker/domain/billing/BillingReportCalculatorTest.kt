package com.example.util.simpletimetracker.domain.billing

import com.example.util.simpletimetracker.domain.billing.interactor.BillableResolver
import com.example.util.simpletimetracker.domain.billing.interactor.BillingReportCalculator
import com.example.util.simpletimetracker.domain.billing.model.BillableOverride
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingReport
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingReportCalculatorTest {

    private val hour = 3_600_000L
    private val calculator = BillingReportCalculator()

    private fun entry(
        activity: String = "Dev",
        projects: List<String> = listOf("Acme"),
        start: Long = 0,
        hours: Double = 1.0,
        billable: Boolean = true,
        rate: Long = 10_000,
    ) = BillingEntry(activity, projects, emptyList(), "", start, start + (hours * hour).toLong(), billable, rate)

    @Test
    fun splitsBillableAndNonBillable() {
        val report = calculator.calculate(
            listOf(entry(hours = 3.0), entry(activity = "Admin", hours = 1.0, billable = false)),
        )
        assertEquals(3 * hour, report.billableMillis)
        assertEquals(hour, report.nonBillableMillis)
        assertEquals(4 * hour, report.totalMillis)
        assertEquals(0.75, report.utilization, 1e-9)
        assertEquals(30_000L, report.amountMinor)
    }

    @Test
    fun nonBillableEarnsNothing() {
        val report = calculator.calculate(listOf(entry(billable = false, rate = 99_999)))
        assertEquals(0L, report.amountMinor)
    }

    @Test
    fun clipsRecordsToRange() {
        val report = calculator.calculate(listOf(entry(start = 0, hours = 4.0)), rangeStart = hour, rangeEnd = 3 * hour)
        assertEquals(2 * hour, report.totalMillis)
        assertEquals(20_000L, report.amountMinor)
    }

    @Test
    fun skipsRecordsOutsideRange() {
        val report = calculator.calculate(listOf(entry(start = 10 * hour)), rangeStart = 0, rangeEnd = hour)
        assertEquals(0L, report.totalMillis)
        assertEquals(0.0, report.utilization, 0.0)
    }

    @Test
    fun recordInTwoProjectsCountsOnceInTotalsAndInEachProject() {
        val report = calculator.calculate(listOf(entry(projects = listOf("A", "B"), hours = 2.0)))
        assertEquals(2 * hour, report.totalMillis)
        assertEquals(listOf("A", "B"), report.byProject.map { it.name })
        assertEquals(listOf(2 * hour, 2 * hour), report.byProject.map { it.totalMillis })
    }

    @Test
    fun recordWithoutProjectLandsInUnassigned() {
        val report = calculator.calculate(listOf(entry(projects = emptyList())))
        assertEquals(BillingReport.UNASSIGNED_PROJECT, report.byProject.single().name)
    }

    @Test
    fun amountRoundsHalfUpToMinorUnit() {
        // 1 minute at 100.01 per hour = 166.7 minor units -> 167.
        val e = entry(hours = 1.0 / 60, rate = 10_001)
        assertEquals(167L, calculator.calculate(listOf(e)).amountMinor)
    }

    @Test
    fun linesSortByTotalDescendingThenName() {
        val report = calculator.calculate(
            listOf(entry(projects = listOf("B"), hours = 1.0), entry(projects = listOf("A"), hours = 1.0), entry(projects = listOf("C"), hours = 5.0)),
        )
        assertEquals(listOf("C", "A", "B"), report.byProject.map { it.name })
    }

    @Test
    fun overrideBeatsActivityDefault() {
        assertEquals(true, BillableResolver.isBillable(false, BillableOverride.BILLABLE))
        assertEquals(false, BillableResolver.isBillable(true, BillableOverride.NON_BILLABLE))
        assertEquals(true, BillableResolver.isBillable(true, BillableOverride.INHERIT))
        assertEquals(false, BillableResolver.isBillable(false, BillableOverride.INHERIT))
    }

    @Test
    fun overrideRoundTripsThroughDbValue() {
        BillableOverride.values().forEach {
            assertEquals(it, BillableOverride.fromDb(it.dbValue))
        }
    }
}
