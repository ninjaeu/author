package com.example.util.simpletimetracker.domain.billing.interactor

import com.example.util.simpletimetracker.domain.billing.model.BillableOverride

object BillableResolver {

    fun isBillable(activityDefault: Boolean, override: BillableOverride): Boolean = when (override) {
        BillableOverride.INHERIT -> activityDefault
        BillableOverride.BILLABLE -> true
        BillableOverride.NON_BILLABLE -> false
    }
}
