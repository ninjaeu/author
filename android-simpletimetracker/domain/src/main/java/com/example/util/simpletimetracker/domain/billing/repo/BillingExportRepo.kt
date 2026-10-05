package com.example.util.simpletimetracker.domain.billing.repo

import com.example.util.simpletimetracker.domain.backup.model.ResultCode
import com.example.util.simpletimetracker.domain.record.model.Range

interface BillingExportRepo {

    /** Writes the billing workbook (.xlsx) to the document the user picked. */
    suspend fun saveBillingWorkbook(
        uriString: String,
        range: Range?,
    ): ResultCode
}
