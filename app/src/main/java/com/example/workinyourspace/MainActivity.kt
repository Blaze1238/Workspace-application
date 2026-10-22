package com.example.workinyourspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.workinyourspace.presentation.MainLauncherScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
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
                MainLauncherScreen(viewModel = viewModel)
            }
        }
    }
}