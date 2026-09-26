package com.example.classroomseating.feature.imports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.data.imports.ImportMode
import com.example.classroomseating.feature.imports.ImportViewModel

/**
 * Оверлей импорта: Snackbar + диалог подтверждения / разрешения конфликтов (4.3).
 */
@Composable
fun ImportUiHost(importViewModel: ImportViewModel) {
    val state by importViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            importViewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    state.pending?.let { pending ->
        if (state.hasConflict) {
            ImportConflictDialog(
                className = pending.dto.schoolClass.name,
                studentsCount = pending.dto.students.size,
                onCopy = { importViewModel.confirm(ImportMode.CREATE_COPY) },
                onReplace = { importViewModel.confirm(ImportMode.REPLACE) },
                onDismiss = importViewModel::dismiss
            )
        } else {
            AlertDialog(
                onDismissRequest = importViewModel::dismiss,
                title = { Text("Импортировать рассадку?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Класс: ${pending.dto.schoolClass.name}")
                        Text("Учеников: ${pending.dto.students.size}")
                        Text("Парт: ${pending.dto.desks.size}")
                    }
                },
                confirmButton = {
                    Button(onClick = { importViewModel.confirm(ImportMode.CREATE_COPY) }) {
                        Text("Импортировать")
                    }
                },
                dismissButton = {
                    TextButton(onClick = importViewModel::dismiss) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
private fun ImportConflictDialog(
    className: String,
    studentsCount: Int,
    onCopy: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Класс «$className» уже существует") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Что сделать с входящей рассадкой ($studentsCount уч.)?\n" +
                        "«Заменить» — перезапишет существующий класс, " +
                        "«Создать копию» — добавит рядом с пометкой «(Импортировано)»."
                )
            }
        },
        confirmButton = {
            Button(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                Text("Создать копию")
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onReplace, modifier = Modifier.fillMaxWidth()) {
                    Text("Заменить существующий", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Отмена")
                }
            }
        }
    )
}