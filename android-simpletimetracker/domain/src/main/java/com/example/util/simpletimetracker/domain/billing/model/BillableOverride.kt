package com.example.util.simpletimetracker.domain.billing.model

/**
 * Per-record billing flag. INHERIT uses the activity default.
 * [dbValue] is stored in the records table (null column value means INHERIT).
 */
enum class BillableOverride(val dbValue: Int?) {
    INHERIT(null),
    BILLABLE(1),
    NON_BILLABLE(0),
    ;

    companion object {
        fun fromDb(value: Int?): BillableOverride = when (value) {
            1 -> BILLABLE
            0 -> NON_BILLABLE
            else -> INHERIT
        }
    }
}
