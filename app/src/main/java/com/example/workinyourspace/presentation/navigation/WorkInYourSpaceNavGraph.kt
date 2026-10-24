package com.example.workinyourspace.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workinyourspace.presentation.DefaultDummyHomeScreen
import com.example.workinyourspace.presentation.WhitelistScreen
import com.example.workinyourspace.presentation.WorkspaceScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel

@Composable
fun WorkInYourSpaceNavGraph(
    navController: NavHostController,
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "dummy_home",
        modifier = modifier
    ) {
        composable("dummy_home") {
            DefaultDummyHomeScreen(
                onOpenConfiguration = { navController.navigate("manager_dashboard") }
            )
        }

        composable("manager_dashboard") {
            WorkspaceScreen(
                viewModel = viewModel,
                onNavigateToWhitelist = { id, name ->
                    navController.navigate("whitelist/$id/$name")
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = "whitelist/{workspaceId}/{workspaceName}",
            arguments = listOf(
                navArgument("workspaceId") { type = NavType.IntType },
                navArgument("workspaceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("workspaceId") ?: return@composable
            val name = backStackEntry.arguments?.getString("workspaceName") ?: ""

            WhitelistScreen(
                workspaceId = id,
                workspaceName = name,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
