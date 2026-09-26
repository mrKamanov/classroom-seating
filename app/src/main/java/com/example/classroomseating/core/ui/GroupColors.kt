package com.example.classroomseating.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Палитра для раскраски малых групп. Совпадает по стилю с палитрой
 * ограничений по здоровью (Material 600-тона), чтобы экран выглядел целостно.
 */
object GroupColors {

    val palette: List<Color> = listOf(
        Color(0xFFE53935), // красный
        Color(0xFF1E88E5), // синий
        Color(0xFF43A047), // зелёный
        Color(0xFFFB8C00), // оранжевый
        Color(0xFF8E24AA), // фиолетовый
        Color(0xFF00897B), // голубовато-зелёный
        Color(0xFFF9A825), // янтарный
        Color(0xFF3949AB)  // индиго
    )

    fun colorFor(groupIndex: Int): Color =
        palette[(groupIndex).mod(palette.size)]
}