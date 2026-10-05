package com.example.util.simpletimetracker.domain.record.model

import com.example.util.simpletimetracker.domain.billing.model.BillingType

data class Record(
    val id: Long = 0,
    val typeId: Long,
    override val timeStarted: Long,
    override val timeEnded: Long,
    override val comment: String,
    override val tags: List<RecordBase.Tag>,
    // null means use the activity default.
    val billingTypeOverride: BillingType? = null,
) : RecordBase {

    override val typeIds: List<Long> = listOf(typeId)
}