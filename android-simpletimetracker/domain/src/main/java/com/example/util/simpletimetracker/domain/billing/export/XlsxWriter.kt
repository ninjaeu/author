package com.example.util.simpletimetracker.domain.billing.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal dependency-free .xlsx writer (Office Open XML): text, numbers,
 * date-times and formulas. Excel recalculates formulas when the file opens.
 */
class XlsxWriter {

    sealed class Cell {
        data class Text(val value: String, val style: Style = Style.DEFAULT) : Cell()
        data class Number(val value: Double, val style: Style = Style.DEFAULT) : Cell()
        data class Formula(val formula: String, val style: Style = Style.DEFAULT) : Cell()
        object Empty : Cell()
    }

    /** Index in styles.xml cellXfs. */
    enum class Style(val index: Int) {
        DEFAULT(0),
        HEADER(1),
        DATE_TIME(2),
        HOURS(3),
        MONEY(4),
        PERCENT(5),
        DATE_TIME_SECONDS(6),
        DURATION(7),
        MONTH(8),
        DATE(9),
    }

    /** List validation: only the listed values are accepted in [range], e.g. "J2:J1000". */
    class ListValidation(val range: String, val values: List<String>, val title: String, val message: String)

    class Sheet(
        val name: String,
        val columnWidths: List<Double> = emptyList(),
        val freezeHeader: Boolean = true,
    ) {
        val rows = mutableListOf<List<Cell>>()
        val validations = mutableListOf<ListValidation>()
        fun row(vararg cells: Cell) = apply { rows.add(cells.toList()) }
        fun row(cells: List<Cell>) = apply { rows.add(cells) }
    }

    fun write(sheets: List<Sheet>, output: OutputStream) {
        require(sheets.isNotEmpty()) { "Workbook needs at least one sheet" }
        ZipOutputStream(output).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(sheets.size))
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(sheets))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, sheet -> zip.put("xl/worksheets/sheet${i + 1}.xml", sheetXml(sheet)) }
        }
    }

    private fun ZipOutputStream.put(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(sheetCount: Int) = buildString {
        append(XML_HEADER)
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun workbook(sheets: List<Sheet>) = buildString {
        append(XML_HEADER)
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { i, s ->
            append("""<sheet name="${escape(s.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("""</sheets><calcPr fullCalcOnLoad="1"/></workbook>""")
    }

    private fun workbookRels(sheetCount: Int) = buildString {
        append(XML_HEADER)
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun sheetXml(sheet: Sheet) = buildString {
        append(XML_HEADER)
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (sheet.freezeHeader) {
            append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        }
        if (sheet.columnWidths.isNotEmpty()) {
            append("<cols>")
            sheet.columnWidths.forEachIndexed { i, w ->
                append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
            }
            append("</cols>")
        }
        append("<sheetData>")
        sheet.rows.forEachIndexed { r, cells ->
            append("""<row r="${r + 1}">""")
            cells.forEachIndexed { c, cell -> append(cellXml(columnName(c) + (r + 1), cell)) }
            append("</row>")
        }
        append("</sheetData>")
        val valid = sheet.validations.filter { v -> v.values.isNotEmpty() && v.values.none { ',' in it || '"' in it } && v.values.joinToString(",").length <= MAX_LIST_LENGTH }
        if (valid.isNotEmpty()) {
            append("""<dataValidations count="${valid.size}">""")
            valid.forEach { v ->
                append("""<dataValidation type="list" allowBlank="1" showErrorMessage="1" errorTitle="${escape(v.title)}" error="${escape(v.message)}" sqref="${v.range}">""")
                append("<formula1>${escape("\"" + v.values.joinToString(",") + "\"")}</formula1></dataValidation>")
            }
            append("</dataValidations>")
        }
        append("</worksheet>")
    }

    private fun cellXml(ref: String, cell: Cell): String = when (cell) {
        is Cell.Empty -> ""
        is Cell.Text -> """<c r="$ref" s="${cell.style.index}" t="inlineStr"><is><t xml:space="preserve">${escape(cell.value)}</t></is></c>"""
        is Cell.Number -> """<c r="$ref" s="${cell.style.index}"><v>${cell.value}</v></c>"""
        is Cell.Formula -> """<c r="$ref" s="${cell.style.index}"><f>${escape(cell.formula)}</f></c>"""
    }

    companion object {
        /** Excel limits an inline validation list to 255 characters. */
        private const val MAX_LIST_LENGTH = 255
        private const val XML_HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

        private const val ROOT_RELS = XML_HEADER +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

        // numFmt 164 date-time, 165 hours, 166 money, 167 percent.
        private const val STYLES = XML_HEADER +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<numFmts count="8">""" +
            """<numFmt numFmtId="164" formatCode="yyyy-mm-dd hh:mm"/>""" +
            """<numFmt numFmtId="165" formatCode="0.00"/>""" +
            """<numFmt numFmtId="166" formatCode="#,##0.00"/>""" +
            """<numFmt numFmtId="167" formatCode="0.0%"/>""" +
            """<numFmt numFmtId="168" formatCode="yyyy-mm-dd hh:mm:ss"/>""" +
            """<numFmt numFmtId="169" formatCode="[h]:mm:ss"/>""" +
            """<numFmt numFmtId="170" formatCode="mmm yyyy"/>""" +
            """<numFmt numFmtId="171" formatCode="yyyy-mm-dd"/>""" +
            "</numFmts>" +
            """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FFD9E1F2"/></patternFill></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="10">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
            """<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="166" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="167" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="168" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="169" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="170" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="171" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            "</cellXfs>" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            "</styleSheet>"

        /** 0 -> A, 25 -> Z, 26 -> AA. */
        fun columnName(index: Int): String {
            var n = index
            val sb = StringBuilder()
            do {
                sb.insert(0, ('A' + n % 26))
                n = n / 26 - 1
            } while (n >= 0)
            return sb.toString()
        }

        private fun escape(s: String): String {
            val sb = StringBuilder(s.length)
            for (ch in s) {
                when {
                    ch == '&' -> sb.append("&amp;")
                    ch == '<' -> sb.append("&lt;")
                    ch == '>' -> sb.append("&gt;")
                    ch == '"' -> sb.append("&quot;")
                    ch.code < 0x20 && ch != '\n' && ch != '\t' && ch != '\r' -> Unit
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }
    }
}
