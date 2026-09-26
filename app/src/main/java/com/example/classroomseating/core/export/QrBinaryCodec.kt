package com.example.classroomseating.core.export

import java.io.ByteArrayOutputStream

/**
 * Компактный бинарный формат рассадки для QR-кодов.
 *
 * Вместо JSON+GZIP+Base64 (Base64 съедает ~25% ёмкости и загоняет код
 * в байтовый режим) храним сырые байты без оверхеда: поля без имён,
 * ссылки на учеников/парты — индексами вместо UUID, enum-поля — байтами.
 *
 * Итоговые байты = GZIP(payload), где payload начинается с байта-маркера
 * формата (0x01). Маркер отличает бинарный формат от legacy JSON+GZIP+Base64,
 * чей gzip также начинается с магии 0x1F 0x8B. В QR-строку payload переводится
 * через Base64 (ASCII-транспорт без потерь).
 *
 * Payload:
 *   класс     name str, academicYear str, classTeacherName str?
 *   кабинет   name str, columnsCount int, rowsCount int, boardPosition str
 *   парты     count int, {gridX int, gridY int, capacity int} — id по индексу "d$i"
 *   ученики   count int, {firstName str, lastName str, gender int, vision bool, restriction str?, note str?} — id "s$i"
 *   назначения count int, {studentIndex int, deskIndex int, seatIndex int} — только валидные
 */
object QrBinaryCodec {

    private const val FORMAT_MARKER = 0x01

    fun encode(dto: SeatingPlanExportDto): ByteArray {
        val writer = ByteWriter()
        writer.raw(FORMAT_MARKER.toByte())
        writer.str(dto.schoolClass.name)
        writer.str(dto.schoolClass.academicYear)
        writer.strOrNull(dto.schoolClass.classTeacherName)

        writer.str(dto.classroom.name.ifBlank { "Кабинет" })
        writer.int(dto.classroom.columnsCount)
        writer.int(dto.classroom.rowsCount)
        writer.str(dto.classroom.boardPosition)

        writer.int(dto.desks.size)
        dto.desks.forEach { desk ->
            writer.int(desk.gridX)
            writer.int(desk.gridY)
            writer.int(desk.capacity.coerceAtLeast(1))
        }

        writer.int(dto.students.size)
        dto.students.forEach { student ->
            writer.str(student.firstName)
            writer.str(student.lastName)
            writer.int(genderOrdinal(student.gender))
            writer.flag(student.visionConstraint)
            writer.strOrNull(student.seatingRestriction)
            writer.strOrNull(student.behaviorNote)
        }

        val studentIndexById = dto.students.mapIndexed { i, s -> s.id to i }.toMap()
        val deskIndexById = dto.desks.mapIndexed { i, d -> d.id to i }.toMap()
        val validAssignments = dto.assignments.filter { a ->
            a.studentId in studentIndexById && a.deskId in deskIndexById
        }
        writer.int(validAssignments.size)
        validAssignments.forEach { a ->
            writer.int(studentIndexById.getValue(a.studentId))
            writer.int(deskIndexById.getValue(a.deskId))
            writer.int(a.seatIndex)
        }

        return QrCodeCompressor.gzipBytes(writer.toByteArray())
    }

