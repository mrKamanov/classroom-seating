package com.example.classroomseating.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Экспорт схемы рассадки в файл `.seating` и отправка через системный share-диалог (4.1).
 */
class FileExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    fun shareSeatingPlan(dto: SeatingPlanExportDto) {
        if (dto.students.isEmpty() && dto.desks.isEmpty()) return

        val jsonString = dto.encodeToJson(json)
        val safeClassName = dto.schoolClass.name
            .replace(Regex("[^A-Za-zА-Яа-яЁё0-9 ]"), "")
            .trim()
            .replace(" ", "_")
            .ifBlank { "class" }
        val cacheFile = File(context.cacheDir, "Seating_$safeClassName.seating")
        cacheFile.writeText(jsonString)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-seating"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Рассадка класса ${dto.schoolClass.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Поделиться рассадкой")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}