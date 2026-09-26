package com.example.classroomseating.feature.class_management.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.core.ui.dragdrop.restrictionColor
import com.example.classroomseating.core.util.StudentDraft
import com.example.classroomseating.core.util.XlsxTemplateWriter
import com.example.classroomseating.domain.model.Gender
import com.example.classroomseating.domain.model.SeatingRestriction
import com.example.classroomseating.domain.model.Student
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    classId: String,
    onOpenSeatingPlan: () -> Unit,
    onOpenLayoutEditor: () -> Unit,
    onOpenGroups: () -> Unit,
    onBack: () -> Unit,
    viewModel: StudentsViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsStateWithLifecycle()
    val className by viewModel.className.collectAsStateWithLifecycle()
    val filePreview by viewModel.filePreview.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editingStudent by rememberSaveable(stateSaver = StudentSaver) {
        mutableStateOf<Student?>(null)
    }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var deletingStudent by rememberSaveable(stateSaver = StudentSaver) {
        mutableStateOf<Student?>(null)
    }

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(XlsxTemplateWriter.createTemplate()) }
            }.isSuccess
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Шаблон сохранён. Заполните его и загрузите обратно."
                    else "Не удалось сохранить шаблон"
                )
            }
        }
    }

    val journalPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.onPickedXlsxFile(uri, journalFormat = true) }

    val templatePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.onPickedXlsxFile(uri, journalFormat = false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(className ?: "Ученики") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSeatingPlan) {
                        Icon(Icons.Default.EventSeat, contentDescription = "Рассадка")
                    }
                    IconButton(onClick = onOpenLayoutEditor) {
                        Icon(Icons.Default.Settings, contentDescription = "Настроить кабинет")
                    }
                    IconButton(onClick = onOpenGroups) {
                        Icon(Icons.Default.Groups, contentDescription = "Малые группы")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Загрузить из файла")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ученик") }
            )
        }
    ) { padding ->
        if (students.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Список пуст", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Добавьте учеников вручную или загрузите список из файла Excel (журнал \"Моя школа\" или шаблон).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    students,
                    key = { _, student -> student.id },
                    contentType = { _, _ -> "student" }
                ) { index, student ->
                    StudentRow(
                        student = student,
                        zebra = index % 2 == 1,
                        colors = colors,
                        onClick = { editingStudent = student },
                        onDelete = { deletingStudent = student }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        StudentEditDialog(
            title = "Новый ученик",
            onDismiss = { showCreateDialog = false },
            onSave = { student ->
                viewModel.saveStudent(student)
                showCreateDialog = false
            }
        )
    }

    editingStudent?.let { student ->
        StudentEditDialog(
            title = "Редактировать ученика",
            initial = student,
            onDismiss = { editingStudent = null },
            onSave = { updated ->
                viewModel.saveStudent(updated)
                editingStudent = null
            }
        )
    }

    if (showImportDialog) {
        StudentFileImportDialog(
            onDismiss = { showImportDialog = false },
            onDownloadTemplate = { templateLauncher.launch("Шаблон учеников.xlsx") },
            onLoadTemplate = {
                showImportDialog = false
                templatePickerLauncher.launch(arrayOf(MIME_XLSX, MIME_ALL))
            },
            onLoadJournal = {
                showImportDialog = false
                journalPickerLauncher.launch(arrayOf(MIME_XLSX, MIME_ALL))
            }
        )
    }

    filePreview?.let { preview ->
        FilePreviewDialog(
            drafts = preview.drafts,
            onDismiss = viewModel::dismissFilePreview,
            onImport = {
                viewModel.importDrafts(preview.drafts)
            }
        )
    }

    deletingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("Удалить ученика?") },
            text = { Text("${student.lastName} ${student.firstName} будет удалён из списка.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudent(student)
                    deletingStudent = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun StudentRow(
    student: Student,
    zebra: Boolean,
    colors: ColorScheme,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (zebra) colors.surfaceContainerLow else colors.surface
    val displayName = remember(student) { "${student.lastName} ${student.firstName}" }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeadingBadge(student = student, colors = colors)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                student.behaviorNote?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = colors.error
                )
            }
        }
    }
}

