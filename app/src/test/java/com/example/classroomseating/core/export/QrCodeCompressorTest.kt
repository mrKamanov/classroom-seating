package com.example.classroomseating.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.zip.ZipException

class QrCodeCompressorTest {

    @Test
    fun roundTripRestoresOriginalString() {
        val json = """{"version":1,"schoolClass":{"name":"5 А","academicYear":"2026-2027"}}"""
        val compressed = QrCodeCompressor.compressToQrString(json)
        assertEquals(json, QrCodeCompressor.decompressFromQrString(compressed))
    }

    @Test
    fun compressHandlesCyrillicAndUnicode() {
        val json = """{"className":"5-А (Кабинет № 304)","students":["Иванов Иван","Петров Пётр"]}"""
        val compressed = QrCodeCompressor.compressToQrString(json)
        val decompressed = QrCodeCompressor.decompressFromQrString(compressed)
        assertTrue(decompressed.contains("5-А"))
        assertTrue(decompressed.contains("Иванов Иван"))
        assertTrue(decompressed.contains("Петров Пётр"))
    }

    @Test
    fun compressReducesSizeForRepetitivePayload() {
        val students = (1..30).joinToString("\n") { "Ученик_$it Группа" }
        val json = """{"students":["${students.replace("\n", "\",\n\"")}"]}"""
        val compressed = QrCodeCompressor.compressToQrString(json)
        assertTrue(compressed.let { it.toByteArray(Charsets.UTF_8).size } < json.toByteArray(Charsets.UTF_8).size)
    }

    @Test
    fun decompressBlankThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            QrCodeCompressor.decompressFromQrString("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QrCodeCompressor.decompressFromQrString("   ")
        }
    }

    @Test
    fun decompressCorruptedBase64Throws() {
        assertThrows(IllegalArgumentException::class.java) {
            QrCodeCompressor.decompressFromQrString("!!!NotABase64String!!!")
        }
    }

    @Test
    fun decompressNonGzipContentThrows() {
        val plain = Base64.getEncoder().encodeToString("Just plain text".toByteArray())
        assertThrows(ZipException::class.java) {
            QrCodeCompressor.decompressFromQrString(plain)
        }
    }
}