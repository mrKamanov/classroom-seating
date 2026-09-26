package com.example.classroomseating.core.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Сжатие данных рассадки для QR: старый путь JSON -> GZIP -> Base64 (для совместимости)
 * и байтовые хелперы для бинарного формата [QrBinaryCodec] (без Base64).
 */
object QrCodeCompressor {

    fun compressToQrString(jsonString: String): String {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(jsonString)
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun decompressFromQrString(qrString: String): String {
        require(qrString.isNotBlank()) { "Compressed data cannot be empty" }
        val bytes = Base64.getDecoder().decode(qrString)
        return GZIPInputStream(ByteArrayInputStream(bytes))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    fun gzipBytes(bytes: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { it.write(bytes) }
        return outputStream.toByteArray()
    }

    fun gunzipBytes(bytes: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }

    /**
     * Бинарные байты в Base64 для QR. ASCII-транспорт гарантирует потерь на
     * byte-mode кодировании ZXing (сырые байты 0..255 в String его ломают) и
     * читается любым сканером, включая сторонние.
     */
    fun toQrBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun fromQrBase64(text: String): ByteArray? = runCatching {
        Base64.getDecoder().decode(text.trim())
    }.getOrNull()

    /** GZIP-магия 0x1F 0x8B в начале служит детектором сжатых данных. */
    fun isGzipMagic(bytes: ByteArray): Boolean =
        bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0x1F && (bytes[1].toInt() and 0xFF) == 0x8B

    /** Байты -> строка из символов 0..255 (байтовый режим QR, ISO-8859-1). */
    fun bytesToQrString(bytes: ByteArray): String = buildString(bytes.size) {
        for (b in bytes) append((b.toInt() and 0xFF).toChar())
    }

    /** Обратное преобразование (0..255 в байты, символы вне диапазона — '?'). */
    fun qrStringToBytes(text: String): ByteArray = text.toByteArray(Charsets.ISO_8859_1)
}