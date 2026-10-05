package com.example.util.simpletimetracker.domain.billing.interactor

import com.example.util.simpletimetracker.domain.billing.model.BillingReport
import com.example.util.simpletimetracker.domain.billing.repo.BillingDataRepo
import com.example.util.simpletimetracker.domain.record.model.Range
import javax.inject.Inject

class BillingReportInteractor @Inject constructor(
    private val billingDataRepo: BillingDataRepo,
    private val calculator: BillingReportCalculator,
) {

    /** [range] null covers all time. */
    suspend fun get(range: Range?): BillingReport {
        return calculator.calculate(
            entries = billingDataRepo.getEntries(range),
            income = billingDataRepo.getIncome(range),
            rangeStart = range?.timeStarted ?: Long.MIN_VALUE,
            rangeEnd = range?.timeEnded ?: Long.MAX_VALUE,
        )
    }
}
