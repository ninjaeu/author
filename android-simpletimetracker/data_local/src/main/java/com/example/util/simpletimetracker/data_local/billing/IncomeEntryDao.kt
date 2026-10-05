package com.example.util.simpletimetracker.data_local.billing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncomeEntryDao {

    @Query("SELECT * FROM incomeEntries ORDER BY date")
    suspend fun getAll(): List<IncomeEntryDBO>

    @Query("SELECT * FROM incomeEntries WHERE date >= :start AND date < :end ORDER BY date")
    suspend fun getByRange(start: Long, end: Long): List<IncomeEntryDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: IncomeEntryDBO): Long

    @Query("DELETE FROM incomeEntries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM incomeEntries")
    suspend fun clear()
}
