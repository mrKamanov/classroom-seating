package com.example.classroomseating.core.database

import androidx.room.TypeConverter
import com.example.classroomseating.core.database.entity.BoardPosition
import com.example.classroomseating.core.database.entity.Gender

/**
 * Конвертация перечислений в строки для хранения в Room.
 */
class RoomConverters {

    @TypeConverter
    fun fromGender(gender: Gender): String = gender.name

    @TypeConverter
    fun toGender(value: String): Gender =
        runCatching { Gender.valueOf(value) }.getOrDefault(Gender.UNSPECIFIED)

    @TypeConverter
    fun fromBoardPosition(position: BoardPosition): String = position.name

    @TypeConverter
    fun toBoardPosition(value: String): BoardPosition =
        runCatching { BoardPosition.valueOf(value) }.getOrDefault(BoardPosition.TOP)
}