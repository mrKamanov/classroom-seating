package com.example.classroomseating.core.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Декодирование QR-кода из растрового изображения (скриншот/картинка из чата).
 *
 * Сначала пробуем ML Kit — он устойчив к перспективе, шуму и бликам (фото экрана);
 * если не распознал — ZXing с фолбэком [GlobalHistogramBinarizer].
 *
 * Изображение масштабируется до [MAX_DECODE_DIMENSION]: полный разрешённый скриншот
 * (или фото камеры) свалить ML Kit/ZXing по памяти и времени, а для распознавания
 * достаточно ~2-4 px на модуль. Возвращает ту же ASCII/Latin-1 строку, что и камерный
 * скан: бинарный формат передаётся Base64, поэтому rawBytes и text совпадают.
 */
object QrImageDecoder {

    /** Максимальная длинная сторона кадра, отдаваемая декодерам (для памяти и скорости). */
    private const val MAX_DECODE_DIMENSION = 1400

    /**
     * Декодирует QR прямо из байтов файла (PNG/JPEG/WebP), не раздувая память:
     * сначала выясняем размер через bounds, затем декодируем с inSampleSize.
     */
    fun decode(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        try {
            return decode(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun decode(bitmap: Bitmap): String? {
        // ML Kit отказывается/падает на картинках больше ~1280x1280 — уменьшаем заранее.
        val scaled = scaleDown(bitmap) ?: return null
        try {
            mlKitDecode(scaled)?.let { return it }
            return zxingDecode(scaled)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun mlKitDecode(bitmap: Bitmap): String? {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAllPotentialBarcodes()
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val result = runCatching {
            Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
        }.getOrNull() ?: return null
        val barcode = result.firstOrNull { it.format == Barcode.FORMAT_QR_CODE } ?: return null
        return barcode.rawBytes?.let { QrCodeCompressor.bytesToQrString(it) } ?: barcode.rawValue
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_DECODE_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap? {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DECODE_DIMENSION) return bitmap
        val scale = MAX_DECODE_DIMENSION.toFloat() / longest
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun zxingDecode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return decodeFromPixels(pixels, width, height)
    }

    /** Чистая JVM-часть: принимает ARGB-пиксели (как из [Bitmap.getPixels]). */
    fun decodeFromPixels(pixels: IntArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null

        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        val reader = QRCodeReader()

        val result = try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
        } catch (e: Exception) {
            // Канонический фолбэк ZXing: GlobalHistogram надёжнее для больших полей с маленьким кодом.
            try {
                reader.reset()
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints)
            } catch (e2: Exception) {
                null
            }
        } ?: return null

        // ВАЖНО: берём result.text, а не rawBytes. Для контента с сегментами
        // (Base64) ZXing собирает rawBytes не полностью (теряя сегменты), тогда
        // как text всегда корректен. Наш контент ASCII/Latin-1, поэтому text
        // в точности равен переданной строке.
        return result.text
    }
}