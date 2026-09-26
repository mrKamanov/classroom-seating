package com.example.classroomseating.core.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudentFileParserTest {

    @Test
    fun journalParsesNamesStartingFromRow4ColumnB() {
        // Раскладка, как в экспорте «Моя школа»: заголовки в строке 1,
        // список класса — колонка B, начиная с 4-й строки, «Фамилия Имя Отчество».
        val bytes = buildJournalXlsx(
            rows = listOf(
                listOf("A1" to "№", "B1" to "Обучающийся", "U1" to "Дата"),
                listOf("B2" to "демо"),
                listOf("B3" to "марки"),
                listOf("A4" to "1", "B4" to "Архиреев Алексей Алексеевич  "),
                listOf("A5" to "2", "B5" to "Багдасарян Эдгар Каренович  "),
                listOf("A6" to "3", "B6" to "Садаев Мухамад-Али Исаевич"),
                listOf("U7" to "Учитель: Иванова И.И."),
                listOf("U8" to "Копия электронного журнала верна:")
            )
        )

        val drafts = StudentFileParser.parseJournalXlsx(bytes)

        assertEquals(3, drafts.size)
        assertEquals(StudentDraft("Архиреев", "Алексей"), drafts[0])
        assertEquals(StudentDraft("Багдасарян", "Эдгар"), drafts[1])
        assertEquals(StudentDraft("Садаев", "Мухамад-Али"), drafts[2])
    }

    @Test
    fun journalIgnoresRowsBeforeRow4AndFooter() {
        val bytes = buildJournalXlsx(
            rows = listOf(
                listOf("B1" to "Обучающийся"),
                listOf("B2" to "Иванов Иван"), // до строки 4 — игнор
                listOf("A4" to "1", "B4" to "Петров Пётр Петрович"),
                listOf("A5" to "2", "B5" to "Сидоров Олег Иванович"),
                listOf("U9" to "Директор_______________________")
            )
        )

        val drafts = StudentFileParser.parseJournalXlsx(bytes)

        assertEquals(listOf(StudentDraft("Петров", "Пётр"), StudentDraft("Сидоров", "Олег")), drafts)
    }

    @Test
    fun journalTrailingSpacesAndDuplicatesHandled() {
        val bytes = buildJournalXlsx(
            rows = listOf(
                listOf("B1" to "Обучающийся"),
                listOf("A4" to "1", "B4" to "Смирнова Алина Евгеньевна   "),
                listOf("A5" to "2", "B5" to " Смирнова Алина Евгеньевна   ")
            )
        )

        val drafts = StudentFileParser.parseJournalXlsx(bytes)

        assertEquals(1, drafts.size)
        assertEquals(StudentDraft("Смирнова", "Алина"), drafts[0])
    }

    @Test
    fun templateParsesSeparatedColumnsAndSkipsOtchestvo() {
        val bytes = buildTemplateXlsx(
            header = listOf("A1" to "№", "B1" to "Фамилия", "C1" to "Имя", "D1" to "Отчество"),
            dataRows = listOf(
                listOf("A2" to "1", "B2" to "Иванова", "C2" to "Анна", "D2" to "Сергеевна"),
                listOf("A3" to "2", "B3" to "Петров", "C3" to "Пётр"),
                listOf("A4" to "3", "B4" to "Сидоров", "C4" to "Олег", "D4" to "Иванович")
            )
        )

        val drafts = StudentFileParser.parseTemplateXlsx(bytes)

        assertEquals(3, drafts.size)
        assertEquals(StudentDraft("Иванова", "Анна"), drafts[0])
        assertEquals(StudentDraft("Петров", "Пётр"), drafts[1])
        assertEquals(StudentDraft("Сидоров", "Олег"), drafts[2])
    }

    @Test
    fun templateFullNameInOneCellStillSplit() {
        val bytes = buildTemplateXlsx(
            header = listOf("B1" to "Фамилия"),
            dataRows = listOf(
                listOf("B2" to "Ковалёва Елена")
            )
        )

        val drafts = StudentFileParser.parseTemplateXlsx(bytes)

        assertEquals(listOf(StudentDraft("Ковалёва", "Елена")), drafts)
    }

    @Test
    fun generatedTemplateIsReadableAndEmptyWithoutData() {
        val bytes = XlsxTemplateWriter.createTemplate()

        // Шаблон с только что созданным файлом: заголовки есть, данных пока нет.
        assertTrue(bytes.isNotEmpty())
        val drafts = StudentFileParser.parseTemplateXlsx(bytes)
        assertTrue(drafts.isEmpty())
    }

    @Test
    fun capitalizationAppliedToImportedNames() {
        val bytes = buildJournalXlsx(
            rows = listOf(
                listOf("B1" to "Обучающийся"),
                listOf("A4" to "1", "B4" to "иванов иван иванович")
            )
        )

        val drafts = StudentFileParser.parseJournalXlsx(bytes)

        assertEquals(StudentDraft("Иванов", "Иван"), drafts[0])
    }

    @Test
    fun journalSplitsFullNameWithUnicodeSpaces() {
        // Реальные экспорты журналов используют неразрывные и узкие пробелы
        // (U+00A0, U+202F), которые не покрываются символом `\s`.
        val nbsp = "\u00A0"
        val narrow = "\u202F"
        val bytes = buildJournalXlsx(
            rows = listOf(
                listOf("B1" to "Обучающийся"),
                listOf("A4" to "1", "B4" to "Садаев${nbsp}Мухамад-Али${nbsp}Исаевич"),
                listOf("A5" to "2", "B5" to "Иванова${narrow}Анна${narrow}Сергеевна$nbsp"),
                listOf("A6" to "3", "B6" to "Петров Пётр Петрович\u200B")
            )
        )

        val drafts = StudentFileParser.parseJournalXlsx(bytes)

        assertEquals(StudentDraft("Садаев", "Мухамад-Али"), drafts[0])
        assertEquals(StudentDraft("Иванова", "Анна"), drafts[1])
        assertEquals(StudentDraft("Петров", "Пётр"), drafts[2])
    }

    @Test
    fun templateParsesCellsWithUnicodeSpaces() {
        val nbsp = "\u00A0"
        val bytes = buildTemplateXlsx(
            header = listOf("A1" to "№", "B1" to "Фамилия", "C1" to "Имя"),
            dataRows = listOf(
                listOf("A2" to "1", "B2" to "Смирнов", "C2" to "Олег"),
                listOf("A3" to "2", "B3" to "Ковалёва", "C3" to "Елена$nbsp")
            )
        )

        val drafts = StudentFileParser.parseTemplateXlsx(bytes)

        assertEquals(listOf(StudentDraft("Смирнов", "Олег"), StudentDraft("Ковалёва", "Елена")), drafts)
    }

    @Test
    fun templateContainsInstructionsAndNoOtchestvo() {
        val bytes = XlsxTemplateWriter.createTemplate()

        val workbookXml = readZipEntry(bytes, "xl/workbook.xml")
        assertTrue(workbookXml.contains("Ученики"))
        assertTrue(!workbookXml.contains("Инструкция"))
        assertTrue(readZipEntry(bytes, "xl/worksheets/sheet2.xml").isEmpty())

        val studentsSheet = readZipEntry(bytes, "xl/worksheets/sheet1.xml")
        assertTrue(!studentsSheet.contains("Отчество"))
        assertTrue(studentsSheet.contains("Фамилия ученика"))
        assertTrue(studentsSheet.contains("Имя ученика"))
        assertTrue(studentsSheet.contains("Как заполнить:"))

        // Памятка в колонке A не должна попадать в список учеников.
        val drafts = StudentFileParser.parseTemplateXlsx(bytes)
        assertTrue(drafts.isEmpty())
    }

    /** Собирает журналоподобный xlsx (общие строки, как в реальном экспорте). */
    private fun buildJournalXlsx(rows: List<List<Pair<String, String>>>): ByteArray =
        buildXlsx(rows)

    /** Собирает xlsx с обычной шапкой и строками данных. */
    private fun buildTemplateXlsx(
        header: List<Pair<String, String>>,
        dataRows: List<List<Pair<String, String>>>
    ): ByteArray = buildXlsx(listOf(header) + dataRows)

    private fun buildXlsx(rows: List<List<Pair<String, String>>>): ByteArray {
        val shared = buildSharedStrings(rows.flatMap { it.map { p -> p.second } })
        val sheet = buildSheet(rows, shared.indicesByText)

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            write(zip, "[Content_Types].xml", CONTENT_TYPES)
            write(zip, "_rels/.rels", ROOT_RELS)
            write(zip, "xl/workbook.xml", WORKBOOK)
            write(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            write(zip, "xl/sharedStrings.xml", shared.xml)
            write(zip, "xl/worksheets/sheet1.xml", sheet)
        }
        return out.toByteArray()
    }

    private class SharedStrings(
        val xml: String,
        val indicesByText: Map<String, Int>
    )

    private fun buildSharedStrings(texts: List<String>): SharedStrings {
        val unique = texts.distinct()
        val index = unique.withIndex().associate { it.value to it.index }
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<sst count=\"${texts.size}\" uniqueCount=\"${unique.size}\" " +
            "xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        unique.forEach { text ->
            sb.append("<si><t xml:space=\"preserve\">${escape(text)}</t></si>")
        }
        sb.append("</sst>")
        return SharedStrings(sb.toString(), index)
    }

    private fun buildSheet(rows: List<List<Pair<String, String>>>, index: Map<String, Int>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetData>")
        rows.forEachIndexed { i, cells ->
            val rowNum = cells.firstOrNull()?.first
                ?.takeLastWhile { it.isDigit() }
                ?.toIntOrNull()
                ?: (i + 1)
            sb.append("<row r=\"$rowNum\">")
            cells.forEach { (ref, text) ->
                sb.append("<c r=\"$ref\" t=\"s\"><v>${index.getValue(text)}</v></c>")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun write(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun readZipEntry(bytes: ByteArray, name: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) return zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ""
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private companion object {
        val CONTENT_TYPES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>"

        val ROOT_RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>"

        val WORKBOOK =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Лист1\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                "</workbook>"

        val WORKBOOK_RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>"
    }
}