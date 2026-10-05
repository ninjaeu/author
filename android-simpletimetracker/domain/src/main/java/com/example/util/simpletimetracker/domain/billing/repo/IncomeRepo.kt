package com.example.util.simpletimetracker.domain.billing.repo

import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry

interface IncomeRepo {

    suspend fun getAll(): List<IncomeEntry>

    suspend fun getByRange(start: Long, end: Long): List<IncomeEntry>

    suspend fun add(entry: IncomeEntry): Long

    suspend fun remove(id: Long)

    suspend fun clear()
}
