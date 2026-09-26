package com.example.classroomseating.domain.model

/**
 * Ограничение по здоровью, влияющее на выбор места в классе.
 *
 * Опирается на СанПиН 2.4.2.2821-10 (п. 5.5), СанПиН 2.4.2.3286-15 (ОВЗ)
 * и практические рекомендации врачей:
 * - зрение (некорректированное) — ближние к доске парты;
 * - слух — первые парты, центральный ряд, чтобы видеть лицо учителя;
 * - частые простуды, светобоязнь — дальше от окон/наружной стены;
 * - опорно-двигательный аппарат — центральные ряды, место по росту;
 * - внимание (СДВГ) — ближе к учителю;
 * - высокий рост + зрение — центральный ряд, 2–3-я парта.
 *
 * [rule] задаёт зону, которую проверяет авто-рассадка и валидация.
 */
enum class SeatingRestriction(
    val title: String,
    val hint: String,
    val badge: String,
    val rule: RestrictionRule
) {
    NONE(
        title = "Без ограничений",
        hint = "Ограничений по здоровью нет",
        badge = "",
        rule = RestrictionRule.ANY
    ),
    VISION(
        title = "Зрение (нескорректированное)",
        hint = "Первые парты, ближе к доске",
        badge = "ЗР",
        rule = RestrictionRule.FRONT
    ),
    VISION_CORRECTED(
        title = "Зрение (с коррекцией)",
        hint = "Любое место; важны регулярные проверки",
        badge = "ЗРК",
        rule = RestrictionRule.ANY
    ),
    HYPEROPIA(
        title = "Дальнозоркость",
        hint = "Средние и дальние парты (тренировка аккомодации)",
        badge = "ДЗ",
        rule = RestrictionRule.BACK
    ),
    HEARING(
        title = "Нарушение слуха",
        hint = "Первые парты, центральный ряд — видно лицо учителя",
        badge = "СЛ",
        rule = RestrictionRule.FRONT_CENTER
    ),
    FREQUENT_ILLNESS(
        title = "Частые простуды (ОРЗ)",
        hint = "Подальше от окон и сквозняков — центральные ряды",
        badge = "ОРЗ",
        rule = RestrictionRule.CENTER
    ),
    PHOTOPHOBIA(
        title = "Светобоязнь",
        hint = "Подальше от окна и прямого света",
        badge = "СВ",
        rule = RestrictionRule.CENTER
    ),
    MUSCULOSKELETAL(
        title = "Опорно-двигательный аппарат",
        hint = "Центральные ряды, удобное место по росту",
        badge = "ОДА",
        rule = RestrictionRule.CENTER
    ),
    ATTENTION(
        title = "Внимание (СДВГ и др.)",
        hint = "Ближе к учителю, не на крайние задние места",
        badge = "ВН",
        rule = RestrictionRule.FRONT_CENTER
    ),
    TALL_VISION(
        title = "Высокий рост + зрение",
        hint = "Центральный ряд, 2–3-я парта",
        badge = "РВ",
        rule = RestrictionRule.MIDDLE_CENTER
    );

    val hasRestriction: Boolean get() = this != NONE

    fun isSuitableAt(gridX: Int, gridY: Int, rows: Int, columns: Int): Boolean =
        rule.isSuitableAt(gridX, gridY, rows, columns)

    companion object {
        fun fromStorage(value: String?): SeatingRestriction =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: NONE

        /**
         * Совместимость со старыми данными: если категория не задана,
         * но стояла галочка «зрение» — считаем её ограничением по зрению.
         */
        fun fromLegacy(visionConstraint: Boolean, storage: String?): SeatingRestriction {
            val parsed = fromStorage(storage)
            return if (parsed != NONE) parsed else if (visionConstraint) VISION else NONE
        }
    }
}

/**
 * Зона посадки относительно доски.
 *
 * В координатах приложения gridX — номер ряда (вдоль доски),
 * gridY — номер парты от доски (0 — у доски).
 */
enum class RestrictionRule {
    ANY,
    FRONT,
    FRONT_CENTER,
    MIDDLE_CENTER,
    BACK,
    CENTER;

    fun isSuitableAt(gridX: Int, gridY: Int, rows: Int, columns: Int): Boolean {
        val r = rows.coerceAtLeast(1)
        val c = columns.coerceAtLeast(1)
        val frontDepth = FRONT_DEPTH.coerceAtMost(c)
        val centerRows = centerBand(r)
        return when (this) {
            ANY -> true
            FRONT -> gridY < frontDepth
            FRONT_CENTER -> gridY < frontDepth && gridX in centerRows
            MIDDLE_CENTER -> gridY in 1..(MIDDLE_MAX_DEPTH.coerceAtMost(c - 1)) && gridX in centerRows
            BACK -> gridY >= (c - 2).coerceAtLeast(0)
            CENTER -> gridX in centerRows
        }
    }

    private fun centerBand(rows: Int): IntRange {
        val r = rows.coerceAtLeast(1)
        if (r <= 2) return 0..(r - 1)
        val margin = r / 3
        return margin..(r - 1 - margin)
    }

    private companion object {
        const val FRONT_DEPTH = 2
        const val MIDDLE_MAX_DEPTH = 2
    }
}