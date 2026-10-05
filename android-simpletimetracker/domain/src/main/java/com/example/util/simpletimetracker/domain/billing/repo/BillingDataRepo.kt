package com.example.util.simpletimetracker.domain.billing.repo

import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import com.example.util.simpletimetracker.domain.record.model.Range

/** Records and income joined with activity, project and tag names, ready for reports and export. */
interface BillingDataRepo {

    suspend fun getEntries(range: Range?): List<BillingEntry>

    suspend fun getIncome(range: Range?): List<IncomeEntry>
}
