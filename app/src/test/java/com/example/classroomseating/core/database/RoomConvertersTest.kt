package com.example.classroomseating.core.database

import com.example.classroomseating.core.database.entity.BoardPosition
import com.example.classroomseating.core.database.entity.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomConvertersTest {

    private val converter = RoomConverters()

    @Test
    fun genderRoundTripWorks() {
        assertEquals("MALE", converter.fromGender(Gender.MALE))
        assertEquals(Gender.FEMALE, converter.toGender("FEMALE"))
        assertEquals(Gender.UNSPECIFIED, converter.toGender("UNKNOWN_VALUE"))
    }

    @Test
    fun boardPositionRoundTripWorks() {
        assertEquals("LEFT", converter.fromBoardPosition(BoardPosition.LEFT))
        assertEquals(BoardPosition.BOTTOM, converter.toBoardPosition("BOTTOM"))
        assertEquals(BoardPosition.TOP, converter.toBoardPosition("BAD"))
    }
}