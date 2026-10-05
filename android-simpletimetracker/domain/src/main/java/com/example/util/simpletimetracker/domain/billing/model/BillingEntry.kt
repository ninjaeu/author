package com.example.util.simpletimetracker.domain.billing.model

/**
 * One tracked record with the activity data needed for billing.
 * [hourlyRateMinor] is in minor currency units (cents) per hour; 0 means no rate.
 * [project] is the single primary project, empty when the activity has none.
 */
data class BillingEntry(
    val activityName: String,
    val project: String,
    val categories: String,
    val tags: String,
    val comment: String,
    val timeStarted: Long,
    val timeEnded: Long,
    val type: BillingType,
    val hourlyRateMinor: Long,
)