@Composable
private fun LeadingBadge(student: Student, colors: ColorScheme) {
    val restriction = student.restriction
    val neutralBg = colors.secondaryContainer
    val bg = remember(restriction, neutralBg) {
        if (restriction.hasRestriction) restrictionColor(restriction) else neutralBg
    }
    val label = remember(student) {
        if (restriction.hasRestriction) {
            restriction.badge
        } else {
            student.lastName.firstOrNull()?.uppercase().orEmpty()
        }
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (bg == neutralBg) colors.onSecondaryContainer else Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentEditDialog(
    title: String,
    initial: Student? = null,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var lastName by rememberSaveable { mutableStateOf(initial?.lastName.orEmpty()) }
    var firstName by rememberSaveable { mutableStateOf(initial?.firstName.orEmpty()) }
    var restriction by rememberSaveable {
        mutableStateOf(initial?.restriction ?: SeatingRestriction.NONE)
    }
    var note by rememberSaveable { mutableStateOf(initial?.behaviorNote.orEmpty()) }
    var gender by rememberSaveable { mutableStateOf(initial?.gender ?: Gender.UNSPECIFIED) }
    var restrictionExpanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = capitalizeName(it) },
                    label = { Text("Фамилия") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = capitalizeName(it) },
                    label = { Text("Имя") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = restrictionExpanded,
                    onExpandedChange = { restrictionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = restriction.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ограничение по здоровью") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = restrictionExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = restrictionExpanded,
                        onDismissRequest = { restrictionExpanded = false }
                    ) {
                        SeatingRestriction.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.title)
                                        Text(
                                            text = option.hint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    restriction = option
                                    restrictionExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Примечание (например: не сажать вместе)") },
                    minLines = 1,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = lastName.isNotBlank(),
                onClick = {
                    onSave(
                        Student(
                            id = initial?.id ?: "",
                            classId = initial?.classId ?: "",
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            gender = gender,
                            restriction = restriction,
                            behaviorNote = note.trim().ifBlank { null }
                        )
                    )
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun StudentFileImportDialog(
    onDismiss: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onLoadTemplate: () -> Unit,
    onLoadJournal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Загрузка учеников из файла") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Шаблон — самый простой способ:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onDownloadTemplate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("Скачать шаблон")
                }
                Button(
                    onClick = onLoadTemplate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Text("Загрузить шаблон")
                }
                Text(
                    "Скачайте файл, впишите в него фамилии и имена учеников и загрузите обратно.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Button(
                    onClick = onLoadJournal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Text("Загрузить из журнала «Моя школа»")
                }
                Text(
                    "Выберите выгруженный файл журнала — список класса добавится сам.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun FilePreviewDialog(
    drafts: List<StudentDraft>,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Найдено учеников: ${drafts.size}") },
        text = {
            if (drafts.isEmpty()) {
                Text("Учеников не найдено. Проверьте, что выбран именно файл Excel (.xlsx).")
            } else {
                Column {
                    Text(
                        "Проверьте список и добавьте учеников в класс:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .padding(top = 4.dp)
                    ) {
                        items(drafts, key = { "${it.lastName}_${it.firstName}" }) { draft ->
                            Text(
                                text = "${draft.lastName} ${draft.firstName}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (drafts.isNotEmpty()) {
                Button(onClick = onImport) { Text("Добавить") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (drafts.isNotEmpty()) "Отмена" else "Закрыть") }
        }
    )
}

/** Первая буква имени/фамилии — автоматически заглавная при ручном вводе. */
private const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val MIME_ALL = "*/*"
private fun capitalizeName(value: String): String {
    if (value.isEmpty()) return value
    val firstLetterIndex = value.indexOfFirst { it.isLetter() }
    if (firstLetterIndex < 0) return value
    return value.replaceRange(
        firstLetterIndex,
        firstLetterIndex + 1,
        value[firstLetterIndex].uppercase()
    )
}

/** Saver для восстановления редактируемого ученика после поворота экрана. */
private val StudentSaver = listSaver<Student?, Any?>(
    save = { s ->
        s?.let {
            listOf(
                it.id,
                it.classId,
                it.firstName,
                it.lastName,
                it.gender.name,
                it.restriction.name,
                it.behaviorNote,
                it.tagColorHex
            )
        } ?: emptyList()
    },
    restore = { list ->
        if (list.isEmpty()) {
            null
        } else {
            Student(
                id = list[0] as String,
                classId = list[1] as String,
                firstName = list[2] as String,
                lastName = list[3] as String,
                gender = runCatching {
                    Gender.valueOf(list[4] as String)
                }.getOrDefault(Gender.UNSPECIFIED),
                restriction = SeatingRestriction.fromStorage(list[5] as String),
                behaviorNote = list[6] as? String,
                tagColorHex = list[7] as? String
            )
        }
    }
)