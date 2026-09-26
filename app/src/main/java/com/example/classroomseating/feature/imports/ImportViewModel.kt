package com.example.classroomseating.feature.imports

import android.content.Context
import android.net.Uri
import com.example.classroomseating.core.export.QrBinaryCodec
import com.example.classroomseating.core.export.QrCodeCompressor
import com.example.classroomseating.core.export.QrImageDecoder
import com.example.classroomseating.core.export.SeatingPlanExportDto
import com.example.classroomseating.data.imports.ImportMode
import com.example.classroomseating.data.imports.SeatingPlanImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Импорт рассадки: файл `.seating` или результат сканирования QR (4.1, 4.2).
 * Привязан к Activity: диалог конфликтов показывается поверх любого экрана.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val importService: SeatingPlanImportService
) : ViewModel() {

    data class PendingImport(val dto: SeatingPlanExportDto)

    data class UiState(
        val pending: PendingImport? = null,
        val hasConflict: Boolean = false,
        val message: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIncomingFileUri(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) return@launch
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                _state.update { it.copy(message = "Файл пуст или не может быть прочитан") }
                return@launch
            }
            parseAndStage(text)
        }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch { parseAndStage(text) }
    }

    /** Получено изображение с QR-кодом (галерея/фото пикер или входящий SEND). */
    fun onQrImageUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val text = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    QrImageDecoder.decode(bytes)
                }.getOrNull()
            }
            if (text == null) {
                _state.update { it.copy(message = "QR-код на изображении не найден") }
                return@launch
            }
            parseAndStage(text)
        }
    }

    fun dismiss() {
        _state.update { it.copy(pending = null, hasConflict = false) }
    }

    fun confirm(mode: ImportMode) {
        viewModelScope.launch {
            val dto = _state.value.pending?.dto ?: return@launch
            val result = runCatching { importService.import(dto, mode) }.getOrNull()
            _state.update {
                it.copy(
                    pending = null,
                    hasConflict = false,
                    message = result?.let { r -> "Импортировано: ${r.className}" }
                        ?: "Ошибка импорта"
                )
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun parseAndStage(text: String) {
        if (text.isBlank()) {
            _state.update { it.copy(message = "Файл пуст или не может быть прочитан") }
            return
        }
        val dto = parseDto(text)
        if (dto == null) {
            _state.update { it.copy(message = "Не удалось распознать рассадку") }
            return
        }
        val conflict = importService.existsClassWithName(dto.schoolClass.name)
        _state.update { it.copy(pending = PendingImport(dto), hasConflict = conflict) }
    }

    private fun parseDto(text: String): SeatingPlanExportDto? {
        QrBinaryCodec.decodeFromQrText(text)?.let { return it }
        return runCatching { json.decodeFromString<SeatingPlanExportDto>(text) }.getOrNull()
            ?: runCatching {
                json.decodeFromString<SeatingPlanExportDto>(QrCodeCompressor.decompressFromQrString(text))
            }.getOrNull()
    }
}