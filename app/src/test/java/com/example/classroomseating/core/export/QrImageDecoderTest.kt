package com.example.classroomseating.core.export

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QrImageDecoderTest {

    private val encoder = MultiFormatWriter()

    /** Рисует QR-матрицу на белое полотно с полями (как скриншот/фотография экрана). */
    private fun renderQr(
        content: String,
        matrixSize: Int = 800,
        marginPx: Int = 40
    ): Pair<IntArray, Int> {
        val matrix: BitMatrix = encoder.encode(
            content,
            BarcodeFormat.QR_CODE,
            matrixSize,
            matrixSize,
            mapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "ISO-8859-1",
                EncodeHintType.MARGIN to 4
            )
        )
        val canvasSize = matrixSize + marginPx * 2
        val pixels = IntArray(canvasSize * canvasSize)
        for (y in 0 until canvasSize) {
            for (x in 0 until canvasSize) {
                val mx = x - marginPx
                val my = y - marginPx
                val black = mx in 0 until matrix.width && my in 0 until matrix.height &&
                    matrix.get(mx, my)
                pixels[y * canvasSize + x] = if (black) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return pixels to canvasSize
    }

    private fun binaryContentThatFits(): String {
        val desks = (0 until 12).map { i ->
            SeatingPlanExportDto.DeskDto("desk-uuid-$i", i % 4, i / 4, 2)
        }
        val students = (0 until 30).map { i ->
            SeatingPlanExportDto.StudentDto(
                id = "student-uuid-$i-aaaaaaaaaaaaa",
                firstName = "Имя$i",
                lastName = "Фамилия$i",
                gender = "UNSPECIFIED"
            )
        }
        val dto = SeatingPlanExportDto(
            schoolClass = SeatingPlanExportDto.SchoolClassDto(
                name = "5 А",
                academicYear = "2026-2027",
                classTeacherName = null
            ),
            classroom = SeatingPlanExportDto.ClassroomDto(
                name = "Кабинет 304",
                columnsCount = 4,
                rowsCount = 3,
                boardPosition = "TOP"
            ),
            desks = desks,
            students = students,
            assignments = students.mapIndexed { i, s ->
                SeatingPlanExportDto.AssignmentDto(
                    studentId = s.id,
                    deskId = desks[i % desks.size].id,
                    seatIndex = i % 2
                )
            }
        )
        return QrCodeCompressor.toQrBase64(QrBinaryCodec.encode(dto))
    }

    @Test
    fun binaryRoundTripThroughBase64AndPixels() {
        val content = binaryContentThatFits()

        // Полный цикл: encode(DTO) -> Base64 -> QR-картинка -> чтение -> парсинг.
        val (pixels, size) = renderQr(content)
        val decoded = QrImageDecoder.decodeFromPixels(pixels, size, size)
        assertNotNull("Наш сгенерированный QR должен декодироваться из картинки", decoded)
        assertEquals(content, decoded)

        val dto = QrBinaryCodec.decodeFromQrText(decoded!!)
        assertNotNull("Отсканированное должно парситься в рассадку", dto)
        assertEquals("5 А", dto!!.schoolClass.name)
        assertEquals(30, dto.students.size)
    }

    @Test
    fun decodesLegacyBase64AndPlainTextQr() {
        val legacy = QrCodeCompressor.compressToQrString("legacy-plain-content")
        val (pixels, size) = renderQr(legacy)
        assertEquals(legacy, QrImageDecoder.decodeFromPixels(pixels, size, size))

        val plain = "https://example.com/class-seating"
        val (plainPixels, plainSize) = renderQr(plain)
        assertEquals(plain, QrImageDecoder.decodeFromPixels(plainPixels, plainSize, plainSize))
    }

    @Test
    fun decodesCompactSmallQrAfterChatCompression() {
        // Мелкий QR (типично после сжатия в мессенджере): маленькая матрица, немного полей.
        val content = binaryContentThatFits()
        val (pixels, size) = renderQr(content, matrixSize = 320, marginPx = 16)
        assertEquals(content, QrImageDecoder.decodeFromPixels(pixels, size, size))
    }

    @Test
    fun decodesLargeBinaryFitsQrBudget() {
        val content = binaryContentThatFits()
        assertNotNull("Даже Base64 контент должен помещаться в лимит QR", content)
        assert(content.length <= QR_MAX_STORAGE_CHARS)
    }
}