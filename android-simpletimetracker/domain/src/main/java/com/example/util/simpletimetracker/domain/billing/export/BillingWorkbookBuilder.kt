package com.example.util.simpletimetracker.domain.billing.export

import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter.Cell
import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter.Style
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Builds the three-sheet billing workbook:
 * TimeLog (one row per record, hours and amount as formulas),
 * Summary (totals and per-project / per-activity SUMIFS formulas),
 * Rates (activity defaults).
 */
class BillingWorkbookBuilder @Inject constructor() {

    data class Rate(val activityName: String, val billable: Boolean, val hourlyRateMinor: Long)

    fun write(
        entries: List<BillingEntry>,
        rates: List<Rate>,
        zone: ZoneId,
        output: OutputStream,
    ) {
        val sorted = entries.sortedBy { it.timeStarted }
        XlsxWriter().write(
            listOf(timeLog(sorted, zone), summary(sorted), ratesSheet(rates)),
            output,
        )
    }

    private fun timeLog(entries: List<BillingEntry>, zone: ZoneId): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(
            name = TIME_LOG,
            columnWidths = listOf(17.0, 17.0, 26.0, 26.0, 22.0, 30.0, 9.0, 10.0, 10.0, 11.0),
        )
        sheet.row(
            listOf("Start", "End", "Activity", "Project / Client", "Tags", "Comment", "Hours", "Billable", "Rate / h", "Amount")
                .map { Cell.Text(it, Style.HEADER) },
        )
        entries.forEachIndexed { i, e ->
            val r = i + 2
            sheet.row(
                Cell.Number(excelSerial(e.timeStarted, zone), Style.DATE_TIME),
                Cell.Number(excelSerial(e.timeEnded, zone), Style.DATE_TIME),
                Cell.Text(e.activityName),
                Cell.Text(e.projects.joinToString(", ")),
                Cell.Text(e.tags.joinToString(", ")),
                Cell.Text(e.comment),
                Cell.Formula("(B$r-A$r)*24", Style.HOURS),
                Cell.Text(if (e.billable) YES else NO),
                Cell.Number(e.hourlyRateMinor / 100.0, Style.MONEY),
                Cell.Formula("""IF(H$r="$YES",G$r*I$r,0)""", Style.MONEY),
            )
        }
        return sheet
    }

    private fun summary(entries: List<BillingEntry>): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(
            name = SUMMARY,
            columnWidths = listOf(30.0, 14.0, 16.0, 14.0, 14.0, 14.0),
        )
        val hours = "$TIME_LOG!\$G:\$G"
        val billable = "$TIME_LOG!\$H:\$H"
        val amount = "$TIME_LOG!\$J:\$J"
        sheet.row(listOf("Metric", "Value").map { Cell.Text(it, Style.HEADER) })
        sheet.row(Cell.Text("Total hours"), Cell.Formula("SUM($hours)", Style.HOURS))
        sheet.row(Cell.Text("Billable hours"), Cell.Formula("""SUMIFS($hours,$billable,"$YES")""", Style.HOURS))
        sheet.row(Cell.Text("Non-billable hours"), Cell.Formula("""SUMIFS($hours,$billable,"$NO")""", Style.HOURS))
        sheet.row(Cell.Text("Utilization"), Cell.Formula("IF(B2=0,0,B3/B2)", Style.PERCENT))
        sheet.row(Cell.Text("Billable amount"), Cell.Formula("SUM($amount)", Style.MONEY))
        sheet.row(Cell.Empty)

        sheet.row(
            listOf("Project / Client", "Billable h", "Non-billable h", "Total h", "Amount")
                .map { Cell.Text(it, Style.HEADER) },
        )
        val projects = entries.flatMap { it.projects }.distinct().sorted()
        val projectCol = "$TIME_LOG!\$D:\$D"
        projects.forEach { name ->
            val row = sheet.rows.size + 1
            val key = "\"*${wildcardSafe(name).replace("\"", "\"\"")}*\""
            sheet.row(
                Cell.Text(name),
                Cell.Formula("SUMIFS($hours,$projectCol,$key,$billable,\"$YES\")", Style.HOURS),
                Cell.Formula("SUMIFS($hours,$projectCol,$key,$billable,\"$NO\")", Style.HOURS),
                Cell.Formula("B$row+C$row", Style.HOURS),
                Cell.Formula("SUMIFS($amount,$projectCol,$key)", Style.MONEY),
            )
        }
        if (entries.any { it.projects.isEmpty() }) {
            val row = sheet.rows.size + 1
            sheet.row(
                Cell.Text("(No project)"),
                Cell.Formula("SUMIFS($hours,$projectCol,\"\",$billable,\"$YES\")", Style.HOURS),
                Cell.Formula("SUMIFS($hours,$projectCol,\"\",$billable,\"$NO\")", Style.HOURS),
                Cell.Formula("B$row+C$row", Style.HOURS),
                Cell.Formula("SUMIFS($amount,$projectCol,\"\")", Style.MONEY),
            )
        }
        return sheet
    }

    private fun ratesSheet(rates: List<Rate>): XlsxWriter.Sheet {
        val sheet = XlsxWriter.Sheet(name = RATES, columnWidths = listOf(30.0, 12.0, 12.0))
        sheet.row(listOf("Activity", "Billable", "Rate / h").map { Cell.Text(it, Style.HEADER) })
        rates.sortedBy { it.activityName }.forEach {
            sheet.row(
                Cell.Text(it.activityName),
                Cell.Text(if (it.billable) YES else NO),
                Cell.Number(it.hourlyRateMinor / 100.0, Style.MONEY),
            )
        }
        return sheet
    }

    /** SUMIFS treats * ? ~ as wildcards; a ~ prefix makes them literal. */
    private fun wildcardSafe(name: String): String =
        name.replace("~", "~~").replace("*", "~*").replace("?", "~?")

    /** Excel serial date: days since 1899-12-30 in the given zone's local time. */
    private fun excelSerial(epochMillis: Long, zone: ZoneId): Double {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()
        val epoch = java.time.LocalDateTime.of(1899, 12, 30, 0, 0)
        val seconds = ChronoUnit.SECONDS.between(epoch, local)
        return seconds / SECONDS_IN_DAY
    }

    companion object {
        const val TIME_LOG = "TimeLog"
        const val SUMMARY = "Summary"
        const val RATES = "Rates"
        private const val YES = "Yes"
        private const val NO = "No"
        private const val SECONDS_IN_DAY = 86_400.0
    }
}
