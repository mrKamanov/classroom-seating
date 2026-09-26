package com.example.classroomseating.core.util

import com.example.classroomseating.domain.model.Student

/**
 * Сценарии деления класса на малые группы и шаблоны пространственных зон.
 *
 * Логика чистая (без Room/ViewModel), чтобы её можно было покрыть тестами.
 *
 * Все разрезы балансируются ПО ЧИСЛУ УЧЕНИКОВ (weight = сколько учеников за партой),
 * а не по числу парт — поэтому группы получаются практически равными
 * (отклонение не больше, чем человек за одной партой).
 */
object StudentGrouper {

    enum class Scenario(val title: String) {
        TERRITORIAL("По месту"),
        RANDOM("Случайно"),
        ALPHABET("По алфавиту"),
        MANUAL("Вручную")
    }

    enum class Template(val title: String) {
        BANDS("Полосы"),
        COLUMNS("Колонки"),
        BLOCKS("Блоки"),
        SNAKE("Змейка")
    }

    enum class SizeMode(val title: String) {
        BY_COUNT("Число групп"),
        PER_GROUP("По N в группе")
    }

    /**
     * Связный разрез [weights] на [k] частей, сбалансированный по сумме весов.
     * Возвращает номер части для каждого элемента (0..k-1).
     * Отклонение между суммами частей не больше максимального веса одного элемента.
     */
    fun balancedCut(weights: IntArray, k: Int): IntArray {
        val n = weights.size
        val safeK = k.coerceIn(1, n.coerceAtLeast(1))
        if (n == 0) return IntArray(0)
        val total = weights.sum().coerceAtLeast(1)
        val result = IntArray(n)
        var cum = 0L
        for (i in 0 until n) {
            cum += weights[i]
            // (cum-1), чтобы граница ложилась на следующий элемент, а не теряла первую группу.
            val beyond = ((cum - 1).coerceAtLeast(0L) * safeK) / total
            result[i] = if (safeK <= 1) 0 else beyond.toInt().coerceIn(0, safeK - 1)
        }
        return result
    }

    /**
     * Разбиение сетки на зоны по шаблону [Template], балансируя по числу учеников.
     * [desksByCoord] — вес каждой парты (сколько учеников за ней сидит; 0 — пустая парта).
     * Возвращает соответствие координата -> номер группы (0..groupCount-1) для ВСЕХ парт.
     *
     * [lines] — строки отрисовки из buildDeskGrid(BOTTOM, ...): сверху вниз от доски.
     */
    fun partitionTerritorial(
        lines: List<List<DeskCoord>>,
        desksByCoord: Map<DeskCoord, Int>,
        groupCount: Int,
        template: Template
    ): Map<DeskCoord, Int> {
        val count = groupCount.coerceAtLeast(1)
        val numLines = lines.size
        val columns = lines.firstOrNull()?.size ?: 0
        if (numLines == 0 || columns == 0) return emptyMap()

        val result = HashMap<DeskCoord, Int>()

        fun lineWeights(): IntArray = IntArray(numLines) { li ->
            lines[li].sumOf { desksByCoord[it] ?: 0 }
        }

        fun colWeights(): IntArray = IntArray(columns) { c ->
            lines.sumOf { line -> desksByCoord[line[c]] ?: 0 }
        }

        when (template) {
            // Полосы параллельно доске: разрез идёт по вертикали рядами,
            // граница ставится по накопленному числу учеников между партами.
            Template.BANDS -> {
                val zones = balancedCut(
                    IntArray(numLines * columns) { i ->
                        val li = i / columns
                        val c = i % columns
                        desksByCoord[lines[li][c]] ?: 0
                    },
                    count
                )
                lines.forEachIndexed { li, line ->
                    line.forEachIndexed { c, coord ->
                        result[coord] = zones[li * columns + c]
                    }
                }
            }

            // Колонки: разрезы идут вдоль доски, границы — между колонками по числу учеников.
            Template.COLUMNS -> {
                val zones = balancedCut(colWeights(), count)
                lines.forEach { line ->
                    line.forEach { coord -> result[coord] = zones[coord.gridX] }
                }
            }

            // Блоки: сначала ряд-зоны (целые линии), внутри каждой — колонки.
            Template.BLOCKS -> {
                val (lineGroups, colGroups) = blockDims(count)
                val lineZones = balancedCut(lineWeights(), lineGroups)
                val colZones = balancedCut(colWeights(), colGroups)
                lines.forEachIndexed { li, line ->
                    line.forEachIndexed { c, coord ->
                        result[coord] = lineZones[li] * colGroups + colZones[c]
                    }
                }
            }

            // Змейка: серпантин сверху вниз, сбалансированный разрез по ученикам.
            Template.SNAKE -> {
                val coords = ArrayList<DeskCoord>(numLines * columns)
                val weights = IntArray(numLines * columns)
                var idx = 0
                lines.forEachIndexed { li, line ->
                    val seq = if (li % 2 == 0) line else line.reversed()
                    seq.forEach { coord ->
                        coords.add(coord)
                        weights[idx++] = desksByCoord[coord] ?: 0
                    }
                }
                val zones = balancedCut(weights, count)
                coords.forEachIndexed { i, coord -> result[coord] = zones[i] }
            }
        }
        return result
    }

    /** Почти квадратные размеры блока: (число по горизонтали, число по вертикали), r*g = count. */
    internal fun blockDims(count: Int): Pair<Int, Int> {
        var bestCols = 1
        var bestRow = count
        var bestDiff = count - 1
        for (cols in 1..count) {
            if (count % cols != 0) continue
            val rows = count / cols
            val diff = kotlin.math.abs(rows - cols)
            if (diff < bestDiff || (diff == bestDiff && rows > bestRow)) {
                bestCols = cols
                bestRow = rows
                bestDiff = diff
            }
        }
        return bestRow to bestCols
    }

    /** Разбиение списка на [count] сбалансированных по размеру групп (порядок сохраняется). */
    fun chunk(list: List<Student>, count: Int): List<List<Student>> {
        if (count <= 0 || list.isEmpty()) return List(count.coerceAtLeast(1)) { emptyList() }
        val zones = balancedCut(IntArray(list.size) { 1 }, count)
        val k = (zones.maxOrNull() ?: 0) + 1
        val buckets = Array(k) { ArrayList<Student>() }
        list.forEachIndexed { i, student ->
            if (zones[i] < k) buckets[zones[i]].add(student)
        }
        return buckets.map { it }
    }

    /** Сортировка по алфавиту (фамилия, затем имя). */
    fun alphabetical(students: List<Student>): List<Student> =
        students.sortedWith(
            compareBy(
                { it.lastName.lowercase() },
                { it.firstName.lowercase() }
            )
        )
}