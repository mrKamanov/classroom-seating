package com.example.classroomseating.feature.instructions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Экран «Инструкция». Подробное пошаговое руководство для пользователя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Инструкция") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IntroCard()

            SectionTitle("Шаг 1. Создайте класс")
            StepCard(
                icon = Icons.Filled.Add,
                text = "Нажмите кнопку «Класс» в правом нижнем углу главного экрана. " +
                    "Впишите название класса (например, «5 \"А\"»), учебный год и классного руководителя. " +
                    "После создания класса вы сразу попадёте в его список учеников."
            )

            SectionTitle("Шаг 2. Загрузите учеников")
            StepCard(
                icon = Icons.Filled.FolderOpen,
                text = "Откройте класс — перед вами список учеников (пока пустой). " +
                    "Нажмите кнопку с иконкой загрузки (🔽 стрелка из облака) в правом верхнем углу, " +
                    "чтобы открыть меню «Загрузка учеников из файла»."
            )

            MySchoolCard()

            TemplateCard()

            ManualCard()

            SectionTitle("Шаг 3. Настройте кабинет")
            StepCard(
                icon = Icons.Filled.Settings,
                text = "В списке учеников нажмите иконку шестерёнки (⚙ «Настроить кабинет») " +
                    "в правом верхнем углу. Укажите название кабинета, число парт в ряду и число рядов. " +
                    "Выберите, где находится школьная доска (сверху, снизу, слева или справа). " +
                    "Проверьте предпросмотр и сохраните."
            )

            SectionTitle("Шаг 4. Рассадите учеников")
            StepCard(
                icon = Icons.Filled.EventSeat,
                text = "Нажмите иконку стульев (🪑 «Рассадка») в списке учеников. " +
                    "Внизу экрана будет полоса «Нерассаженные» — перетащите ученика пальцем " +
                    "на нужную парту. Так можно рассадить всех вручную."
            )
            StepCard(
                icon = Icons.Filled.AutoAwesome,
                text = "Или используйте автоматическую рассадку: иконка с искорками (✨) — " +
                    "ученики с ограничениями по здоровью будут посажены так, как требуется, " +
                    "а остальные распределятся автоматически. Есть и второй вариант — " +
                    "случайная рассадка (иконка игральных костей 🎲)."
            )
            StepCard(
                icon = Icons.Filled.Info,
                text = "Если кабинет ещё не настроен, экран рассадки предложит сделать это кнопкой «Настроить кабинет»."
            )

            SectionTitle("Шаг 5. Разделите класс на малые группы")
            StepCard(
                icon = Icons.Filled.Groups,
                text = "Иконка с силуэтами людей (👥 «Малые группы») откроет деление класса на группы: " +
                    "По месту (зоны парт), Случайно, По алфавиту или Вручную. " +
                    "Выберите сценарий и задайте число групп или размер группы."
            )

            SectionTitle("Шаг 6. Поделитесь рассадкой")
            StepCard(
                icon = Icons.Filled.Share,
                text = "На экране рассадки нажмите иконку «Поделиться». В открывшемся окне можно: " +
                    "показать коллеге QR-код рассадки, отправить QR-картинку или файл .seating " +
                    "в мессенджер, либо сохранить рассадку в PDF."
            )
            StepCard(
                icon = Icons.Filled.QrCodeScanner,
                text = "Чтобы получить чужую рассадку, на главном экране (иконка сканера вверху) " +
                    "отсканируйте QR-код камерой или загрузите QR-картинку из галереи " +
                    "(иконка с фото). Готовый класс появится на главном экране."
            )

            SectionTitle("Важные подсказки")
            BulletCard(text = "Все данные хранятся только на вашем телефоне и никуда не отправляются.")
            BulletCard(text = "При загрузке журнала «Моя школа» отчество в имени ученика автоматически отбрасывается.")
            BulletCard(text = "Если вы проверили список и учеников «не найдено» — выберите файл формата .xlsx (не .xls, не .csv).")
            BulletCard(text = "Один и тот же класс нельзя создать дважды с одинаковым названием — при импорте приложение предложит заменить или создать копию.")
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "«Классная Рассадка» помогает быстро рассадить класс по партам, " +
                    "регулировать посадку учеников с ограничениями по здоровью и делиться схемой " +
                    "с коллегами. Вот как ей пользоваться.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun StepCard(icon: ImageVector, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BulletCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MySchoolCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Из журнала «Моя школа» (рекомендуется)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "1. На компьютере или в телефоне откройте «Мою школу» и перейдите в журнал своего класса.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "2. Скачайте выгрузку в формате Excel (файл с расширением .xlsx). Подойдёт обычный " +
                    "журнал на любой выбранный период или вариант «Расширенный» — приложение само " +
                    "найдёт список детей, какую бы из этих версий вы ни скачали.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "3. В приложении откройте класс → меню загрузки учеников → кнопка «Загрузить из журнала \"Моя школа\"».",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "4. Выберите скачанный файл Excel. Приложение покажет список найденных учеников — " +
                    "проверьте его и нажмите «Добавить».",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Совет: фамилии и имена из журнала импортируются автоматически — " +
                    "печатать ничего не нужно. Отчества не переносятся.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TemplateCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Из шаблона Excel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "В том же меню «Загрузка учеников из файла» есть кнопки «Скачать шаблон» и «Загрузить шаблон». " +
                    "Скачайте пустую таблицу, впишите в неё фамилии и имена учеников, а затем загрузите обратно. " +
                    "Подходит, когда нет доступа к «Моей школе».",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ManualCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Вручную",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Нажмите кнопку «Ученик» в правом нижнем углу списка учеников и впишите фамилию и имя. " +
                    "Там же можно указать ограничение по здоровью (например, «Слышимость — первые парты») " +
                    "и примечание, например «Не сажать вместе с Ивановым».",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}