package com.example.util.simpletimetracker.data_local.billing

import com.example.util.simpletimetracker.data_local.base.logDataAccess
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import com.example.util.simpletimetracker.domain.billing.repo.IncomeRepo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class IncomeRepoImpl @Inject constructor(
    private val dao: IncomeEntryDao,
    private val mapper: IncomeEntryDataLocalMapper,
) : IncomeRepo {

    override suspend fun getAll(): List<IncomeEntry> = withContext(Dispatchers.IO) {
        logDataAccess("getAll")
        dao.getAll().map(mapper::map)
    }

    override suspend fun getByRange(start: Long, end: Long): List<IncomeEntry> = withContext(Dispatchers.IO) {
        logDataAccess("getByRange")
        dao.getByRange(start, end).map(mapper::map)
    }

    override suspend fun add(entry: IncomeEntry): Long = withContext(Dispatchers.IO) {
        logDataAccess("add")
        dao.insert(mapper.map(entry))
    }

    override suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        logDataAccess("remove")
        dao.delete(id)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        logDataAccess("clear")
        dao.clear()
    }
}
