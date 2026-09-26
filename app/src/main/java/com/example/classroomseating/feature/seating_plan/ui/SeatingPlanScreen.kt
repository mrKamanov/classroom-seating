package com.example.classroomseating.feature.seating_plan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.core.ui.dragdrop.DeskTarget
import com.example.classroomseating.core.ui.dragdrop.DragAndDropContainer
import com.example.classroomseating.core.ui.dragdrop.DraggableStudentCard
import com.example.classroomseating.core.ui.dragdrop.DraggableStudentUi
import com.example.classroomseating.core.ui.dragdrop.LocalDragAndDropState
import com.example.classroomseating.core.ui.dragdrop.restrictionColor
import com.example.classroomseating.core.util.buildDeskGrid
import com.example.classroomseating.domain.model.BoardPosition
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatingPlanScreen(
    classId: String,
    onEditLayout: () -> Unit,
    onShare: () -> Unit,
    onScanQr: () -> Unit,
    onBack: () -> Unit,
    viewModel: SeatingPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DragAndDropContainer {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.className?.let { "Рассадка — $it" } ?: "Рассадка",
                                fontWeight = FontWeight.SemiBold
                            )
                            state.classroom?.let { classroom ->
                                Text(
                                    text = classroom.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onShare,
                            enabled = state.hasPlan
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Обмен рассадкой"
                            )
                        }
                        IconButton(
                            onClick = onScanQr
                        ) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "Сканировать QR"
                            )
                        }
                        IconButton(
                            onClick = viewModel::autoArrange,
                            enabled = state.hasPlan && state.allStudents.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Автоматическая рассадка"
                            )
                        }
                        IconButton(
                            onClick = viewModel::shuffleArrange,
                            enabled = state.hasPlan && state.allStudents.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = "Бросок кубика: случайный вариант рассадки"
                            )
                        }
                    }
                )
            },
        ) { padding ->
            when {
                !state.hasPlan -> EmptyPlan(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onEditLayout = onEditLayout
                )
                state.classroom == null || state.desks.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Кабинет ещё не настроен",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onEditLayout) {
                        Text("Настроить кабинет")
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    SeatingGrid(
                        state = state,
                        onStudentDropped = viewModel::onStudentDropped,
                        onRemoveStudent = viewModel::unassignStudent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    UnassignedStudentsBar(students = state.unassignedStudents)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlan(
    modifier: Modifier = Modifier,
    onEditLayout: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Рассадка ещё не создана", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Настройте кабинет (сетка парт и положение доски), чтобы начать",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEditLayout) {
            Text("Настроить кабинет")
        }
    }
}

@Composable
private fun SeatingGrid(
    state: SeatingPlanUiState,
    onStudentDropped: (String, String, Int) -> Unit,
    onRemoveStudent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardPosition = state.classroom?.boardPosition ?: BoardPosition.TOP
    val columns = state.classroom?.columnsCount ?: 1
    val rows = state.classroom?.rowsCount ?: 1
    val desksByCoord = remember(state.desks) {
        state.desks.associateBy { it.gridX to it.gridY }
    }
    val grid = buildDeskGrid(boardPosition, rows = rows, columns = columns)

    val gridContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grid.lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    line.forEach { coord ->
                        deskAt(desksByCoord, coord)?.let { desk ->
                            DeskCard(
                                desk = desk,
                                state = state,
                                boardPosition = boardPosition,
                                gridRows = rows,
                                gridColumns = columns,
                                onStudentDropped = onStudentDropped,
                                onRemoveStudent = onRemoveStudent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (boardPosition) {
            BoardPosition.TOP -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardHeader()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    gridContent()
                }
            }
            BoardPosition.BOTTOM -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    gridContent()
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardHeader()
                }
            }
            BoardPosition.LEFT -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardHeader(horizontal = false, rotateText = true)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        gridContent()
                    }
                }
            }
            BoardPosition.RIGHT -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        gridContent()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardHeader(horizontal = false, rotateText = false)
                    }
                }
            }
        }
    }
}

private fun deskAt(
    desksByCoord: Map<Pair<Int, Int>, Desk>,
    coord: com.example.classroomseating.core.util.DeskCoord
): Desk? = desksByCoord[coord.gridX to coord.gridY]

