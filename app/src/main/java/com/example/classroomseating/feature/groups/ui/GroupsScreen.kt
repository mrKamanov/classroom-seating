package com.example.classroomseating.feature.groups.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.classroomseating.core.ui.GroupColors
import com.example.classroomseating.core.util.StudentGrouper
import com.example.classroomseating.domain.model.Desk
import com.example.classroomseating.domain.model.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.className ?: "Малые группы") },
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
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.hasPlan || state.classroom == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Сначала создайте схему рассадки для класса,\nчтобы делить учеников на группы по месту.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                return@Scaffold
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GroupControls(state = state, viewModel = viewModel)
                }

                gridItems(state.groups, key = { it.index }) { group ->
                    ClusterCard(
                        group = group,
                        state = state,
                        manual = state.settings.scenario == StudentGrouper.Scenario.MANUAL,
                        showSeats = state.settings.scenario == StudentGrouper.Scenario.TERRITORIAL,
                        onAdd = { viewModel.addToGroup(group.index) },
                        onRemoveLast = { viewModel.removeLastFromGroup(group.index) },
                        onRemoveStudent = { viewModel.removeFromGroup(it) }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    UngroupedStrip(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GroupControls(state: GroupsUiState, viewModel: GroupsViewModel) {
    val manual = state.settings.scenario == StudentGrouper.Scenario.MANUAL
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ControlsLabel("Сценарий")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudentGrouper.Scenario.entries.forEach { scenario ->
                FilterChip(
                    selected = state.settings.scenario == scenario,
                    onClick = { viewModel.setScenario(scenario) },
                    label = { Text(scenario.title) }
                )
            }
        }

        if (state.settings.scenario == StudentGrouper.Scenario.TERRITORIAL) {
            ControlsLabel("Шаблон зон")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudentGrouper.Template.entries.forEach { template ->
                    FilterChip(
                        selected = state.settings.template == template,
                        onClick = { viewModel.setTemplate(template) },
                        label = { Text(template.title) }
                    )
                }
            }
        }

        if (manual) {
            Text(
                "Ручной режим: кнопки +/− меняют состав групп, тап по имени убирает ученика.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ControlsLabel(
                when (state.settings.sizeMode) {
                    StudentGrouper.SizeMode.BY_COUNT -> "Размер групп"
                    StudentGrouper.SizeMode.PER_GROUP -> "Число человек в группе"
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.settings.scenario == StudentGrouper.Scenario.RANDOM) {
                    OutlinedButton(onClick = { viewModel.reshuffle() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Перемешать")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudentGrouper.SizeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.settings.sizeMode == mode,
                        onClick = { viewModel.setSizeMode(mode) },
                        label = { Text(mode.title) }
                    )
                }
            }
            when (state.settings.sizeMode) {
                StudentGrouper.SizeMode.BY_COUNT -> StepperUnit(
                    value = "${state.groups.size}",
                    onDecrease = { viewModel.setGroupCount(state.groups.size - 1) },
                    decreaseEnabled = state.groups.size > 1,
                    onIncrease = { viewModel.setGroupCount(state.groups.size + 1) },
                    increaseEnabled = state.groups.size < state.students.size
                ) { Text(" групп", style = MaterialTheme.typography.bodyMedium) }

                StudentGrouper.SizeMode.PER_GROUP -> StepperUnit(
                    value = "${state.settings.sizePerGroup}",
                    onDecrease = { viewModel.setSizePerGroup(-1) },
                    decreaseEnabled = state.settings.sizePerGroup > 2,
                    onIncrease = { viewModel.setSizePerGroup(+1) },
                    increaseEnabled = state.settings.sizePerGroup < 12
                ) { Text(" чел./группа", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun ControlsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Компактный степпер в рамке: минус — значение — плюс. */
@Composable
private fun StepperUnit(
    value: String,
    onDecrease: () -> Unit,
    decreaseEnabled: Boolean,
    onIncrease: () -> Unit,
    increaseEnabled: Boolean,
    trailing: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            StepButton(icon = Icons.Default.Remove, enabled = decreaseEnabled, onClick = onDecrease)
            Spacer(Modifier.width(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(6.dp))
            trailing()
            Spacer(Modifier.width(10.dp))
            StepButton(icon = Icons.Default.Add, enabled = increaseEnabled, onClick = onIncrease)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp)
    ) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun ClusterCard(
    group: GroupUi,
    state: GroupsUiState,
    manual: Boolean,
    showSeats: Boolean,
    onAdd: () -> Unit,
    onRemoveLast: () -> Unit,
    onRemoveStudent: (String) -> Unit
) {
    val color = GroupColors.colorFor(group.index)
    val desks = state.desks
        .filter { state.deskGroup[it.id] == group.index }
        .sortedWith(compareBy<Desk> { it.gridY }.thenBy { it.gridX })

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Группа ${group.index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${group.students.size} чел.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
if (manual) {
                    StepButton(icon = Icons.Default.Remove, enabled = group.students.isNotEmpty()) { onRemoveLast() }
                    StepButton(icon = Icons.Default.Add, enabled = state.ungrouped.isNotEmpty()) { onAdd() }
                }
            }

            if (showSeats) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (desks.isEmpty() && group.students.isEmpty()) {
                        Text(
                            "пустая группа",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        desks.chunked(2).forEach { rowDesks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowDesks.forEach { desk ->
                                    ClusterDeskCell(
                                        desk = desk,
                                        color = color,
                                        state = state,
                                        groupIndex = group.index,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(2 - rowDesks.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (group.students.isEmpty()) {
                        Text(
                            "пустая группа",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        group.students.forEach { student ->
                            Text(
                                "\u2022 ${shortName(student)}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (manual) {
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onRemoveStudent(student.id) }
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClusterDeskCell(
    desk: Desk,
    color: Color,
    state: GroupsUiState,
    groupIndex: Int,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(color.copy(alpha = 0.15f), label = "bg$groupIndex")
    val borderColor by animateColorAsState(color.copy(alpha = 0.75f), label = "brd$groupIndex")
    val seats = (0 until desk.capacity.coerceIn(1, 2)).map { seatIndex ->
        state.seatOccupants[SeatKey(desk.id, seatIndex)]
    }
    Box(
        modifier = modifier
            .height(46.dp)
            .background(bg, RoundedCornerShape(5.dp))
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            seats.forEach { student ->
                if (student != null) {
                    Text(
                        shortName(student),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun shortName(student: Student): String {
    val last = student.lastName.ifBlank { student.firstName }
    val firstInit = student.firstName.takeIf { it.isNotBlank() }?.take(1)?.let { " $it." } ?: ""
    return last + firstInit
}

@Composable
private fun UngroupedStrip(state: GroupsUiState, viewModel: GroupsViewModel) {
    if (state.ungrouped.isEmpty()) return
    val manual = state.settings.scenario == StudentGrouper.Scenario.MANUAL
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text(
                "Без группы (${state.ungrouped.size}):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        lazyRowItems(state.ungrouped, key = { it.id }) { student ->
            Text(
                shortName(student),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (manual) {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { viewModel.assignUngroupedToFirst(student.id) }
                        .padding(horizontal = 4.dp)
                } else Modifier
            )
        }
    }
}