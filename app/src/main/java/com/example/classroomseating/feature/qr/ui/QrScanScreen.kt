package com.example.classroomseating.feature.qr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.classroomseating.core.export.QrCodeCompressor
import com.example.classroomseating.core.export.QrImageDecoder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat as ZxingBarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ML Kit (barhopper) надёжно декодирует только компактные QR (примерно до version 20).
 * Плотные коды рассадки (version 25+) он обнаруживает, но не может разобрать —
 * поэтому декодируем ZXing'ом (он же генерирует код), а ML Kit оставляем для авто-зума.
 */
private fun decodeQrWithZxing(imageProxy: ImageProxy): String? {
    val plane = imageProxy.planes.getOrNull(0) ?: return null
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val buffer = plane.buffer
    if (buffer.remaining() < rowStride * (height - 1) + width) return null

    val luminance = ByteArray(width * height)
    if (pixelStride == 1) {
        for (y in 0 until height) {
            buffer.position(y * rowStride)
            buffer.get(luminance, y * width, width)
        }
    } else {
        for (y in 0 until height) {
            for (x in 0 until width) {
                luminance[y * width + x] = buffer.get(y * rowStride + x * pixelStride)
            }
        }
    }

    val source = PlanarYUVLuminanceSource(
        luminance, width, height,
        0, 0, width, height,
        false
    )
    val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.POSSIBLE_FORMATS to listOf(ZxingBarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )
    val result = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints)
    // ВАЖНО: берём text, а не rawBytes: для Base64-контента с сегментами ZXing
    // собирает rawBytes не полностью, тогда как text всегда корректен.
    return result.text
}

/**
 * Сканирование QR-кода рассадки: CameraX Preview + ML Kit Barcode Scanning (4.2).
 *
 * Плотные QR (крупная рассадка) требуют высокого разрешения кадра и авто-зума:
 * - targetResolution 1280x720 (дефолт 640x480 такие коды не читает);
 * - enableAllPotentialBarcodes() + setZoomSuggestionOptions() — ML Kit сам
 *   подтаскивает к камеру, пока код не распознается;
 * - анализ на фоновом потоке, чтобы не ронять fps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val scope = rememberCoroutineScope()
    var imageError by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        imageError = null
        scope.launch {
            val text = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    QrImageDecoder.decode(bytes)
                }.getOrNull()
            }
            if (text != null) onResult(text) else imageError = "QR-код на изображении не распознан"
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканировать QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Filled.Image, contentDescription = "Открыть QR из изображения")
                    }
                }
            )
        }
    ) { padding ->
        if (!permissionGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет доступа к камере", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val mainExecutor = ContextCompat.getMainExecutor(ctx)
                        val analysisExecutor = Executors.newSingleThreadExecutor()
                        var cameraControl: CameraControl? = null

                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .enableAllPotentialBarcodes()
                            .setZoomSuggestionOptions(
                                ZoomSuggestionOptions.Builder(
                                    object : ZoomSuggestionOptions.ZoomCallback {
                                        override fun setZoom(zoomRatio: Float): Boolean {
                                            val control = cameraControl ?: return false
                                            return runCatching {
                                                control.setZoomRatio(zoomRatio)
                                                true
                                            }.getOrDefault(false)
                                        }
                                    }
                                ).setMaxSupportedZoomRatio(4.0f).build()
                            )
                            .build()
                        val scanner = BarcodeScanning.getClient(options)
                        var handled = false

                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener(
                            {
                                val cameraProvider = runCatching { providerFuture.get() }.getOrNull()
                                    ?: return@addListener
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetResolution(Size(1280, 720))
                                    .build()
                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    val zxingRaw = runCatching { decodeQrWithZxing(imageProxy) }.getOrNull()
                                    if (zxingRaw != null) {
                                        if (!handled) {
                                            handled = true
                                            onResult(zxingRaw)
                                        }
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val cameraImage = imageProxy.image
                                    if (cameraImage == null) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    val inputImage = InputImage.fromMediaImage(
                                        cameraImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            if (!handled) {
                                                val barcode = barcodes.firstOrNull {
                                                    it.format == Barcode.FORMAT_QR_CODE
                                                }
                                                val raw = barcode?.rawBytes
                                                    ?.let { QrCodeCompressor.bytesToQrString(it) }
                                                    ?: barcode?.rawValue
                                                if (raw != null) {
                                                    handled = true
                                                    onResult(raw)
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                }
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                                cameraControl = camera.cameraControl
                            },
                            mainExecutor
                        )
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = imageError ?: "Наведите камеру на QR-код схемы",
                        color = if (imageError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}