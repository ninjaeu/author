package com.example.util.simpletimetracker.data_local.billing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomeEntries")
data class IncomeEntryDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    // IncomeEntry.Kind.dbValue.
    @ColumnInfo(name = "kind")
    val kind: Int,

    // Minor currency units (cents).
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    // Epoch millis.
    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "note")
    val note: String,
)
