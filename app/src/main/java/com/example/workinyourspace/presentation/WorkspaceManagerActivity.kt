package com.example.workinyourspace.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.components.WorkspacePickerCarousel
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkspaceManagerActivity : ComponentActivity() {

    private val viewModel: WorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkInYourSpaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkspaceManagerScreen(
                        viewModel = viewModel,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkspaceManagerScreen(
    viewModel: WorkspaceViewModel,
    onFinish: () -> Unit
) {
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()

    var configuringWorkspace by remember { mutableStateOf<Workspace?>(null) }

    configuringWorkspace?.let { workspace ->
        WhitelistScreen(
            workspaceId = workspace.id,
            workspaceName = workspace.name,
            viewModel = viewModel,
            onBackClick = { configuringWorkspace = null }
        )
        return
    }

    WorkspacePickerCarousel(
        workspaces = workspaces,
        activeWorkspaceId = activeWorkspaceId,
        viewModel = viewModel,
        onSelectWorkspace = { id ->
            if (id != null) {
                viewModel.activateWorkspace(id)
            } else {
                viewModel.deactivateWorkspace()
            }
            onFinish()
        },
        onConfigureWhitelist = { workspace ->
            configuringWorkspace = workspace
        },
        onClosePicker = {
            onFinish()
        }
    )
}
