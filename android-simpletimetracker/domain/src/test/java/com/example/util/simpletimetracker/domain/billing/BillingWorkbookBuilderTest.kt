package com.example.util.simpletimetracker.domain.billing

import com.example.util.simpletimetracker.domain.billing.export.BillingWorkbookBuilder
import com.example.util.simpletimetracker.domain.billing.export.XlsxWriter
import com.example.util.simpletimetracker.domain.billing.model.BillingEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingWorkbookBuilderTest {

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { result[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        return result
    }

    private val jan1 = 1_704_067_200_000L // 2024-01-01T00:00:00Z

    private fun build(vararg entries: BillingEntry): Map<String, String> {
        val out = ByteArrayOutputStream()
        BillingWorkbookBuilder().write(
            entries.toList(),
            listOf(BillingWorkbookBuilder.Rate("Dev", true, 12_500)),
            ZoneOffset.UTC,
            out,
        )
        return unzip(out.toByteArray())
    }

    private fun entry(projects: List<String>, billable: Boolean = true, comment: String = "") =
        BillingEntry("Dev", projects, listOf("t1"), comment, jan1, jan1 + 5_400_000, billable, 12_500)

    @Test
    fun containsAllPackageParts() {
        val files = build(entry(listOf("Acme")))
        listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
            "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml",
        ).forEach { assertTrue("missing $it", files.containsKey(it)) }
    }

    @Test
    fun writesExcelSerialDatesAndFormulas() {
        val sheet = build(entry(listOf("Acme")))["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<v>45292.0</v>")) // 2024-01-01 00:00
        assertTrue(sheet.contains("<v>45292.0625</v>")) // + 1.5 h
        assertTrue(sheet.contains("<f>(B2-A2)*24</f>"))
        assertTrue(sheet.contains("""<f>IF(H2=&quot;Yes&quot;,G2*I2,0)</f>"""))
        assertTrue(sheet.contains("<v>125.0</v>"))
    }

    @Test
    fun summaryHasTotalsAndOneRowPerProject() {
        val sheet = build(entry(listOf("Acme")), entry(listOf("Beta"), billable = false))["xl/worksheets/sheet2.xml"]!!
        assertTrue(sheet.contains("SUMIFS(TimeLog!\$G:\$G,TimeLog!\$H:\$H,&quot;Yes&quot;)"))
        assertTrue(sheet.contains(">Acme<"))
        assertTrue(sheet.contains(">Beta<"))
    }

    @Test
    fun escapesXmlAndWildcardsInNames() {
        val files = build(entry(listOf("R&D *1"), comment = "a<b & \"c\""))
        val log = files["xl/worksheets/sheet1.xml"]!!
        assertTrue(log.contains("a&lt;b &amp; &quot;c&quot;"))
        assertTrue(files["xl/worksheets/sheet2.xml"]!!.contains("R&amp;D ~*1"))
    }

    @Test
    fun columnNamesRollOver() {
        assertEquals("A", XlsxWriter.columnName(0))
        assertEquals("Z", XlsxWriter.columnName(25))
        assertEquals("AA", XlsxWriter.columnName(26))
        assertEquals("AZ", XlsxWriter.columnName(51))
        assertEquals("BA", XlsxWriter.columnName(52))
    }
}