    fun decode(qrBytes: ByteArray): SeatingPlanExportDto {
        val reader = ByteReader(QrCodeCompressor.gunzipBytes(qrBytes))
        require(reader.ubyte() == FORMAT_MARKER) { "Unknown binary format marker" }

        val className = reader.str()
        val academicYear = reader.str()
        val classTeacherName = reader.strOrNull()

        val classroomName = reader.str()
        val columnsCount = reader.int()
        val rowsCount = reader.int()
        val boardPosition = reader.str()

        val deskCount = reader.int()
        val desks = (0 until deskCount).map { i ->
            SeatingPlanExportDto.DeskDto(
                id = "d$i",
                gridX = reader.int(),
                gridY = reader.int(),
                capacity = reader.int()
            )
        }

        val studentCount = reader.int()
        val students = (0 until studentCount).map { i ->
            SeatingPlanExportDto.StudentDto(
                id = "s$i",
                firstName = reader.str(),
                lastName = reader.str(),
                gender = genderName(reader.int()),
                visionConstraint = reader.flag(),
                seatingRestriction = reader.strOrNull(),
                behaviorNote = reader.strOrNull()
            )
        }

        val assignmentCount = reader.int()
        val assignments = (0 until assignmentCount).map {
            val studentIndex = reader.int()
            val deskIndex = reader.int()
            val seatIndex = reader.int()
            SeatingPlanExportDto.AssignmentDto(
                studentId = students.getOrNull(studentIndex)?.id ?: "s$studentIndex",
                deskId = desks.getOrNull(deskIndex)?.id ?: "d$deskIndex",
                seatIndex = seatIndex
            )
        }

        return SeatingPlanExportDto(
            schoolClass = SeatingPlanExportDto.SchoolClassDto(
                name = className,
                academicYear = academicYear,
                classTeacherName = classTeacherName
            ),
            classroom = SeatingPlanExportDto.ClassroomDto(
                name = classroomName,
                columnsCount = columnsCount,
                rowsCount = rowsCount,
                boardPosition = boardPosition
            ),
            desks = desks,
            students = students,
            assignments = assignments
        )
    }

    private fun genderOrdinal(value: String): Int = when (value) {
        "MALE" -> 1
        "FEMALE" -> 2
        else -> 0
    }

    private fun genderName(ordinal: Int): String = when (ordinal) {
        1 -> "MALE"
        2 -> "FEMALE"
        else -> "UNSPECIFIED"
    }

    /**
     * Парсинг QR-строки в бинарный формат. Поддерживает:
     * - Base64(gzip[marker + payload]) — текущий формат;
     * - старые QR без Base64 (символы 0..255 с магией GZIP) — обратная совместимость.
     * Возвращает null, если формат не бинарный.
     */
    fun decodeFromQrText(text: String): SeatingPlanExportDto? {
        QrCodeCompressor.fromQrBase64(text)?.let { bytes ->
            if (QrCodeCompressor.isGzipMagic(bytes)) {
                return runCatching { decode(bytes) }.getOrNull()
            }
            return null
        }
        // Старый способ: строка из символов 0..255 с магией GZIP в начале.
        if (text.length >= 2 && text[0].code == 0x1F && text[1].code == 0x8B) {
            val bytes = QrCodeCompressor.qrStringToBytes(text)
            return runCatching { decode(bytes) }.getOrNull()
        }
        return null
    }

    private class ByteWriter {
        private val buffer = ByteArrayOutputStream()

        fun raw(value: Byte) {
            buffer.write(value.toInt())
        }

        fun int(value: Int) {
            var v = value
            while (v >= 0x80) {
                buffer.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
            buffer.write(v)
        }

        fun flag(value: Boolean) {
            buffer.write(if (value) 1 else 0)
        }

        fun str(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            int(bytes.size)
            buffer.write(bytes)
        }

        fun strOrNull(value: String?) {
            if (value == null) {
                buffer.write(0)
            } else {
                buffer.write(1)
                str(value)
            }
        }

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }

    private class ByteReader(private val bytes: ByteArray, private var pos: Int = 0) {
        fun int(): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = ubyte()
                result = result or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
                require(shift <= 35) { "Varint overflow" }
            }
            require(result >= 0 && result < 1_000_000) { "Varint value out of range" }
            return result
        }

        fun flag(): Boolean = ubyte() == 1

        fun str(): String {
            val length = int()
            require(length >= 0 && pos + length <= bytes.size) { "String length out of bounds" }
            val value = String(bytes, pos, length, Charsets.UTF_8)
            pos += length
            return value
        }

        fun strOrNull(): String? = if (ubyte() == 0) null else str()

        fun ubyte(): Int {
            require(pos < bytes.size) { "Buffer underflow" }
            return bytes[pos++].toInt() and 0xFF
        }
    }
}