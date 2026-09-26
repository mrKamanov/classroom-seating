package com.example.classroomseating.feature.export.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentAdapter.LayoutResultCallback
import android.print.PrintDocumentAdapter.WriteResultCallback
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import com.example.classroomseating.core.export.SeatingPlanExportDto
import com.example.classroomseating.core.util.buildDeskGrid
import com.example.classroomseating.domain.model.BoardPosition
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Ориентация листа А4 для экспорта схемы в PDF. */
enum class PdfOrientation(val label: String, val width: Int, val height: Int) {
    PORTRAIT("Портрет", 595, 842),
    LANDSCAPE("Альбом", 842, 595)
}

/**
 * Печать схемы рассадки в PDF через системный PrintManager (4.4).
 */
@Singleton
class SeatingPlanPdfPrinter @Inject constructor() {

    private companion object {
        const val TAG = "SeatingPlanPdfPrinter"
    }

    fun print(
        activity: Activity,
        dto: SeatingPlanExportDto,
        qrBitmap: android.graphics.Bitmap?,
        orientation: PdfOrientation
    ) {
        // PrintManager.print() бросает IllegalStateException, если контекст не является Activity.
        if (activity.isFinishing) return
        val printManager = try {
            activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось получить PrintManager", e)
            return
        }
        val baseName = "Seating_${dto.schoolClass.name}"

        try {
            printManager.print(
                "Рассадка ${dto.schoolClass.name}",
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes,
                        newAttributes: PrintAttributes,
                        cancellationSignal: CancellationSignal,
                        callback: LayoutResultCallback,
                        extras: Bundle
                    ) {
                        if (newAttributes != null && !cancellationSignal.isCanceled) {
                            val info = PrintDocumentInfo.Builder(baseName)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                .build()
                            callback.onLayoutFinished(info, true)
                        } else {
                            callback.onLayoutCancelled()
                        }
                    }

                    override fun onWrite(
                        pages: Array<PageRange>,
                        destination: ParcelFileDescriptor,
                        cancellationSignal: CancellationSignal,
                        callback: WriteResultCallback
                    ) {
                        val pdf = PdfDocument()
                        try {
                            val pageInfo = PageInfo.Builder(orientation.width, orientation.height, 10) // A4
                                .create()
                            val page = pdf.startPage(pageInfo)
                            drawPlanPage(
                                page.canvas,
                                dto,
                                qrBitmap,
                                pageW = orientation.width.toFloat(),
                                pageH = orientation.height.toFloat()
                            )
                            pdf.finishPage(page)

                            val output = FileOutputStream(destination.fileDescriptor)
                            pdf.writeTo(output)
                            output.flush()

                            callback.onWriteFinished(arrayOf(PageRange(0, 0)))
                        } catch (e: Exception) {
                            callback.onWriteFailed(e.message)
                        } finally {
                            pdf.close()
                        }
                    }
                },
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось начать печать", e)
        }
    }

    private fun drawPlanPage(
        canvas: Canvas,
        dto: SeatingPlanExportDto,
        qrBitmap: android.graphics.Bitmap?,
        pageW: Float,
        pageH: Float
    ) {
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val deskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val deskStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }

        var y = 40f
        canvas.drawText("Рассадка класса ${dto.schoolClass.name}", 40f, y, titlePaint)
        y += 22f
        val teacher = dto.schoolClass.classTeacherName
        val header = buildString {
            append("Кабинет: ${dto.classroom.name} ")
            append("(${dto.classroom.columnsCount}×${dto.classroom.rowsCount})")
            if (teacher?.isNotBlank() == true) append(" | Кл. руководитель: $teacher")
            append(" | Учеников: ${dto.students.size} | Парт: ${dto.desks.size}")
        }
        canvas.drawText(header, 40f, y, smallPaint)
        y += 30f

        // Карта парт: положение доски определяет ориентацию рядов и парт.
        val columns = dto.classroom.columnsCount.coerceAtLeast(1)
        val rows = dto.classroom.rowsCount.coerceAtLeast(1)
        val boardPosition = runCatching { BoardPosition.valueOf(dto.classroom.boardPosition) }
            .getOrDefault(BoardPosition.TOP)
        val boardGap = 14f
        val horizontalBoard = boardPosition == BoardPosition.TOP ||
            boardPosition == BoardPosition.BOTTOM

        // Сетка занимает ровно всю доступную область листа (как схема занимает
        // экран телефона): без принудительных минимумов/максимумов ячейки —
        // ничего не обрезается и не съезжает к центру.
        val landscape = pageW > pageH
        val sideMargin = if (landscape) 30f else 26f
        val topMargin = y + 46f
        // Резерв снизу под QR-код, чтобы сетка парт его не пересекала.
        val bottomMargin = pageH - (if (landscape) 115f else 125f)
        val availW = pageW - 2f * sideMargin
        val availH = (bottomMargin - topMargin).coerceAtLeast(200f)

        val grid = buildDeskGrid(boardPosition, rows = rows, columns = columns)
        val perLine = grid.lines.first().size.coerceAtLeast(1)
        val gridLines = grid.lines.size.coerceAtLeast(1)
        // perLine = число мест вдоль доски (gridX); сетка заполняет лист целиком.
        val cellW = availW / perLine
        val cellH = availH / gridLines
        val gridW = perLine * cellW
        val gridH = gridLines * cellH
        // Прижимаем сетку к шапке и заполняем лист от начала до конца.
        val startY = topMargin
        val startX = (pageW - gridW) / 2f

        if (horizontalBoard) {
            val boardW = minOf(gridW * 0.7f, pageW * 0.64f)
            val boardLeft = (pageW - boardW) / 2f
            val boardTop = if (boardPosition == BoardPosition.TOP) {
                startY - 18f - boardGap
            } else {
                startY + gridH + boardGap
            }
            canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + 18f, boardPaint)
            canvas.drawText("ДОСКА", boardLeft + boardW / 2f - 20f, boardTop + 14f, smallPaint)
        } else {
            val boardH = minOf(gridH * 0.7f, 220f)
            val boardTop = startY + (gridH - boardH) / 2f
            val boardLeft = if (boardPosition == BoardPosition.LEFT) {
                startX - 20f - boardGap
            } else {
                startX + gridW + boardGap
            }
            canvas.drawRect(boardLeft, boardTop, boardLeft + 20f, boardTop + boardH, boardPaint)
            canvas.drawText("ДОСКА", boardLeft + 4f, boardTop + boardH / 2f, smallPaint)
        }

        val seatsByDesk = HashMap<String, MutableList<Pair<Int, SeatingPlanExportDto.StudentDto>>>()
        dto.assignments.forEach { assignment ->
            seatsByDesk.getOrPut(assignment.deskId) { mutableListOf() }
                .add(assignment.seatIndex to studentById(dto, assignment.studentId))
        }

        val deskByCoord = dto.desks.associateBy { it.gridX to it.gridY }

        fun drawDeskAt(deskX: Float, deskY: Float, desk: SeatingPlanExportDto.DeskDto) {
            val deskW = cellW - 10f
            val deskH = cellH - 10f
            val corner = 12f
            canvas.drawRoundRect(deskX, deskY, deskX + deskW, deskY + deskH, corner, corner, deskPaint)
            canvas.drawRoundRect(deskX, deskY, deskX + deskW, deskY + deskH, corner, corner, deskStroke)

            val bySeat = seatsByDesk[desk.id].orEmpty()
                .sortedBy { it.first }
                .associate { it.first to it.second }
            val capacity = desk.capacity.coerceAtLeast(1)
            val seatGap = 3f
            val seatW = (deskW - seatGap * (capacity + 1)) / capacity
            val seatTop = deskY + 16f
            val seatH = deskH - 24f

            for (seat in 0 until capacity) {
                val seatLeft = deskX + seatGap + seat * (seatW + seatGap)
                val seatFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                val seatStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CCCCCC")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(
                    seatLeft, seatTop, seatLeft + seatW, seatTop + seatH,
                    8f, 8f, seatFill
                )
                canvas.drawRoundRect(
                    seatLeft, seatTop, seatLeft + seatW, seatTop + seatH,
                    8f, 8f, seatStroke
                )

                drawSeatName(
                    canvas,
                    seatLeft + seatW / 2f,
                    seatTop + seatH / 2f,
                    seatW - 8f,
                    seatH,
                    bySeat[seat]
                )
            }

            smallPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "Парта ${desk.gridX + 1}-${desk.gridY + 1}",
                deskX + deskW / 2f, deskY + deskH + 8f, smallPaint
            )
            smallPaint.textAlign = Paint.Align.LEFT
        }

        grid.lines.forEachIndexed { lineIndex, line ->
            line.forEachIndexed { cellIndex, coord ->
                val desk = deskByCoord[coord.gridX to coord.gridY] ?: return@forEachIndexed
                // «Ряды» идут от доски вглубь, «парты в ряду» — вдоль доски.
                val deskX = startX + cellIndex * cellW
                val deskY = startY + lineIndex * cellH
                drawDeskAt(deskX, deskY, desk)
            }
        }

        // QR в правом нижнем углу
        qrBitmap?.let { bmp ->
            val size = 90f
            canvas.drawBitmap(
                bmp,
                null,
                android.graphics.RectF(pageW - size - 30f, pageH - size - 30f, pageW - 30f, pageH - 30f),
                null
            )
            canvas.drawText("QR для импорта", pageW - size - 30f, pageH - size - 42f, smallPaint)
        }
    }

    /** Вписывает ФИО ученика в ячейку места: одна строка, если помещается, иначе несколько строк,
 *  шрифт сжимается до 6pt, чтобы имя не обрезалось. */
    private fun drawSeatName(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        maxW: Float,
        maxH: Float,
        student: SeatingPlanExportDto.StudentDto?
    ) {
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        if (student == null) {
            canvas.drawText("-", centerX, centerY - (gray.ascent() + gray.descent()) / 2f, gray)
            return
        }

        val firstName = student.firstName
        val lastName = student.lastName.ifBlank { if (firstName.isBlank()) "Ученик" else "" }
        val joined = "${firstName} ${lastName}".trim()

        /** Уменьшает шрифт [paint], пока [text] не влезет в [maxW] (минимум 6pt). */
        fun fit(paint: Paint, text: String) {
            while (paint.measureText(text) > maxW && paint.textSize > 6f) paint.textSize -= 0.5f
        }

        val lines = ArrayList<Pair<String, Paint>>()

        if (joined.isNotEmpty()) {
            base.textSize = 15f
            fit(base, joined)
            if (base.measureText(joined) <= maxW) {
                lines.add(joined to base)
            }
        }

        if (lines.isEmpty()) {
            val primary = if (lastName.isNotBlank()) lastName else firstName
            accent.textSize = 15f
            fit(accent, primary)
            if (accent.measureText(primary) <= maxW) {
                lines.add(primary to accent)
                if (firstName.isNotBlank() && lastName.isNotBlank() && maxH >= 40f) {
                    gray.textSize = accent.textSize
                    lines.add(firstName to gray)
                }
            } else if (maxH >= 50f) {
                // Фамилия шире ячейки даже на 6pt — кладём на две строки.
                val mid = if (primary.length > 2) primary.length / 2 else 1
                val space = primary.lastIndexOf(' ', mid)
                val pivot = if (space > 1) space else mid
                val a = primary.substring(0, pivot)
                val b = primary.substring(pivot).trimStart()
                val pa = Paint(accent).apply { textSize = 6f }
                val pb = Paint(accent).apply { textSize = 6f }
                fit(pa, a)
                fit(pb, b)
                lines.add(a to pa)
                lines.add(b to pb)
            } else {
                lines.add(primary to accent)
            }
        }

        if (lines.isEmpty()) {
            canvas.drawText("…", centerX, centerY - (gray.ascent() + gray.descent()) / 2f, gray)
            return
        }

        val lineH = lines.maxOf { it.second.textSize } * 1.15f
        val topY = centerY - lineH * (lines.size - 1) / 2f
        lines.forEachIndexed { i, (text, paint) ->
            canvas.drawText(
                text,
                centerX,
                topY + i * lineH - (paint.ascent() + paint.descent()) / 2f,
                paint
            )
        }
    }

    private fun studentById(dto: SeatingPlanExportDto, id: String): SeatingPlanExportDto.StudentDto =
        dto.students.firstOrNull { it.id == id } ?: SeatingPlanExportDto.StudentDto(id, "?", "", "", false, null)
}