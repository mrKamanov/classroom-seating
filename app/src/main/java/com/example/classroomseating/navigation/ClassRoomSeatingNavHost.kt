package com.example.classroomseating.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.classroomseating.feature.class_management.ui.StudentsScreen
import com.example.classroomseating.feature.classroom_editor.ui.LayoutEditorScreen
import com.example.classroomseating.feature.dashboard.ui.DashboardScreen
import com.example.classroomseating.feature.export.ui.ExportImportViewModel
import com.example.classroomseating.feature.export.ui.ShareExportScreen
import com.example.classroomseating.feature.groups.ui.GroupsScreen
import com.example.classroomseating.feature.imports.ImportViewModel
import com.example.classroomseating.feature.instructions.ui.InstructionsScreen
import com.example.classroomseating.feature.qr.ui.QrScanScreen
import com.example.classroomseating.feature.seating_plan.ui.SeatingPlanScreen

/**
 * Корневой NavHost с типобезопасными маршрутами (Type-Safe Navigation).
 */
@Composable
fun ClassRoomSeatingNavHost(importViewModel: ImportViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onClassClick = { classId ->
                    navController.navigate(ClassroomEditorRoute(classId))
                },
                onOpenSeatingPlan = { classId ->
                    navController.navigate(SeatingPlanRoute(classId))
                },
                onScanQr = {
                    navController.navigate(QrScanRoute())
                },
                onQrImagePicked = { uri ->
                    importViewModel.onQrImageUri(uri)
                },
                onOpenInstructions = {
                    navController.navigate(InstructionsRoute)
                }
            )
        }
        composable<InstructionsRoute> {
            InstructionsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<ClassroomEditorRoute> { entry ->
            val route = entry.toRoute<ClassroomEditorRoute>()
            StudentsScreen(
                classId = route.classId.orEmpty(),
                onOpenSeatingPlan = {
                    navController.navigate(SeatingPlanRoute(route.classId.orEmpty()))
                },
                onOpenLayoutEditor = {
                    navController.navigate(LayoutEditorRoute(route.classId.orEmpty()))
                },
                onOpenGroups = {
                    navController.navigate(GroupsRoute(route.classId.orEmpty()))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<GroupsRoute> { entry ->
            val route = entry.toRoute<GroupsRoute>()
            GroupsScreen(
                classId = route.classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<LayoutEditorRoute> { entry ->
            val route = entry.toRoute<LayoutEditorRoute>()
            LayoutEditorScreen(
                classId = route.classId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<SeatingPlanRoute> { entry ->
            val route = entry.toRoute<SeatingPlanRoute>()
            SeatingPlanScreen(
                classId = route.classId,
                onEditLayout = { navController.navigate(LayoutEditorRoute(route.classId)) },
                onShare = { navController.navigate(ShareExportRoute(route.classId)) },
                onScanQr = { navController.navigate(QrScanRoute(route.classId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<ShareExportRoute> { entry ->
            val route = entry.toRoute<ShareExportRoute>()
            val viewModel: ExportImportViewModel = hiltViewModel()
            ShareExportScreen(
                viewModel = viewModel,
                onScanQr = { navController.navigate(QrScanRoute(route.classId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<QrScanRoute> { entry ->
            QrScanScreen(
                onResult = { text ->
                    importViewModel.onQrScanned(text)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}