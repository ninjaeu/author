package com.example.util.simpletimetracker.domain.billing.model

/**
 * Lump-sum income attached to a project: a monthly stipend, a travel grant,
 * the value of housing. Counted once, on [date], in any report range containing it.
 */
data class IncomeEntry(
    val id: Long = 0,
    // Category id of the project, 0 when none.
    val projectId: Long = 0,
    // Project display name, filled in from [projectId] when a report or export is built.
    val project: String = "",
    val kind: Kind,
    val amountMinor: Long,
    val date: Long,
    val note: String = "",
) {
    enum class Kind(val dbValue: Int, val label: String) {
        STIPEND(0, "Stipend"),
        GRANT(1, "Grant"),
        BENEFIT(2, "Benefit"),
        OTHER(3, "Other"),
        ;

        companion object {
            fun fromDb(value: Int): Kind = values().firstOrNull { it.dbValue == value } ?: OTHER
        }
    }
}
