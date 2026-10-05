package com.example.util.simpletimetracker.domain.billing.model

/**
 * How an activity's time earns money. [dbValue] is stored in the database,
 * [label] is written into the Excel export and must stay stable.
 */
enum class BillingType(val dbValue: Int, val label: String) {
    NON_BILLABLE(0, "Non-billable"),
    PAID(1, "Paid"),
    CLIENT_BILLABLE(2, "Client-billable"),
    ;

    companion object {
        fun fromDb(value: Int?): BillingType? = values().firstOrNull { it.dbValue == value }

        fun fromDbOrDefault(value: Int): BillingType = fromDb(value) ?: NON_BILLABLE

        fun fromLabel(label: String): BillingType? = values().firstOrNull { it.label == label }
    }
}
