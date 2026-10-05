package com.example.util.simpletimetracker.domain.billing

import com.example.util.simpletimetracker.domain.billing.export.BillingWorkbookBuilder
import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import com.example.util.simpletimetracker.domain.billing.model.BillingType
import com.example.util.simpletimetracker.domain.billing.model.IncomeEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingWorkbookBuilderTest {

    private val jan1 = 1_704_067_200_000L // 2024-01-01T00:00:00Z

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { result[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        return result
    }

    private fun entry(
        project: String = "Business",
        type: BillingType = BillingType.CLIENT_BILLABLE,
        comment: String = "",
        start: Long = jan1,
        millis: Long = 5_449_000, // 1:30:49
    ) = BillingEntry("Billable", project, "Business, Work (PhD) + Business", "", comment, start, start + millis, type, 12_500)

    private fun build(entries: List<BillingEntry>, income: List<IncomeEntry> = emptyList()): Map<String, String> {
        val out = ByteArrayOutputStream()
        BillingWorkbookBuilder().write(entries, income, ZoneOffset.UTC, out)
        return unzip(out.toByteArray())
    }

    @Test
    fun containsAllPackageParts() {
        val files = build(listOf(entry()))
        listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml",
            "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml",
        ).forEach { assertTrue("missing $it", files.containsKey(it)) }
        val wb = files["xl/workbook.xml"]!!
        assertTrue(wb.contains("""name="stt_records_automatic""""))
        assertTrue(wb.contains("""name="Income""""))
        assertTrue(wb.contains("""name="Billing""""))
    }

    @Test
    fun rawSheetKeepsAppColumnsAndAddsBillingColumns() {
        val sheet = build(listOf(entry()))["xl/worksheets/sheet1.xml"]!!
        val headers = listOf(
            "activity name", "time started", "time ended", "comment", "categories", "record tags",
            "duration", "duration minutes", "project", "type", "rate / h", "hours", "amount",
        )
        var from = 0
        headers.forEach {
            val i = sheet.indexOf(">$it<", from)
            assertTrue("header $it out of order", i >= from)
            from = i
        }
    }

    @Test
    fun writesSerialDatesDurationAndExactHours() {
        val sheet = build(listOf(entry()))["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<v>45292.0</v>"))
        assertTrue(sheet.contains("<f>G2*24</f>"))
        assertTrue(sheet.contains("""<f>IF(J2=&quot;Non-billable&quot;,0,L2*K2)</f>"""))
        assertTrue(sheet.contains("<v>90.0</v>")) // 1:30:49 truncates to 90 whole minutes, as the app export does
        assertTrue(sheet.contains("<v>125.0</v>")) // rate
    }

    @Test
    fun rawSheetHasDropDownValidationForTypeAndProject() {
        val sheet = build(listOf(entry(project = "Business"), entry(project = "GCA", type = BillingType.PAID)))["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("""sqref="J2:J1000""""))
        assertTrue(sheet.contains("&quot;Non-billable,Paid,Client-billable&quot;"))
        assertTrue(sheet.contains("""sqref="I2:I1000""""))
        assertTrue(sheet.contains("&quot;Business,GCA&quot;"))
    }

    @Test
    fun skipsProjectValidationWhenListWouldBeInvalid() {
        val tooMany = (1..40).map { entry(project = "Project number $it") }
        assertFalse(build(tooMany)["xl/worksheets/sheet1.xml"]!!.contains("""sqref="I2"""))
        val comma = build(listOf(entry(project = "A, B")))["xl/worksheets/sheet1.xml"]!!
        assertFalse(comma.contains("""sqref="I2"""))
    }

    @Test
    fun billingSheetHasTypeTotalsProjectRowsAndMonthRows() {
        val feb = jan1 + 40L * 24 * 3_600_000
        val sheet = build(
            listOf(entry(), entry(project = "GCA", type = BillingType.PAID, start = feb)),
            listOf(IncomeEntry(project = "GCA", kind = IncomeEntry.Kind.STIPEND, amountMinor = 300_000, date = feb)),
        )["xl/worksheets/sheet3.xml"]!!
        assertTrue(sheet.contains("SUMIFS(stt_records_automatic!\$L:\$L,stt_records_automatic!\$J:\$J,A2)"))
        assertTrue(sheet.contains("<f>IF(B2=0,&quot;n/a&quot;,B4/B2)</f>"))
        assertTrue(sheet.contains(">Business<"))
        assertTrue(sheet.contains(">GCA<"))
        assertTrue(sheet.contains("SUMIFS(Income!\$D:\$D,Income!\$B:\$B,\$A"))
        assertTrue(sheet.contains("EDATE("))
    }

    @Test
    fun incomeSheetListsAmountsInCurrencyUnits() {
        val sheet = build(
            listOf(entry()),
            listOf(IncomeEntry(project = "GCA", kind = IncomeEntry.Kind.BENEFIT, amountMinor = 123_456, date = jan1, note = "Housing")),
        )["xl/worksheets/sheet2.xml"]!!
        assertTrue(sheet.contains("<v>1234.56</v>"))
        assertTrue(sheet.contains(">Benefit<"))
        assertTrue(sheet.contains(">Housing<"))
    }

    @Test
    fun escapesXmlInText() {
        val files = build(listOf(entry(project = "R&D", comment = "a<b & \"c\"")))
        assertTrue(files["xl/worksheets/sheet1.xml"]!!.contains("a&lt;b &amp; &quot;c&quot;"))
    }

    @Test
    fun columnNamesRollOver() {
        assertEquals("A", XlsxWriter.columnName(0))
        assertEquals("Z", XlsxWriter.columnName(25))
        assertEquals("AA", XlsxWriter.columnName(26))
        assertEquals("BA", XlsxWriter.columnName(52))
    }
}
