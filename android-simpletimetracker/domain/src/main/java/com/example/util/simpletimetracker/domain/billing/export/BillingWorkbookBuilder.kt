package com.example.util.simpletimetracker.domain.billing.export

import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter.Cell
import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter.Style
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingType
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Builds the billing workbook.
 *
 * Sheet [RAW]: paste-compatible. Columns A-H match the app's CSV export that existing
 * Excel trackers already read (activity name, time started, time ended, comment,
 * categories, record tags, duration, duration minutes). Columns I-M are new: Project,
 * Type, Rate / h, Hours (exact, to the second) and Amount.
 * Sheet [INCOME]: lump-sum income entries.
 * Sheet [BILLING]: totals by type, by project and by month, all as live formulas.
 */
class BillingWorkbookBuilder @Inject constructor() {

    fun write(
        entries: List<BillingEntry>,
        income: List<IncomeEntry>,
        zone: ZoneId,
        output: OutputStream,
    ) {
        val sorted = entries.sortedBy { it.timeStarted }
        XlsxWriter().write(
            listOf(rawSheet(sorted, zone), incomeSheet(income, zone), billingSheet(sorted, income, zone)),
            output,
        )
    }

    private fun rawSheet(entries: List<BillingEntry>, zone: ZoneId): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(
            name = RAW,
            columnWidths = listOf(22.0, 20.0, 20.0, 24.0, 40.0, 14.0, 11.0, 12.0, 18.0, 16.0, 10.0, 10.0, 12.0),
        )
        sheet.row(
            listOf(
                "activity name", "time started", "time ended", "comment", "categories", "record tags",
                "duration", "duration minutes", "project", "type", "rate / h", "hours", "amount",
            ).map { Cell.Text(it, Style.HEADER) },
        )
        entries.forEachIndexed { i, e ->
            val r = i + 2
            val seconds = (e.timeEnded - e.timeStarted) / MILLIS_IN_SECOND
            sheet.row(
                Cell.Text(e.activityName),
                Cell.Number(excelSerial(e.timeStarted, zone), Style.DATE_TIME_SECONDS),
                Cell.Number(excelSerial(e.timeEnded, zone), Style.DATE_TIME_SECONDS),
                Cell.Text(e.comment),
                Cell.Text(e.categories),
                Cell.Text(e.tags),
                Cell.Number(seconds / SECONDS_IN_DAY, Style.DURATION),
                Cell.Number((seconds / SECONDS_IN_MINUTE).toDouble()),
                Cell.Text(e.project),
                Cell.Text(e.type.label),
                Cell.Number(e.hourlyRateMinor / 100.0, Style.MONEY),
                Cell.Formula("G$r*24", Style.HOURS),
                Cell.Formula("""IF(J$r="${BillingType.NON_BILLABLE.label}",0,L$r*K$r)""", Style.MONEY),
            )
        }
        val lastRow = maxOf(entries.size + 1, VALIDATION_ROWS)
        sheet.validations.add(
            XlsxWriter.ListValidation(
                range = "J2:J$lastRow",
                values = BillingType.values().map { it.label },
                title = "Type",
                message = "Choose Paid, Client-billable or Non-billable.",
            ),
        )
        sheet.validations.add(
            XlsxWriter.ListValidation(
                range = "I2:I$lastRow",
                values = entries.map { it.project }.filter { it.isNotEmpty() }.distinct().sorted(),
                title = "Project",
                message = "Choose a project that exists in the app.",
            ),
        )
        return sheet
    }

    private fun incomeSheet(income: List<IncomeEntry>, zone: ZoneId): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(name = INCOME, columnWidths = listOf(14.0, 24.0, 12.0, 14.0, 36.0))
        sheet.row(listOf("date", "project", "kind", "amount", "note").map { Cell.Text(it, Style.HEADER) })
        income.sortedBy { it.date }.forEach {
            sheet.row(
                Cell.Number(excelSerial(it.date, zone), Style.DATE),
                Cell.Text(it.project),
                Cell.Text(it.kind.label),
                Cell.Number(it.amountMinor / 100.0, Style.MONEY),
                Cell.Text(it.note),
            )
        }
        sheet.validations.add(
            XlsxWriter.ListValidation(
                range = "C2:C500",
                values = IncomeEntry.Kind.values().map { it.label },
                title = "Kind",
                message = "Choose Stipend, Grant, Benefit or Other.",
            ),
        )
        return sheet
    }

    private fun billingSheet(
        entries: List<BillingEntry>,
        income: List<IncomeEntry>,
        zone: ZoneId,
    ): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(
            name = BILLING,
            columnWidths = listOf(34.0, 12.0, 14.0, 16.0, 12.0, 14.0, 12.0, 14.0),
            freezeHeader = false,
        )
        val hours = "$RAW!\$L:\$L"
        val type = "$RAW!\$J:\$J"
        val project = "$RAW!\$I:\$I"
        val amount = "$RAW!\$M:\$M"
        val start = "$RAW!\$B:\$B"

        // Rows 1-5: totals by type.
        sheet.row(listOf("Type", "Hours", "Share", "Amount").map { Cell.Text(it, Style.HEADER) })
        BillingType.values().sortedByDescending { it.dbValue }.forEach { t ->
            val r = sheet.rows.size + 1
            sheet.row(
                Cell.Text(t.label),
                Cell.Formula("""SUMIFS($hours,$type,A$r)""", Style.HOURS),
                Cell.Formula("IF(B\$5=0,0,B$r/B\$5)", Style.PERCENT),
                Cell.Formula("""SUMIFS($amount,$type,A$r)""", Style.MONEY),
            )
        }
        sheet.row(
            Cell.Text("Total", Style.HEADER),
            Cell.Formula("SUM(B2:B4)", Style.HOURS),
            Cell.Formula("SUM(C2:C4)", Style.PERCENT),
            Cell.Formula("SUM(D2:D4)", Style.MONEY),
        )
        // Rows are ordered Client-billable, Paid, Non-billable (dbValue descending).
        sheet.row(
            Cell.Text("Unpaid hours per client-billable hour"),
            Cell.Formula("""IF(B2=0,"n/a",B4/B2)""", Style.HOURS),
        )
        sheet.row(Cell.Empty)

        // By project.
        sheet.row(
            listOf("Project", "Client h", "Paid h", "Non-billable h", "Total h", "Rate amount", "Income", "Return / h")
                .map { Cell.Text(it, Style.HEADER) },
        )
        val projects = (entries.map { it.project } + income.map { it.project }).distinct().sorted()
        projects.forEach { name ->
            val r = sheet.rows.size + 1
            val crit = if (name.isEmpty()) "\"\"" else "\$A$r"
            sheet.row(
                Cell.Text(name.ifEmpty { "(no project)" }),
                Cell.Formula("""SUMIFS($hours,$project,$crit,$type,"${BillingType.CLIENT_BILLABLE.label}")""", Style.HOURS),
                Cell.Formula("""SUMIFS($hours,$project,$crit,$type,"${BillingType.PAID.label}")""", Style.HOURS),
                Cell.Formula("""SUMIFS($hours,$project,$crit,$type,"${BillingType.NON_BILLABLE.label}")""", Style.HOURS),
                Cell.Formula("SUM(B$r:D$r)", Style.HOURS),
                Cell.Formula("SUMIFS($amount,$project,$crit)", Style.MONEY),
                Cell.Formula("SUMIFS($INCOME!\$D:\$D,$INCOME!\$B:\$B,$crit)", Style.MONEY),
                Cell.Formula("IF(E$r=0,\"\",(F$r+G$r)/E$r)", Style.MONEY),
            )
        }
        sheet.row(Cell.Empty)

        // By month.
        sheet.row(
            listOf("Month", "Client h", "Paid h", "Non-billable h", "Unpaid per client h")
                .map { Cell.Text(it, Style.HEADER) },
        )
        val months = entries.map { YearMonth.from(Instant.ofEpochMilli(it.timeStarted).atZone(zone)) }
        if (months.isNotEmpty()) {
            var m = months.min()
            val last = months.max()
            while (m <= last) {
                val r = sheet.rows.size + 1
                val range = """$start,">="&A$r,$start,"<"&EDATE(A$r,1)"""
                sheet.row(
                    Cell.Number(excelSerial(m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(), zone), Style.MONTH),
                    Cell.Formula("""SUMIFS($hours,$type,"${BillingType.CLIENT_BILLABLE.label}",$range)""", Style.HOURS),
                    Cell.Formula("""SUMIFS($hours,$type,"${BillingType.PAID.label}",$range)""", Style.HOURS),
                    Cell.Formula("""SUMIFS($hours,$type,"${BillingType.NON_BILLABLE.label}",$range)""", Style.HOURS),
                    Cell.Formula("""IF(B$r=0,"n/a",D$r/B$r)""", Style.HOURS),
                )
                m = m.plusMonths(1)
            }
        }
        return sheet
    }

    /** Excel serial date-time: days since 1899-12-30 in the given zone's local time. */
    private fun excelSerial(epochMillis: Long, zone: ZoneId): Double {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()
        val seconds = ChronoUnit.SECONDS.between(EXCEL_EPOCH, local)
        return seconds / SECONDS_IN_DAY
    }

    companion object {
        const val RAW = "stt_records_automatic"
        const val INCOME = "Income"
        const val BILLING = "Billing"
        private const val VALIDATION_ROWS = 1000
        private const val MILLIS_IN_SECOND = 1000L
        private const val SECONDS_IN_MINUTE = 60L
        private const val SECONDS_IN_DAY = 86_400.0
        private val EXCEL_EPOCH = LocalDateTime.of(1899, 12, 30, 0, 0)
    }
}
