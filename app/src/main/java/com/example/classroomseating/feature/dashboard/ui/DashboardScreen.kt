package com.example.classroomseating.feature.dashboard.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.domain.model.SchoolClass
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onClassClick: (String) -> Unit,
    onScanQr: () -> Unit,
    onQrImagePicked: (Uri) -> Unit,
    onOpenInstructions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val classes by viewModel.classes.collectAsStateWithLifecycle()

    val qrImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onQrImagePicked) }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingClass by rememberSaveable(stateSaver = SchoolClassSaver) {
        mutableStateOf<SchoolClass?>(null)
    }
    var deletingClass by rememberSaveable(stateSaver = SchoolClassSaver) {
        mutableStateOf<SchoolClass?>(null)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Классная Рассадка") },
                actions = {
                    IconButton(onClick = onOpenInstructions) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Инструкция"
                        )
                    }
                    IconButton(onClick = onScanQr) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Сканировать QR"
                        )
                    }
                    IconButton(onClick = {
                        qrImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Загрузить QR из изображения"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Класс") }
            )
        }
    ) { padding ->
        if (classes.isEmpty()) {
            EmptyDashboardHint(
                modifier = Modifier.padding(padding),
                onScanQr = onScanQr,
                onQrImagePicked = {
                    qrImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenInstructions = onOpenInstructions
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classes, key = { it.schoolClass.id }) { item ->
                    ClassCard(
                        item = item,
                        onClick = { onClassClick(item.schoolClass.id) },
                        onEdit = { editingClass = item.schoolClass },
                        onDelete = { deletingClass = item.schoolClass }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        ClassDialog(
            title = "Новый класс",
            confirmText = "Создать",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, year, teacher ->
                val newId = viewModel.createClass(name, year, teacher)
                showCreateDialog = false
                onClassClick(newId)
            }
        )
    }

    editingClass?.let { schoolClass ->
        ClassDialog(
            title = "Редактировать класс",
            confirmText = "Сохранить",
            initialName = schoolClass.name,
            initialYear = schoolClass.academicYear,
            initialTeacher = schoolClass.classTeacherName,
            onDismiss = { editingClass = null },
            onConfirm = { name, year, teacher ->
                viewModel.updateClass(
                    schoolClass.copy(
                        name = name,
                        academicYear = year,
                        classTeacherName = teacher?.trim()?.ifBlank { null }
                    )
                )
                editingClass = null
            }
        )
    }

    deletingClass?.let { schoolClass ->
        AlertDialog(
            onDismissRequest = { deletingClass = null },
            title = { Text("Удалить класс?") },
            text = { Text("Класс «${schoolClass.name}» со списком учеников будет удалён без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteClass(schoolClass)
                    deletingClass = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingClass = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ClassCard(
    item: ClassListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.schoolClass.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${item.schoolClass.academicYear} · ${item.studentsCount} уч.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать класс")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить класс",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassDialog(
    title: String,
    confirmText: String,
    initialName: String = "",
    initialYear: String = currentAcademicYear(),
    initialTeacher: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, academicYear: String, teacher: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var year by rememberSaveable { mutableStateOf(initialYear) }
    var teacher by rememberSaveable { mutableStateOf(initialTeacher.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название класса") },
                    placeholder = { Text("5 \"А\"") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Учебный год") }
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Классный руководитель") },
                    placeholder = { Text("Иванова О. В.") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        name.trim(),
                        year.trim().ifBlank { currentAcademicYear() },
                        teacher.trim().ifBlank { null }
                    )
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun EmptyDashboardHint(
    modifier: Modifier = Modifier,
    onScanQr: () -> Unit,
    onQrImagePicked: () -> Unit,
    onOpenInstructions: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Пока нет классов",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Создайте первый класс или отсканируйте QR-код с готовой рассадкой.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onScanQr) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сканировать QR")
        }
        OutlinedButton(onClick = onQrImagePicked) {
            Icon(Icons.Filled.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Загрузить QR из картинки")
        }
        OutlinedButton(onClick = onOpenInstructions) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Инструкция")
        }
    }
}

private fun currentAcademicYear(): String {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    return "$year-${(year + 1) % 100}"
}

/** Saver для восстановления редактируемого класса после поворота экрана. */
private val SchoolClassSaver = listSaver<SchoolClass?, Any?>(
    save = { s ->
        s?.let { listOf(it.id, it.name, it.academicYear, it.classTeacherName) } ?: emptyList()
    },
    restore = { list ->
        if (list.isEmpty()) {
            null
        } else {
            SchoolClass(
                id = list[0] as String,
                name = list[1] as String,
                academicYear = list[2] as String,
                classTeacherName = list[3] as? String
            )
        }
    }
)