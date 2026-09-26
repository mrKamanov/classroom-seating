package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.BoardPosition

/**
 * Координаты парты в сетке кабинета.
 */
data class DeskCoord(val gridX: Int, val gridY: Int)

/**
 * Визуальная раскладка парт относительно доски.
 *
 * «Ряд» — это полоса парт, идущая от доски вглубь кабинета (когда смотришь
 * на класс со стороны доски): как в классе, где обычно 3 ряда, и в каждом
 * ряду по 4–5 парт. Парты одного ряда выстраиваются перпендикулярно доске,
 * ряды размещаются вдоль доски.
 *
 * Инвариант координат: gridX = номер ряда (0..rows-1), gridY = номер парты
 * в ряду от доски (0..columns-1, 0 — парта у доски).
 *
 * [lines] — готовые к отрисовке строки: каждая строка рисуется горизонтально,
 * строки складываются вертикально. Порядок и состав строк уже учитывают
 * положение доски:
 *  - TOP: строки от доски вглубь (gridY 0,1,2... сверху вниз);
 *  - BOTTOM: строки зеркалятся (парта у доски — снизу);
 *  - LEFT: строки = ряды, парты уходят от доски вправо;
 *  - RIGHT: парты уходят от доски влево.
 */
data class DeskGrid(
    val lines: List<List<DeskCoord>>
)

fun buildDeskGrid(
    boardPosition: BoardPosition,
    rows: Int,
    columns: Int
): DeskGrid {
    val r = rows.coerceAtLeast(1)      // рядов
    val p = columns.coerceAtLeast(1)   // парт в ряду

    return when (boardPosition) {
        BoardPosition.TOP -> DeskGrid(
            (0 until p).map { y ->
                (0 until r).map { x -> DeskCoord(gridX = x, gridY = y) }
            }
        )
        BoardPosition.BOTTOM -> DeskGrid(
            (p - 1 downTo 0).map { y ->
                (0 until r).map { x -> DeskCoord(gridX = x, gridY = y) }
            }
        )
        BoardPosition.LEFT -> DeskGrid(
            (0 until r).map { x ->
                (0 until p).map { y -> DeskCoord(gridX = x, gridY = y) }
            }
        )
        BoardPosition.RIGHT -> DeskGrid(
            (0 until r).map { x ->
                (p - 1 downTo 0).map { y -> DeskCoord(gridX = x, gridY = y) }
            }
        )
    }
}