package com.example.classroomseating.core.export

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Генерация Bitmap QR-кода на базе ZXing (4.2).
 * Содержимое — строка из символов 0..255 (бинарный формат [QrBinaryCodec]):
 * кодируется в байтовом режиме без потерь, поэтому charset ISO-8859-1.
 */
object QrCodeGenerator {

    suspend fun generateQrBitmap(
        content: String,
        sizePx: Int = 800
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (content.isEmpty()) return@withContext null
        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "ISO-8859-1",
                EncodeHintType.MARGIN to 4,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }
}