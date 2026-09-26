package com.example.classroomseating.feature.classroom_editor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.core.util.DeskGrid
import com.example.classroomseating.core.util.buildDeskGrid
import com.example.classroomseating.domain.model.BoardPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    classId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: LayoutEditorViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val boardPosition by viewModel.boardPosition.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == LayoutEditorEvent.Saved) onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Конструктор кабинета") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::updateName,
                label = { Text("Название кабинета") },
                placeholder = { Text("Кабинет 201") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            StepperRow(
                label = "Парт в ряду",
                value = columns,
                onDecrease = { viewModel.setColumns(columns - 1) },
                onIncrease = { viewModel.setColumns(columns + 1) }
            )
            StepperRow(
                label = "Рядов",
                value = rows,
                onDecrease = { viewModel.setRows(rows - 1) },
                onIncrease = { viewModel.setRows(rows + 1) }
            )

            BoardPositionSelector(
                selected = boardPosition,
                onSelect = viewModel::setBoardPosition
            )

            Text(
                "Предпросмотр сетки",
                style = MaterialTheme.typography.titleMedium
            )
            LayoutPreview(
                rows = rows,
                columns = columns,
                boardPosition = boardPosition
            )

            Button(
                onClick = viewModel::save,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сгенерировать парты и сохранить")
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrease) {
            Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(36.dp)
        )
        IconButton(onClick = onIncrease) {
            Icon(Icons.Default.Add, contentDescription = "Увеличить")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardPositionSelector(
    selected: BoardPosition,
    onSelect: (BoardPosition) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Положение доски",
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoardPosition.entries.forEach { position ->
                FilterChip(
                    selected = position == selected,
                    onClick = { onSelect(position) },
                    label = {
                        Text(
                            when (position) {
                                BoardPosition.TOP -> "Сверху"
                                BoardPosition.BOTTOM -> "Снизу"
                                BoardPosition.LEFT -> "Слева"
                                BoardPosition.RIGHT -> "Справа"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LayoutPreview(
    rows: Int,
    columns: Int,
    boardPosition: BoardPosition
) {
    val grid = buildDeskGrid(boardPosition, rows = rows, columns = columns)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (boardPosition) {
            BoardPosition.TOP -> {
                BoardStrip(horizontal = true)
                DeskLines(grid)
            }
            BoardPosition.BOTTOM -> {
                DeskLines(grid)
                BoardStrip(horizontal = true)
            }
            BoardPosition.LEFT -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoardStrip(horizontal = false)
                    DeskLines(grid)
                }
            }
            BoardPosition.RIGHT -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeskLines(grid)
                    BoardStrip(horizontal = false)
                }
            }
        }
    }
}

@Composable
private fun DeskLines(grid: DeskGrid) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        grid.lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                line.forEach { _ -> DeskTile() }
            }
        }
    }
}

@Composable
private fun BoardStrip(horizontal: Boolean) {
    Box(
        modifier = Modifier
            .let {
                if (horizontal) it.height(10.dp).width(90.dp) else it.width(10.dp).height(56.dp)
            }
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                MaterialTheme.shapes.small
            )
    )
}

@Composable
private fun DeskTile() {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 28.dp)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                MaterialTheme.shapes.small
            )
    )
}