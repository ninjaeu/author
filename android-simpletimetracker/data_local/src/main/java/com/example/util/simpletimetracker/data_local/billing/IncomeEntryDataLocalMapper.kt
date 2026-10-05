package com.example.util.simpletimetracker.data_local.billing

import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import javax.inject.Inject

class IncomeEntryDataLocalMapper @Inject constructor() {

    fun map(dbo: IncomeEntryDBO): IncomeEntry {
        return IncomeEntry(
            id = dbo.id,
            projectId = dbo.projectId,
            kind = IncomeEntry.Kind.fromDb(dbo.kind),
            amountMinor = dbo.amountMinor,
            date = dbo.date,
            note = dbo.note,
        )
    }

    fun map(domain: IncomeEntry): IncomeEntryDBO {
        return IncomeEntryDBO(
            id = domain.id,
            projectId = domain.projectId,
            kind = domain.kind.dbValue,
            amountMinor = domain.amountMinor,
            date = domain.date,
            note = domain.note,
        )
    }
}