@Composable
private fun DeskCard(
    desk: Desk,
    state: SeatingPlanUiState,
    boardPosition: BoardPosition,
    gridRows: Int,
    gridColumns: Int,
    onStudentDropped: (String, String, Int) -> Unit,
    onRemoveStudent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val violations = (0 until desk.capacity.coerceAtLeast(1))
        .mapNotNull { state.restrictionViolations[SeatKey(desk.id, it)] }
        .distinct()

    // Подсветка рекомендованных зон для перетаскиваемого ученика с категорией.
    val dragState = LocalDragAndDropState.current
    val draggedRestriction = dragState.draggedRestriction
    val deskHighlight = if (
        draggedRestriction != null &&
        draggedRestriction.hasRestriction &&
        draggedRestriction.isSuitableAt(desk.gridX, desk.gridY, gridRows, gridColumns)
    ) {
        restrictionColor(draggedRestriction)
    } else {
        null
    }

    // При доске слева/справа сиденья ставятся стопкой: длинная сторона парты
    // смотрит на доску, и ученики «повёрнуты» к ней лицом.
    val seatsVertical = boardPosition == BoardPosition.LEFT || boardPosition == BoardPosition.RIGHT
    val deskBackground = with(MaterialTheme.colorScheme) {
        Modifier
            .fillMaxSize()
            .background(surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .padding(3.dp)
    }

    Column(modifier = modifier) {
        if (violations.isNotEmpty()) {
            Text(
                text = "⚠ " + violations.joinToString(", ") { it.title },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        if (seatsVertical) {
            Column(
                modifier = deskBackground,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (0 until desk.capacity.coerceAtLeast(1)).forEach { seatIndex ->
                    SeatSlot(
                        desk = desk,
                        seatIndex = seatIndex,
                        state = state,
                        highlightColor = deskHighlight,
                        onStudentDropped = onStudentDropped,
                        onRemoveStudent = onRemoveStudent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                modifier = deskBackground,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (0 until desk.capacity.coerceAtLeast(1)).forEach { seatIndex ->
                    SeatSlot(
                        desk = desk,
                        seatIndex = seatIndex,
                        state = state,
                        highlightColor = deskHighlight,
                        onStudentDropped = onStudentDropped,
                        onRemoveStudent = onRemoveStudent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatSlot(
    desk: Desk,
    seatIndex: Int,
    state: SeatingPlanUiState,
    highlightColor: androidx.compose.ui.graphics.Color? = null,
    onStudentDropped: (String, String, Int) -> Unit,
    onRemoveStudent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val occupant = state.seatOccupants[SeatKey(desk.id, seatIndex)]
    DeskTarget(
        deskId = desk.id,
        seatIndex = seatIndex,
        assignedStudent = occupant?.let {
            DraggableStudentUi(
                id = it.id,
                displayName = "${it.lastName} ${it.firstName.take(1)}.",
                restriction = it.restriction
            )
        },
        onStudentDropped = onStudentDropped,
        onRemoveStudent = onRemoveStudent,
        highlightColor = highlightColor,
        modifier = modifier
    )
}

@Composable
private fun BoardHeader(
    horizontal: Boolean = true,
    rotateText: Boolean = false
) {
    val label = "ДОСКА"
    Box(
        modifier = Modifier
            .let {
                if (horizontal) {
                    it.fillMaxWidth(0.7f).height(24.dp)
                } else {
                    it.width(20.dp).height(120.dp)
                }
            }
            .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small),
        contentAlignment = if (horizontal) Alignment.Center else Alignment.Center
    ) {
        Text(
            text = label,
            modifier = if (!horizontal && rotateText) Modifier.rotate(-90f) else Modifier,
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UnassignedStudentsBar(students: List<Student>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (students.isEmpty()) {
                "Нерассаженные (0) — все ученики рассажены"
            } else {
                "Нерассаженные (${students.size}) — перетащите ученика на парту"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (students.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                students.forEach { student ->
                    DraggableStudentCard(
                        student = DraggableStudentUi(
                            id = student.id,
                            displayName = "${student.lastName} ${student.firstName.take(1)}.",
                            restriction = student.restriction
                        )
                    )
                }
            }
        }
    }
}