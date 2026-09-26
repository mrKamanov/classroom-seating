package com.example.classroomseating.core.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Генератор шаблона Excel (.xlsx) для ручного заполнения списка учеников.
 *
 * Один лист «Ученики»: колонка A — «№» (заранее проставлены 1..40),
 * B — «Фамилия ученика», C — «Имя ученика». Ниже таблицы — памятка по
 * заполнению в колонке A: она не мешает импорту, так как список читается
 * из колонок B/C. Название файла задаёт пользователь в диалоге сохранения.
 */
object XlsxTemplateWriter {

    private const val DATA_ROWS = 40

    fun createTemplate(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            writeEntry(zip, "_rels/.rels", ROOT_RELS)
            writeEntry(zip, "xl/workbook.xml", WORKBOOK)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            writeEntry(zip, "xl/worksheets/sheet1.xml", buildStudentsSheet())
        }
        return out.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildStudentsSheet(): String {
        val sb = StringBuilder()
        sb.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
                "<cols><col min=\"1\" max=\"1\" width=\"8\"/><col min=\"2\" max=\"2\" width=\"34\"/>" +
                "<col min=\"3\" max=\"3\" width=\"26\"/></cols>\n" +
                "<sheetData>\n"
        )
        sb.append(cellRow(1, "A1", "№"))
        sb.append(cellRow(1, "B1", "Фамилия ученика"))
        sb.append(cellRow(1, "C1", "Имя ученика"))
        for (n in 1..DATA_ROWS) {
            val row = n + 1
            sb.append(cellRow(row, "A$row", n.toString()))
        }
        sb.append(cellRow(DATA_ROWS + 3, "A${DATA_ROWS + 3}", "Как заполнить:"))
        sb.append(cellRow(DATA_ROWS + 4, "A${DATA_ROWS + 4}",
            "1. Фамилию и имя каждого ученика впишите в свою строку — колонки B и C."))
        sb.append(cellRow(DATA_ROWS + 5, "A${DATA_ROWS + 5}",
            "2. Номера в колонке «№» менять не нужно, пустые строки оставляйте."))
        sb.append(cellRow(DATA_ROWS + 6, "A${DATA_ROWS + 6}",
            "3. Сохраните файл и загрузите его в приложении: «Ученики» → «Загрузить из файла» → «Загрузить шаблон»."))
        sb.append("</sheetData>\n</worksheet>\n")
        return sb.toString()
    }

    private fun cellRow(row: Int, ref: String, text: String): String =
        "<row r=\"$row\"><c r=\"$ref\" t=\"inlineStr\"><is><t>${escape(text)}</t></is></c></row>\n"

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private val CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
            "</Types>\n"

    private val ROOT_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
            "</Relationships>\n"

    private val WORKBOOK =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
            "<sheets><sheet name=\"Ученики\" sheetId=\"1\" r:id=\"rId1\"/></sheets>\n" +
            "</workbook>\n"

    private val WORKBOOK_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
            "</Relationships>\n"
}