package com.example.util.simpletimetracker.domain.billing.model

/**
 * One tracked record with the activity data needed for billing.
 * [hourlyRateMinor] is the rate in minor currency units (cents) per hour.
 */
data class BillingEntry(
    val activityName: String,
    val projects: List<String>,
    val tags: List<String>,
    val comment: String,
    val timeStarted: Long,
    val timeEnded: Long,
    val billable: Boolean,
    val hourlyRateMinor: Long,
)
