package com.example.classroomseating.core.util

import com.example.classroomseating.core.xlsx.XlsxReader
import com.example.classroomseating.core.xlsx.XlsxRow
import java.io.ByteArrayInputStream

/**
 * Извлечение учеников из файлов Excel.
 *
 * Два формата:
 * - [parseJournalXlsx] — журнал «Моя школа»: список класса в колонке B,
 *   начиная с 4-й строки, ячейка вида «Фамилия Имя Отчество» (отчество отбрасывается).
 * - [parseTemplateXlsx] — шаблон приложения: заголовки «Фамилия»/«Имя»,
 *   далее по одной строке на ученика.
 */
object StudentFileParser {

    /**
     * Пробельные символы, включая неразрывные и узкие, которые встречаются в
     * выгрузках журналов (NBSP U+00A0, узкий U+202F, U+2000–U+200B и др.).
     * Java/Kotlin `\s` их не покрывает, поэтому разбиение на слова через `\s+`
     * склеивало «Фамилия Имя» в один токен и имя терялось.
     */
    private val whitespace = Regex("[\\s\\u00A0\\u1680\\u2000-\\u200B\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

    private fun String.normalized(): String = replace(whitespace, " ").trim()

    fun parseJournalXlsx(bytes: ByteArray): List<StudentDraft> =
        parse(bytes) { rows ->
            rows
                .filter { it.rowNumber >= 4 }
                .mapNotNull { it.cells[1]?.normalized()?.takeIf { cell -> cell.isNotEmpty() } }
                .mapNotNull { fromFullName(it) }
        }

    private fun String.isLastNameHeader() = startsWith("Фамилия", ignoreCase = true)

    private fun String.isFirstNameHeader() = startsWith("Имя", ignoreCase = true)

    fun parseTemplateXlsx(bytes: ByteArray): List<StudentDraft> =
        parse(bytes) { rows ->
            val header = rows
                .sortedBy { it.rowNumber }
                .firstOrNull { row -> row.cells.values.any { it.normalized().isLastNameHeader() } }
                ?: return emptyList()

            val lastNameCol = header.cells.entries
                .firstOrNull { it.value.normalized().isLastNameHeader() }?.key
                ?: return emptyList()
            val firstNameCol = header.cells.entries
                .firstOrNull { it.value.normalized().isFirstNameHeader() }?.key

            rows.filter { it.rowNumber > header.rowNumber }.mapNotNull { row ->
                val lastName = row.cells[lastNameCol]?.normalized().orEmpty()
                if (lastName.isEmpty()) return@mapNotNull null
                val firstName = firstNameCol?.let { row.cells[it]?.normalized() }.orEmpty()
                if (firstName.isNotEmpty()) {
                    StudentDraft(
                        lastName = StudentListParser.capitalize(lastName),
                        firstName = StudentListParser.capitalize(firstName)
                    )
                } else {
                    fromFullName(lastName)
                }
            }
        }

    /** Ученики «Фамилия Имя Отчество»: нужны первые два слова, отчество не берём. */
    private fun fromFullName(cell: String): StudentDraft? {
        val tokens = cell.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val lastName = StudentListParser.capitalize(tokens[0])
        val firstName = if (tokens.size >= 2) StudentListParser.capitalize(tokens[1]) else ""
        return StudentDraft(lastName = lastName, firstName = firstName)
    }

    private inline fun parse(
        bytes: ByteArray,
        extract: (List<XlsxRow>) -> List<StudentDraft>
    ): List<StudentDraft> {
        val rows = runCatching { XlsxReader.readFirstSheet(ByteArrayInputStream(bytes)) }.getOrNull()
            ?: return emptyList()
        return extract(rows)
            .distinctBy { it.lastName to it.firstName }
    }
}