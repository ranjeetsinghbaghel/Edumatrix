package com.auraface.auraface_app.core.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.auraface.auraface_app.presentation.auth.*
import com.auraface.auraface_app.presentation.admin.*
import com.auraface.auraface_app.presentation.teacher.*
import com.auraface.auraface_app.presentation.student.*
import com.auraface.auraface_app.presentation.home.*

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainViewModel: com.auraface.auraface_app.presentation.home.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(context as androidx.activity.ComponentActivity)
    val pendingChatGroupId = mainViewModel.pendingChatGroupId
    
    LaunchedEffect(pendingChatGroupId) {
        if (pendingChatGroupId != null) {
            navController.navigate("${Screen.ChatMain.route}?groupId=${pendingChatGroupId}") {
                launchSingleTop = true
            }
            mainViewModel.clearPendingChatGroup()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.MainDashboard.route) {
            MainDashboardScreen(navController)
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(navController)
        }
        
        composable(Screen.AdminDefaulters.route) {
            DefaultersListScreen(navController)
        }
        
        composable(Screen.ManageStudents.route) {
            ManageStudentsScreen(navController)
        }

        composable(Screen.ManageTeachers.route) {
            ManageTeachersScreen(navController)
        }

        composable(Screen.ManageSubjects.route) {
            ManageSubjectsScreen(navController)
        }


        composable(Screen.TeacherDashboard.route) {
            TeacherDashboardScreen(navController)
        }
        
        composable("teacher_messages") {
            com.auraface.auraface_app.presentation.chat.ChatMainScreen(navController, isGroupMode = false)
        }

        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(navController)
        }
        
        composable("student_messages") {
            com.auraface.auraface_app.presentation.chat.ChatMainScreen(navController, isGroupMode = false)
        }
        
        composable("student_proctor") {
            com.auraface.auraface_app.presentation.student.StudentProctorScreen(navController)
        }

        composable("admin_teachers") {
            SimpleTextScreen("Manage Teachers")
        }

        composable("admin_attendance") {
            SimpleTextScreen("All Attendance")
        }

        composable("admin_reports") {
            AdminReportsScreen(navController)
        }

        composable("teacher_classes") {
            SimpleTextScreen("My Classes")
        }

        composable("teacher_attendance_history") {
            SimpleTextScreen("Attendance History")
        }

        composable(Screen.StudentProfile.route) {
            StudentProfileScreen(navController)
        }

        composable(Screen.StudentHistory.route) {
            StudentHistoryScreen(navController)
        }

        composable(Screen.StudentLeave.route) {
            LeaveRequestScreen(navController)
        }

        composable("student_schedule") {
            StudentTimetableScreen(navController)
        }

        composable("admin_profile") {
             AdminProfileScreen(navController)
        }

        composable("teacher_attendance_history/{subjectId}") { backStack ->
            val subjectId = backStack.arguments?.getString("subjectId")!!
            SubjectHistoryScreen(navController, subjectId)
        }

        composable(Screen.Attendance.route) { backStack ->
            val subjectId = backStack.arguments?.getString("subjectId")!!
            AttendanceScreen(navController, subjectId)
        }

        composable("change_password/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            ChangePasswordScreen(navController, role)
        }

        composable(Screen.TeacherSectionLeave.route) {
            SectionLeaveScreen(navController)
        }

        composable(Screen.TeacherHodLeave.route) {
            HodLeaveScreen(navController)
        }

        composable(Screen.TeacherProfile.route) {
            TeacherProfileScreen(navController)
        }
        composable(Screen.ManageTimetable.route) {
            ManageTimetableScreen(navController)
        }

        composable(Screen.Settings.route) {
            com.auraface.auraface_app.presentation.settings.SettingsScreen(navController)
        }

        composable(Screen.RealTimeMonitoring.route) {
            RealTimeMonitoringScreen(navController)
        }

        composable("schedule_makeup") {
            ScheduleMakeupScreen(navController)
        }
        
        // Smart Features
        composable(Screen.SmartNotices.route) {
            com.auraface.auraface_app.presentation.smart.SmartNoticeBoardScreen(navController)
        }
        composable("priority_notices") {
            com.auraface.auraface_app.presentation.smart.SmartNoticeBoardScreen(navController)
        }
        
        composable(Screen.SmartInsights.route) {
            com.auraface.auraface_app.presentation.smart.AttendanceInsightsScreen(navController)
        }
        
        composable(Screen.SmartLibrary.route) {
            com.auraface.auraface_app.presentation.student.SmartLibraryScreen(navController)
        }
        
        composable(Screen.AuraChat.route) {
            com.auraface.auraface_app.presentation.smart.AuraChatScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }

        composable(Screen.SendNotice.route) {
            com.auraface.auraface_app.presentation.admin.SendNoticeScreen(navController)
        }
        
        composable(Screen.ExamSchedule.route) {
            ExamScheduleScreen(navController)
        }
        composable(Screen.ExamResults.route) {
            ExamResultsScreen(navController)
        }
        composable(Screen.CGPACalculator.route) {
            CGPAScreen(navController)
        }
        
        composable(Screen.UploadMarks.route) {
            com.auraface.auraface_app.presentation.teacher.UploadMarksScreen(navController)
        }

        composable(Screen.MyClass.route) {
            com.auraface.auraface_app.presentation.teacher.MyClassScreen(navController)
        }

        composable("gallery") {
            com.auraface.auraface_app.presentation.gallery.GalleryFoldersScreen(navController)
        }

        composable(
            route = "gallery_images/{folderId}/{folderName}",
            arguments = listOf(
                androidx.navigation.navArgument("folderId") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("folderName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getInt("folderId") ?: 0
            val folderName = backStackEntry.arguments?.getString("folderName") ?: "Images"
            com.auraface.auraface_app.presentation.gallery.GalleryImagesScreen(navController, folderId, folderName)
        }
        
        composable("placement_readiness") {
             com.auraface.auraface_app.presentation.placement.PlacementReadinessScreen(navController)
        }
        
        composable(Screen.QuizGame.route) {
            com.auraface.auraface_app.presentation.game.QuizGameScreen(navController)
        }
        
        composable(Screen.AddQuizQuestion.route) {
            com.auraface.auraface_app.presentation.admin.AddQuestionScreen(navController)
        }
        
        composable(
            route = "${Screen.ChatMain.route}?groupId={groupId}&groupName={groupName}",
            arguments = listOf(
                androidx.navigation.navArgument("groupId") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true 
                },
                androidx.navigation.navArgument("groupName") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            val groupName = backStackEntry.arguments?.getString("groupName")
            com.auraface.auraface_app.presentation.chat.ChatMainScreen(
                navController = navController, 
                isGroupMode = groupId?.startsWith("DM_") != true,
                autoOpenGroupId = groupId,
                autoOpenGroupName = groupName
            )
        }

        composable(Screen.RewardsStore.route) {
            com.auraface.auraface_app.presentation.student.RewardsStoreScreen(navController)
        }

        composable(Screen.AuraFocus.route) {
            com.auraface.auraface_app.presentation.student.AuraFocusScreen(navController)
        }

        composable(Screen.AuraPulse.route) {
            com.auraface.auraface_app.presentation.student.AuraPulseScreen(navController)
        }

        composable(Screen.AuraQuests.route) {
            com.auraface.auraface_app.presentation.student.AuraQuestsScreen(navController)
        }

        composable(Screen.AuraBites.route) {
            com.auraface.auraface_app.presentation.student.AuraBitesScreen(navController)
        }

        composable(Screen.AuraSpaces.route) {
            com.auraface.auraface_app.presentation.student.AuraSpacesScreen(navController)
        }

        composable(Screen.AuraFound.route) {
            com.auraface.auraface_app.presentation.student.AuraFoundScreen(navController)
        }

        composable(Screen.CampusPulse.route) {
            com.auraface.auraface_app.presentation.teacher.CampusPulseScreen(navController)
        }

        composable("academic_calendar") {
            com.auraface.auraface_app.presentation.smart.UnifiedCalendarScreen(navController)
        }

        composable("admin_calendar") {
            com.auraface.auraface_app.presentation.admin.AdminCalendarScreen(navController)
        }

        composable("study_planner") {
            com.auraface.auraface_app.presentation.smart.SmartStudyPlannerScreen(navController)
        }

        composable("teacher_availability") {
            com.auraface.auraface_app.presentation.student.TeacherAvailabilityScreen(navController)
        }
        
        composable(Screen.CampusMovies.route) {
            com.auraface.auraface_app.presentation.student.CampusMoviesScreen(navController)
        }

        composable(Screen.CampusSports.route) {
            com.auraface.auraface_app.presentation.student.CampusSportsScreen(navController)
        }

        composable(Screen.AIInterview.route) {
            com.auraface.auraface_app.presentation.placement.AIInterviewScreen(navController)
        }

        composable(Screen.MockTest.route) {
            com.auraface.auraface_app.presentation.student.MockTestScreen(navController)
        }

        composable(Screen.ResumeBuilder.route) {
            com.auraface.auraface_app.presentation.placement.ResumeBuilderScreen(navController)
        }

        composable(Screen.FeePayment.route) {
            com.auraface.auraface_app.presentation.student.FeePaymentScreen(navController)
        }

        composable(Screen.LibrarySeatAllocation.route) {
            com.auraface.auraface_app.presentation.student.LibrarySeatAllocationScreen(navController)
        }

        composable(Screen.GietErpDashboard.route) {
            com.auraface.auraface_app.presentation.student.GietErpDashboardScreen(navController)
        }

        composable(Screen.TeacherStudentLookup.route) {
            com.auraface.auraface_app.presentation.teacher.TeacherStudentLookupScreen(navController)
        }
    }
}
