package com.example.util.simpletimetracker.domain.billing.interactor

import com.example.util.simpletimetracker.domain.billing.model.BillingType

object BillingTypeResolver {

    /** A record override wins over the activity default. null means inherit. */
    fun resolve(activityDefault: BillingType, recordOverride: BillingType?): BillingType {
        return recordOverride ?: activityDefault
    }
}
