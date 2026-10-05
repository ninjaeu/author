package com.example.util.simpletimetracker.domain.billing

import com.example.util.simpletimetracker.domain.billing.interactor.BillingReportCalculator
import com.example.util.simpletimetracker.domain.billing.interactor.BillingTypeResolver
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingType
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingReportCalculatorTest {

    private val hour = 3_600_000L
    private val calculator = BillingReportCalculator()

    private fun entry(
        activity: String = "Development",
        project: String = "Business",
        start: Long = 0,
        hours: Double = 1.0,
        type: BillingType = BillingType.CLIENT_BILLABLE,
        rate: Long = 10_000,
    ) = BillingEntry(activity, project, "", "", "", start, start + (hours * hour).toLong(), type, rate)

    @Test
    fun splitsTimeByType() {
        val report = calculator.calculate(
            listOf(
                entry(hours = 2.0),
                entry(activity = "RA", project = "RA", hours = 5.0, type = BillingType.PAID, rate = 0),
                entry(activity = "Thesis", project = "Degree", hours = 3.0, type = BillingType.NON_BILLABLE),
            ),
        )
        assertEquals(2 * hour, report.total.clientMillis)
        assertEquals(5 * hour, report.total.paidMillis)
        assertEquals(3 * hour, report.total.nonBillableMillis)
        assertEquals(10 * hour, report.total.totalMillis)
        assertEquals(0.7, report.total.earningShare, 1e-9)
        assertEquals(20_000L, report.total.rateAmountMinor)
    }

    @Test
    fun nonBillableEarnsNothingEvenWithARate() {
        val report = calculator.calculate(listOf(entry(type = BillingType.NON_BILLABLE, rate = 99_999)))
        assertEquals(0L, report.total.rateAmountMinor)
    }

    @Test
    fun unpaidPerClientHour() {
        val report = calculator.calculate(
            listOf(entry(hours = 1.0), entry(project = "Business", type = BillingType.NON_BILLABLE, hours = 4.0)),
        )
        assertEquals(4.0, report.total.unpaidPerClientHour!!, 1e-9)
    }

    @Test
    fun unpaidPerClientHourIsNullWithoutClientTime() {
        val report = calculator.calculate(listOf(entry(type = BillingType.NON_BILLABLE)))
        assertNull(report.total.unpaidPerClientHour)
    }

    @Test
    fun clipsRecordsToRange() {
        val report = calculator.calculate(listOf(entry(start = 0, hours = 4.0)), rangeStart = hour, rangeEnd = 3 * hour)
        assertEquals(2 * hour, report.total.totalMillis)
        assertEquals(20_000L, report.total.rateAmountMinor)
    }

    @Test
    fun skipsRecordsOutsideRange() {
        val report = calculator.calculate(listOf(entry(start = 10 * hour)), rangeStart = 0, rangeEnd = hour)
        assertEquals(0L, report.total.totalMillis)
        assertEquals(0.0, report.total.earningShare, 0.0)
    }

    @Test
    fun amountRoundsHalfUpToMinorUnit() {
        // 1 minute at 100.01 per hour = 166.7 minor units -> 167.
        val e = entry(hours = 1.0 / 60, rate = 10_001)
        assertEquals(167L, calculator.calculate(listOf(e)).total.rateAmountMinor)
    }

    @Test
    fun incomeCountsOnceInRangeAndFeedsReturnPerHour() {
        val stipend = IncomeEntry(project = "GCA", kind = IncomeEntry.Kind.STIPEND, amountMinor = 200_000, date = 5 * hour)
        val outside = IncomeEntry(project = "GCA", kind = IncomeEntry.Kind.STIPEND, amountMinor = 999, date = 500 * hour)
        val report = calculator.calculate(
            listOf(entry(activity = "GCA", project = "GCA", hours = 100.0, type = BillingType.PAID, rate = 0)),
            income = listOf(stipend, outside),
            rangeStart = 0,
            rangeEnd = 200 * hour,
        )
        val gca = report.byProject.single { it.name == "GCA" }
        assertEquals(200_000L, report.incomeMinor)
        assertEquals(200_000L, gca.incomeMinor)
        assertEquals(2_000L, gca.returnPerHourMinor) // $20.00 per tracked hour
    }

    @Test
    fun incomeOnlyProjectHasNoReturnPerHour() {
        val grant = IncomeEntry(project = "Conferences", kind = IncomeEntry.Kind.GRANT, amountMinor = 50_000, date = 0)
        val line = calculator.calculate(emptyList(), income = listOf(grant)).byProject.single()
        assertEquals(50_000L, line.incomeMinor)
        assertNull(line.returnPerHourMinor)
    }

    @Test
    fun linesSortByTotalDescendingThenName() {
        val report = calculator.calculate(
            listOf(entry(project = "B"), entry(project = "A"), entry(project = "C", hours = 5.0)),
        )
        assertEquals(listOf("C", "A", "B"), report.byProject.map { it.name })
    }

    @Test
    fun recordOverrideBeatsActivityDefault() {
        assertEquals(BillingType.PAID, BillingTypeResolver.resolve(BillingType.NON_BILLABLE, BillingType.PAID))
        assertEquals(BillingType.NON_BILLABLE, BillingTypeResolver.resolve(BillingType.PAID, BillingType.NON_BILLABLE))
        assertEquals(BillingType.CLIENT_BILLABLE, BillingTypeResolver.resolve(BillingType.CLIENT_BILLABLE, null))
    }

    @Test
    fun typesRoundTripThroughDbAndLabel() {
        BillingType.values().forEach {
            assertEquals(it, BillingType.fromDb(it.dbValue))
            assertEquals(it, BillingType.fromLabel(it.label))
        }
        assertNull(BillingType.fromDb(null))
        assertEquals(BillingType.NON_BILLABLE, BillingType.fromDbOrDefault(99))
    }
}
