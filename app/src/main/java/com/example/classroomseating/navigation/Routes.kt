package com.example.classroomseating.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data class ClassroomEditorRoute(
    val classId: String? = null
)

@Serializable
data class LayoutEditorRoute(
    val classId: String
)

@Serializable
data class SeatingPlanRoute(
    val classId: String
)

@Serializable
data class GroupsRoute(
    val classId: String
)

@Serializable
data class ShareExportRoute(
    val classId: String
)

@Serializable
data class QrScanRoute(
    val classId: String? = null
)

@Serializable
data object InstructionsRoute