package com.example.util.simpletimetracker.data_local.billing

import com.example.util.simpletimetracker.core.mapper.RecordTagFullNameMapper
import com.example.util.simpletimetracker.domain.billing.interactor.BillingTypeResolver
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import com.example.util.simpletimetracker.domain.billing.repo.BillingDataRepo
import com.example.util.simpletimetracker.domain.billing.repo.IncomeRepo
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.recordTag.repo.RecordTagRepo
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import javax.inject.Inject

class BillingDataRepoImpl @Inject constructor(
    private val recordTypeRepo: RecordTypeRepo,
    private val categoryRepo: CategoryRepo,
    private val recordRepo: RecordRepo,
    private val recordTypeCategoryRepo: RecordTypeCategoryRepo,
    private val recordTagRepo: RecordTagRepo,
    private val incomeRepo: IncomeRepo,
    private val recordTagFullNameMapper: RecordTagFullNameMapper,
) : BillingDataRepo {

    override suspend fun getEntries(range: Range?): List<BillingEntry> {
        val types = recordTypeRepo.getAll().associateBy { it.id }
        val categories = categoryRepo.getAll().associateBy { it.id }
        val tags = recordTagRepo.getAll().associateBy { it.id }
        val typeToCategories = recordTypeCategoryRepo.getAll()
            .groupBy(RecordTypeCategory::recordTypeId)
            .mapValues { (_, relations) -> relations.mapNotNull { categories[it.categoryId] } }
        val records = if (range != null) recordRepo.getFromRange(range) else recordRepo.getAll()

        return records.sortedBy { it.timeStarted }.mapNotNull { record ->
            val type = types[record.typeId] ?: return@mapNotNull null
            BillingEntry(
                activityName = type.name,
                project = categories[type.projectId]?.name.orEmpty(),
                // Same text the app's CSV export writes, so existing Excel trackers keep working.
                categories = typeToCategories[record.typeId].orEmpty().joinToString(", ") { it.name },
                tags = recordTagFullNameMapper.getFullName(
                    tags = record.tags.mapNotNull { tags[it.tagId] },
                    tagData = record.tags,
                ),
                comment = record.comment,
                timeStarted = record.timeStarted,
                timeEnded = record.timeEnded,
                type = BillingTypeResolver.resolve(type.billingType, record.billingTypeOverride),
                hourlyRateMinor = type.hourlyRateMinor,
            )
        }
    }

    override suspend fun getIncome(range: Range?): List<IncomeEntry> {
        val categories = categoryRepo.getAll().associateBy { it.id }
        val income = if (range != null) incomeRepo.getByRange(range.timeStarted, range.timeEnded) else incomeRepo.getAll()
        return income.map { it.copy(project = categories[it.projectId]?.name.orEmpty()) }
    }
}
