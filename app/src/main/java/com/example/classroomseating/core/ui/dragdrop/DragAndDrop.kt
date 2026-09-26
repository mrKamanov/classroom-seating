package com.example.classroomseating.core.ui.dragdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classroomseating.domain.model.SeatingRestriction

/**
 * Данные ученика для отрисовки на парте/в списке.
 */
data class DraggableStudentUi(
    val id: String,
    val displayName: String,
    val restriction: SeatingRestriction = SeatingRestriction.NONE
)

/** Акцентный цвет категории ограничения по здоровью. */
fun restrictionColor(restriction: SeatingRestriction): Color = when (restriction) {
    SeatingRestriction.NONE -> Color.Unspecified
    SeatingRestriction.VISION -> Color(0xFF1E88E5)
    SeatingRestriction.VISION_CORRECTED -> Color(0xFF64B5F6)
    SeatingRestriction.HYPEROPIA -> Color(0xFF8E24AA)
    SeatingRestriction.HEARING -> Color(0xFF00897B)
    SeatingRestriction.FREQUENT_ILLNESS -> Color(0xFF43A047)
    SeatingRestriction.PHOTOPHOBIA -> Color(0xFFF9A825)
    SeatingRestriction.MUSCULOSKELETAL -> Color(0xFFFB8C00)
    SeatingRestriction.ATTENTION -> Color(0xFFE53935)
    SeatingRestriction.TALL_VISION -> Color(0xFF3949AB)
}

/** Ключ цели перетаскивания: конкретное место за партой. */
data class DropTargetKey(
    val deskId: String,
    val seatIndex: Int
)

/**
 * Единый менеджер состояния перетаскивания (3.1).
 */
class DragAndDropState {
    var isDragging by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggedStudentId by mutableStateOf<String?>(null)
    var draggedStudentName by mutableStateOf<String?>(null)
    var draggedRestriction by mutableStateOf<SeatingRestriction?>(null)
    var lastDraggedStudentId by mutableStateOf<String?>(null)

    /** Место, над которым отпустили ученика в последний раз. */
    var lastDropKey by mutableStateOf<DropTargetKey?>(null)

    private val targetBounds = mutableMapOf<DropTargetKey, Rect>()

    /** Текущая позиция пальца/курсора в координатах окна. */
    val pointerPosition: Offset get() = dragPosition + dragOffset

    fun registerTarget(key: DropTargetKey, bounds: Rect) {
        targetBounds[key] = bounds
    }

    fun unregisterTarget(key: DropTargetKey) {
        targetBounds.remove(key)
    }

    fun targetAt(position: Offset): DropTargetKey? =
        targetBounds.entries.firstOrNull { it.value.contains(position) }?.key

    fun startDrag(studentId: String, studentName: String, restriction: SeatingRestriction, initialPosition: Offset) {
        draggedStudentId = studentId
        draggedStudentName = studentName
        draggedRestriction = restriction
        dragPosition = initialPosition
        dragOffset = Offset.Zero
        lastDropKey = null
        isDragging = true
    }

    fun stopDrag() {
        // Цель определяется до сброса позиции: иначе hit-test уже невозможен.
        lastDropKey = targetAt(pointerPosition)
        isDragging = false
        draggedStudentId = null
        draggedStudentName = null
        draggedRestriction = null
        dragOffset = Offset.Zero
        // lastDraggedStudentId сохраняется до следующего старта перетаскивания
    }
}

val LocalDragAndDropState = staticCompositionLocalOf { DragAndDropState() }

/**
 * Контейнер: предоставляет состояние перетаскивания через CompositionLocal
 * и рисует плавающую карточку ученика поверх всех слоёв (5).
 */
@Composable
fun DragAndDropContainer(content: @Composable () -> Unit) {
    val dragState = remember { DragAndDropState() }

    CompositionLocalProvider(LocalDragAndDropState provides dragState) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            if (dragState.isDragging && dragState.draggedStudentName != null) {
                Card(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = dragState.dragPosition.x + dragState.dragOffset.x
                            translationY = dragState.dragPosition.y + dragState.dragOffset.y
                            scaleX = 1.05f
                            scaleY = 1.05f
                            alpha = 0.9f
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = dragState.draggedStudentName.orEmpty(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Карточка ученика, которую можно перетаскивать (3.2).
 */
@Composable
fun DraggableStudentCard(
    student: DraggableStudentUi,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val dragState = LocalDragAndDropState.current
    var cardPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cardPositionInWindow = coordinates.positionInWindow()
            }
            .pointerInput(student.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragState.lastDraggedStudentId = student.id
                        dragState.startDrag(
                            student.id,
                            student.displayName,
                            student.restriction,
                            cardPositionInWindow + offset
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragState.dragOffset += dragAmount
                    },
                    onDragEnd = { dragState.stopDrag() },
                    onDragCancel = { dragState.stopDrag() }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (student.restriction.hasRestriction)
                restrictionColor(student.restriction).copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = student.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (student.restriction.hasRestriction) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            restrictionColor(student.restriction),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.restriction.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            trailingAction?.let {
                Spacer(modifier = Modifier.width(2.dp))
                it()
            }
        }
    }
}

/**
 * Зона парты / Drop Target (3.3). Подсвечивается при наведении.
 */
@Composable
fun DeskTarget(
    deskId: String,
    seatIndex: Int,
    assignedStudent: DraggableStudentUi?,
    onStudentDropped: (studentId: String, deskId: String, seatIndex: Int) -> Unit,
    onRemoveStudent: (studentId: String) -> Unit = {},
    highlightColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val dragState = LocalDragAndDropState.current
    val targetKey = remember(deskId, seatIndex) { DropTargetKey(deskId, seatIndex) }
    var boundsInWindow by remember { mutableStateOf(Rect.Zero) }

    val isHovered = dragState.isDragging && boundsInWindow.contains(dragState.pointerPosition)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                boundsInWindow = coordinates.boundsInWindow()
                dragState.registerTarget(targetKey, boundsInWindow)
            }
            .border(
                width = if (isHovered || highlightColor != null) 2.dp else 1.dp,
                color = when {
                    isHovered -> MaterialTheme.colorScheme.primary
                    highlightColor != null -> highlightColor
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = when {
                    isHovered ->
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    highlightColor != null -> highlightColor.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (assignedStudent != null) {
            var cellPosition by remember { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        cellPosition = coordinates.positionInWindow()
                    }
                    .pointerInput(assignedStudent.id) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragState.lastDraggedStudentId = assignedStudent.id
                                dragState.startDrag(
                                    assignedStudent.id,
                                    assignedStudent.displayName,
                                    assignedStudent.restriction,
                                    cellPosition + offset
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragState.dragOffset += dragAmount
                            },
                            onDragEnd = { dragState.stopDrag() },
                            onDragCancel = { dragState.stopDrag() }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (assignedStudent.restriction.hasRestriction) {
                        Box(
                            modifier = Modifier
                                .background(
                                    restrictionColor(assignedStudent.restriction),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = assignedStudent.restriction.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = Color.White
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                    Text(
                        text = assignedStudent.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onRemoveStudent(assignedStudent.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Снять с парты",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Text(
                text = "·",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }

    DisposableEffect(targetKey) {
        onDispose { dragState.unregisterTarget(targetKey) }
    }

    LaunchedEffect(dragState.lastDropKey) {
        if (dragState.lastDropKey == targetKey) {
            dragState.lastDropKey = null
            dragState.lastDraggedStudentId?.let { studentId ->
                onStudentDropped(studentId, deskId, seatIndex)
            }
        }
    }
}