package com.example.util.simpletimetracker.domain.recordType.model

import com.example.util.simpletimetracker.domain.billing.model.BillingType
import com.example.util.simpletimetracker.domain.color.model.AppColor

data class RecordType(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: AppColor,
    val defaultDuration: Long,
    val note: String,
    val hidden: Boolean = false,
    val billingType: BillingType = BillingType.NON_BILLABLE,
    // Minor currency units (cents) per hour, 0 means no rate.
    val hourlyRateMinor: Long = 0,
    // Category id used as the primary project, 0 when none.
    val projectId: Long = 0,
)