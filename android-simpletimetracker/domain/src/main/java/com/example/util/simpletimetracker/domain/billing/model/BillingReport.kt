package com.example.util.simpletimetracker.domain.billing.model

data class BillingReport(
    val totalMillis: Long,
    val billableMillis: Long,
    val nonBillableMillis: Long,
    val amountMinor: Long,
    val byProject: List<Line>,
    val byActivity: List<Line>,
) {

    /** Billable share of tracked time, 0.0 to 1.0. */
    val utilization: Double
        get() = if (totalMillis == 0L) 0.0 else billableMillis.toDouble() / totalMillis

    data class Line(
        val name: String,
        val billableMillis: Long,
        val nonBillableMillis: Long,
        val amountMinor: Long,
    ) {
        val totalMillis: Long get() = billableMillis + nonBillableMillis
    }

    companion object {
        const val UNASSIGNED_PROJECT = ""
    }
}
