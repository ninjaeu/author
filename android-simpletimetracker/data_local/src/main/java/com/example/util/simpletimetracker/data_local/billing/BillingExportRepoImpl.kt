package com.example.util.simpletimetracker.data_local.billing

import android.content.ContentResolver
import androidx.core.net.toUri
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.backup.model.ResultCode
import com.example.util.simpletimetracker.domain.billing.export.BillingWorkbookBuilder
import com.example.util.simpletimetracker.domain.billing.repo.BillingDataRepo
import com.example.util.simpletimetracker.domain.billing.repo.BillingExportRepo
import com.example.util.simpletimetracker.domain.record.model.Range
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class BillingExportRepoImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val billingDataRepo: BillingDataRepo,
    private val workbookBuilder: BillingWorkbookBuilder,
    private val resourceRepo: ResourceRepo,
) : BillingExportRepo {

    override suspend fun saveBillingWorkbook(
        uriString: String,
        range: Range?,
    ): ResultCode = withContext(Dispatchers.IO) {
        try {
            val entries = billingDataRepo.getEntries(range)
            val income = billingDataRepo.getIncome(range)
            val stream = contentResolver.openOutputStream(uriString.toUri(), "wt")
                ?: throw IOException("Cannot open $uriString")
            stream.buffered().use { out ->
                workbookBuilder.write(entries, income, ZoneId.systemDefault(), out)
            }
            ResultCode.Success(resourceRepo.getString(R.string.message_export_complete))
        } catch (e: Exception) {
            Timber.e(e)
            ResultCode.Error(resourceRepo.getString(R.string.message_export_error))
        }
    }
}
