package com.example.classroomseating.feature.export.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Экран экспорта/обмена: QR-код, файл `.seating`, печать PDF (4.1, 4.2, 4.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareExportScreen(
    viewModel: ExportImportViewModel,
    onScanQr: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val qrBitmap by viewModel.qrBitmap.collectAsStateWithLifecycle()
    val selectedOrientation by viewModel.orientationState.collectAsStateWithLifecycle()
    val currentActivity = LocalContext.current as? Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обмен рассадкой") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Класс: ${state.className}", style = MaterialTheme.typography.titleLarge)
                    Text("Учеников: ${state.studentCount}  •  Парт: ${state.deskCount}")
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("QR-код схемы", style = MaterialTheme.typography.titleMedium)
                    state.qrError?.let { errorText ->
                        Text(
                            errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!state.loading && state.ready && state.qrError == null && qrBitmap == null) {
                        CircularProgressIndicator()
                    } else if (qrBitmap != null && state.qrError == null) {
                        qrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR-код рассадки",
                                modifier = Modifier
                                    .size(260.dp)
                                    .padding(8.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                    Text(
                        "Отсканируйте код в приложении «Классная Рассадка», чтобы импортировать схему.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onScanQr,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.ready
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Сканировать QR для импорта")
            }

            OutlinedButton(
                onClick = {
                    val activity = currentActivity
                    if (activity != null) viewModel.onShareQrClick(activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = qrBitmap != null && state.qrError == null
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Поделиться QR-картинкой")
            }

            Button(
                onClick = viewModel::onShareFileClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.ready
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Поделиться файлом .seating")
            }

            OutlinedButton(
                onClick = {
                    val activity = currentActivity
                    if (activity != null) viewModel.onPrintClick(activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.ready
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Печать в PDF")
            }

            Text(
                "Ориентация листа:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PdfOrientation.entries.forEach { orientation ->
                    FilterChip(
                        selected = selectedOrientation == orientation,
                        onClick = { viewModel.setOrientation(orientation) },
                        label = { Text(orientation.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}