package com.example.arcana.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.arcana.core.analytics.AnalyticsScreens
import com.example.arcana.core.analytics.AnalyticsTracker
import com.example.arcana.core.analytics.NavigationAnalyticsObserver
import com.example.arcana.ui.screens.HomeScreen
import com.example.arcana.ui.screens.UserDetailScreen
import com.example.arcana.ui.screens.UserScreen

/**
 * Navigation route constants
 */
private object Routes {
    const val HOME = "home"
    const val USER_CRUD = "user_crud"
    const val USER_DETAIL = "user_detail/{userId}"
    const val USER_DETAIL_PREFIX = "user_detail/"

    fun userDetail(userId: Int): String = "user_detail/$userId"
}

/**
 * Main navigation graph with automatic analytics tracking
 *
 * Note: Screen views are automatically tracked via:
 * 1. NavigationAnalyticsObserver (tracks navigation changes)
 * 2. @TrackScreen annotation on ViewModels (tracks when ViewModel is created)
 *
 * Both approaches work together to ensure comprehensive tracking.
 */
@Composable
fun NavGraph(
    analyticsTracker: AnalyticsTracker
) {
    val navController = rememberNavController()

    // Automatically track all navigation changes
    NavigationAnalyticsObserver(
        navController = navController,
        analyticsTracker = analyticsTracker,
        routeToScreenNameMapper = { route ->
            when {
                route == Routes.HOME -> AnalyticsScreens.HOME
                route == Routes.USER_CRUD -> AnalyticsScreens.USER_CRUD
                route.startsWith(Routes.USER_DETAIL_PREFIX) -> AnalyticsScreens.USER_DETAIL
                else -> route
            }
        }
    )

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onNavigateToUserCrud = { navController.navigate(Routes.USER_CRUD) })
        }
        composable(Routes.USER_CRUD) {
            UserScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserDetail = { userId ->
                    navController.navigate(Routes.userDetail(userId))
                }
            )
        }
        composable(
            route = Routes.USER_DETAIL,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) {
            UserDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
