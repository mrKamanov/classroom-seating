package com.example.classroomseating.feature.export.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classroomseating.core.export.FileExporter
import com.example.classroomseating.core.export.QR_MAX_STORAGE_CHARS
import com.example.classroomseating.core.export.QrBinaryCodec
import com.example.classroomseating.core.export.QrCodeCompressor
import com.example.classroomseating.core.export.QrCodeGenerator
import com.example.classroomseating.core.export.SeatingPlanExportDto
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.SchoolClass
import com.example.classroomseating.domain.model.SeatingAssignment
import com.example.classroomseating.domain.repository.ClassroomRepository
import com.example.classroomseating.domain.repository.SchoolClassRepository
import com.example.classroomseating.domain.repository.SeatingPlanRepository
import com.example.classroomseating.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Экспорт рассадки: подготовка DTO, QR-кода и файла `.seating` для обмена (4.1, 4.2, 4.4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExportImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seatingPlanRepository: SeatingPlanRepository,
    private val classroomRepository: ClassroomRepository,
    private val studentRepository: StudentRepository,
    schoolClassRepository: SchoolClassRepository,
    private val fileExporter: FileExporter,
    private val pdfPrinter: SeatingPlanPdfPrinter
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle["classId"])

    data class ShareExportUiState(
        val loading: Boolean = true,
        val className: String = "",
        val studentCount: Int = 0,
        val deskCount: Int = 0,
        val dto: SeatingPlanExportDto? = null,
        val qrContent: String? = null,
        val qrError: String? = null,
        val error: String? = null
    ) {
        val ready: Boolean get() = dto != null
    }

    val uiState: StateFlow<ShareExportUiState> =
        seatingPlanRepository.observeByClass(classId)
            .map { plans -> plans.firstOrNull() }
            .flatMapLatest { plan ->
                if (plan == null) {
                    flowOf(ShareExportUiState(loading = false, error = "Нет данных для экспорта"))
                } else {
                    combine(
                        schoolClassRepository.observeById(classId),
                        studentRepository.observeByClass(classId),
                        classroomRepository.observeById(plan.classroomId),
                        classroomRepository.observeDesks(plan.classroomId),
                        seatingPlanRepository.observeAssignments(plan.id)
                    ) { schoolClass, students, classroom, desks, assignments ->
                        withContext(Dispatchers.Default) {
                            buildState(schoolClass, classroom, students, desks, assignments)
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ShareExportUiState()
            )

    val qrBitmap: StateFlow<Bitmap?> = uiState
        .map { it.qrContent }
        .distinctUntilChanged()
        .flatMapLatest { content ->
            if (content == null) flowOf<Bitmap?>(null)
            else flow<Bitmap?> { emit(QrCodeGenerator.generateQrBitmap(content)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** Выбранная ориентация листа для печати в PDF. */
    private val orientation = MutableStateFlow(PdfOrientation.PORTRAIT)
    val orientationState: StateFlow<PdfOrientation> = orientation.asStateFlow()

    fun setOrientation(value: PdfOrientation) {
        orientation.value = value
    }

    fun onShareFileClick() {
        val dto = uiState.value.dto ?: return
        viewModelScope.launch { fileExporter.shareSeatingPlan(dto) }
    }

    /** Отправка QR-кода как картинки (скриншот для чата/галереи) через системный share-диалог. */
    fun onShareQrClick(activity: Activity) {
        val bitmap = qrBitmap.value ?: return
        val className = uiState.value.className
        runCatching {
            val safeName = className
                .replace(Regex("[^A-Za-zА-Яа-яЁё0-9 ]"), "")
                .trim()
                .replace(" ", "_")
                .ifBlank { "class" }
            val cacheFile = File(activity.cacheDir, "Seating_QR_$safeName.png")
            cacheFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri: Uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                cacheFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Рассадка класса $className")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Поделиться QR-кодом")
            activity.startActivity(chooser)
        }
    }

    fun onPrintClick(activity: Activity) {
        val dto = uiState.value.dto ?: return
        pdfPrinter.print(activity, dto, qrBitmap.value, orientation.value)
    }

    private suspend fun buildState(
        schoolClass: SchoolClass?,
        classroom: com.example.classroomseating.domain.model.Classroom?,
        students: List<com.example.classroomseating.domain.model.Student>,
        desks: List<Desk>,
        assignments: List<SeatingAssignment>
    ): ShareExportUiState {
        if (schoolClass == null || classroom == null || desks.isEmpty()) {
            return ShareExportUiState(
                loading = false,
                className = schoolClass?.name ?: "",
                error = "Нет данных для экспорта"
            )
        }
        val dto = buildDto(schoolClass, classroom, desks, students, assignments)
        val encoded = runCatching { QrBinaryCodec.encode(dto) }.getOrNull()
        val qrContent = encoded
            ?.let { QrCodeCompressor.toQrBase64(it) }
            ?.takeIf { it.length <= QR_MAX_STORAGE_CHARS }
        val qrError = when {
            encoded == null -> "Не удалось подготовить данные для QR-кода"
            qrContent == null -> "Рассадка слишком большая, QR-код не помещается. Используйте файл .seating"
            else -> null
        }
        return ShareExportUiState(
            loading = false,
            className = schoolClass.name,
            studentCount = students.size,
            deskCount = desks.size,
            dto = dto,
            qrContent = qrContent,
            qrError = qrError
        )
    }

    private fun buildDto(
        schoolClass: SchoolClass,
        classroom: com.example.classroomseating.domain.model.Classroom,
        desks: List<Desk>,
        students: List<com.example.classroomseating.domain.model.Student>,
        assignments: List<SeatingAssignment>
    ): SeatingPlanExportDto {
        val desksDto = desks.map { desk ->
            SeatingPlanExportDto.DeskDto(desk.id, desk.gridX, desk.gridY, desk.capacity)
        }
        val studentsDto = students.map { student ->
            SeatingPlanExportDto.StudentDto(
                id = student.id,
                firstName = student.firstName,
                lastName = student.lastName,
                gender = student.gender.name,
                visionConstraint = student.restriction == com.example.classroomseating.domain.model.SeatingRestriction.VISION,
                seatingRestriction = student.restriction.name,
                behaviorNote = student.behaviorNote
            )
        }
        val assignmentsDto = assignments.map { assignment ->
            SeatingPlanExportDto.AssignmentDto(
                studentId = assignment.studentId,
                deskId = assignment.deskId,
                seatIndex = assignment.seatIndex
            )
        }
        return SeatingPlanExportDto(
            schoolClass = SeatingPlanExportDto.SchoolClassDto(
                name = schoolClass.name,
                academicYear = schoolClass.academicYear,
                classTeacherName = schoolClass.classTeacherName
            ),
            classroom = SeatingPlanExportDto.ClassroomDto(
                name = classroom.name,
                columnsCount = classroom.columnsCount,
                rowsCount = classroom.rowsCount,
                boardPosition = classroom.boardPosition.name
            ),
            desks = desksDto,
            students = studentsDto,
            assignments = assignmentsDto
        )
    }
}