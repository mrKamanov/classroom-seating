package com.example.classroomseating.core.xlsx

import android.util.Xml
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

/**
 * Строка листа Excel: [rowNumber] — номер строки (с 1), [cells] — ячейки
 * по индексу колонки (0 = «A», 1 = «B», …). Пустые ячейки не хранятся.
 */
data class XlsxRow(val rowNumber: Int, val cells: Map<Int, String>)

/**
 * Минимальный читатель книг Excel (.xlsx) без внешних зависимостей.
 *
 * Файл xlsx — это zip-архив; строки собраны в `xl/worksheets/sheet*.xml`,
 * тексты — в `xl/sharedStrings.xml`. Поддерживаются общие строки (`t="s"`),
 * инлайновые строки (`t="inlineStr"`) и числа.
 */
object XlsxReader {

    /** Читает первый лист (с наименьшим номером) и возвращает его строки. */
    fun readFirstSheet(input: InputStream): List<XlsxRow> {
        var sharedBytes: ByteArray? = null
        val worksheets = mutableListOf<Pair<String, ByteArray>>()

        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.trimStart('/')
                    when {
                        name == "xl/sharedStrings.xml" -> sharedBytes = zip.readBytes()
                        name.matches(Regex("^xl/worksheets/sheet\\d+\\.xml$")) ->
                            worksheets.add(name to zip.readBytes())
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheet = worksheets
            .minByOrNull { it.first.substringAfterLast("sheet").substringBefore(".xml").toIntOrNull() ?: Int.MAX_VALUE }
            ?: return emptyList()

        val shared = sharedBytes?.let { parseSharedStrings(it) } ?: emptyList()
        return parseSheet(sheet.second, shared)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), "UTF-8")

        val result = mutableListOf<String>()
        var inSi = false
        var inT = false
        val buffer = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> {
                        buffer.setLength(0)
                        inSi = true
                    }
                    "t" -> inT = inSi
                }
                XmlPullParser.TEXT -> if (inT) buffer.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> {
                        result.add(buffer.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<XlsxRow> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), "UTF-8")

        val rows = mutableListOf<XlsxRow>()
        var rowNumber = -1
        var cells = linkedMapOf<Int, String>()
        var cellRef = ""
        var cellType = ""
        var inT = false
        var inV = false
        val buffer = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        cells = linkedMapOf()
                        rowNumber = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: (rowNumber + 1)
                    }
                    "c" -> {
                        cellRef = parser.getAttributeValue(null, "r") ?: ""
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        buffer.setLength(0)
                    }
                    "t" -> if (cellType == "inlineStr" || cellType == "str") inT = true
                    "v" -> if (cellType != "inlineStr") inV = true
                }
                XmlPullParser.TEXT -> if (inT || inV) buffer.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "v" -> inV = false
                    "c" -> {
                        var text = buffer.toString()
                        if (cellType == "s") {
                            text = text.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                        }
                        val column = columnIndexOf(cellRef)
                        text = text.trim()
                        if (column >= 0 && text.isNotEmpty()) cells[column] = text
                    }
                    "row" -> if (cells.isNotEmpty()) rows.add(XlsxRow(rowNumber, cells))
                }
            }
            event = parser.next()
        }
        return rows
    }

    /** «B4» -> 1 (0-индекс колонки), «A1» -> 0. */
    private fun columnIndexOf(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        if (letters.isEmpty()) return -1
        var index = 0
        for (ch in letters.uppercase()) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }
}