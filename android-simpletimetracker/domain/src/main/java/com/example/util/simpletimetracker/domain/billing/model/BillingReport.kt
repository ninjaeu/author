package com.example.util.simpletimetracker.domain.billing.model

data class BillingReport(
    val total: Split,
    val byProject: List<Line>,
    val byActivity: List<Line>,
    val incomeMinor: Long,
) {

    /** Time in each billing type, plus the money rates produce. */
    data class Split(
        val paidMillis: Long = 0,
        val clientMillis: Long = 0,
        val nonBillableMillis: Long = 0,
        val rateAmountMinor: Long = 0,
    ) {
        val totalMillis: Long get() = paidMillis + clientMillis + nonBillableMillis

        /** Unpaid hours spent for each client-billable hour. null when no client hours exist. */
        val unpaidPerClientHour: Double?
            get() = if (clientMillis == 0L) null else nonBillableMillis.toDouble() / clientMillis

        val earningShare: Double
            get() = if (totalMillis == 0L) 0.0 else (paidMillis + clientMillis).toDouble() / totalMillis
    }

    data class Line(
        val name: String,
        val split: Split,
        val incomeMinor: Long,
    ) {
        /** Rate amounts plus income, divided by all tracked hours. null when no time is tracked. */
        val returnPerHourMinor: Long?
            get() = if (split.totalMillis == 0L) null else
                java.math.BigDecimal(split.rateAmountMinor + incomeMinor)
                    .multiply(java.math.BigDecimal(MILLIS_IN_HOUR))
                    .divide(java.math.BigDecimal(split.totalMillis), 0, java.math.RoundingMode.HALF_UP)
                    .toLong()
    }

    companion object {
        const val NO_PROJECT = ""
        const val MILLIS_IN_HOUR = 3_600_000L
    }
}
