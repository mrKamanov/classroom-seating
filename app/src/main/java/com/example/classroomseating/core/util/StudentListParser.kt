package com.example.classroomseating.core.util

/**
 * Черновик ученика, полученный при массовом вводе.
 */
data class StudentDraft(
    val lastName: String,
    val firstName: String,
    val behaviorNote: String? = null
)

/**
 * Разбор списка учеников из вставленного текста (Smart Paste).
 *
 * Поддерживаются строки вида:
 * - "Фамилия Имя"
 * - "Фамилия	Имя" (табуляция)
 * - "1. Фамилия Имя", "1) Фамилия Имя", "- Фамилия Имя"
 */
object StudentListParser {

    private val listPrefix = Regex("^\\s*(?:\\d+[.)-]|[-•*]+[.)-]?)\\s*")

    fun parse(input: String): List<StudentDraft> =
        input.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val cleaned = listPrefix.replace(line, "").trim()
                val tokens = cleaned.split(Regex("\\s+"))
                when {
                    tokens.isEmpty() || tokens[0].isBlank() -> null
                    tokens.size >= 2 ->
                        StudentDraft(lastName = capitalize(tokens[0]), firstName = capitalize(tokens[1]))
                    else -> StudentDraft(lastName = capitalize(tokens[0]), firstName = "")
                }
            }

    /** Первая буква слова — заглавная. Используется и при импорте из Excel. */
    internal fun capitalize(word: String): String =
        word.replaceFirstChar { it.uppercase() }
}