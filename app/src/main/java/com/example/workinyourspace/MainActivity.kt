package com.example.workinyourspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.workinyourspace.presentation.MainLauncherScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
import com.example.workinyourspace.presentation.navigation.WorkInYourSpaceNavGraph
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkInYourSpaceTheme {
                val activeWorkspaceId by viewModel.activeWorkspaceID.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                if (activeWorkspaceId != null) {
                    MainLauncherScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WorkInYourSpaceNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
