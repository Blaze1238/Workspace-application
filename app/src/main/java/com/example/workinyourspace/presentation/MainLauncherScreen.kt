package com.example.workinyourspace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.components.AppDrawer
import com.example.workinyourspace.presentation.components.PermissionsPrompt
import com.example.workinyourspace.presentation.components.WorkspaceCard

@Composable
fun MainLauncherScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var configuringWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()

    // If a workspace is selected for whitelist configuration, show WhitelistScreen
    configuringWorkspace?.let { workspace ->
        WhitelistScreen(
            workspaceId = workspace.id,
            workspaceName = workspace.name,
            viewModel = viewModel,
            onBackClick = { configuringWorkspace = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Workspaces") },
                    icon = { Text("📁") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("App Drawer") },
                    icon = { Text("📱") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Text("+")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            PermissionsPrompt(modifier = Modifier.padding(16.dp))

            when (selectedTab) {
                0 -> WorkspaceTabContent(
                    workspaces = workspaces,
                    activeWorkspaceId = activeWorkspaceId,
                    onActivate = { viewModel.activateWorkspace(it.id) },
                    onDeactivate = { viewModel.deactivateWorkspace() },
                    onConfigure = { workspace -> configuringWorkspace = workspace },
                    onDelete = { viewModel.deleteWorkspace(it) }
                )
                1 -> AppDrawer(viewModel = viewModel)
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Workspace") },
            text = {
                OutlinedTextField(
                    value = newWorkspaceName,
                    onValueChange = { newWorkspaceName = it },
                    label = { Text("Workspace Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWorkspaceName.isNotBlank()) {
                            viewModel.createWorkspace(newWorkspaceName)
                            newWorkspaceName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkspaceTabContent(
    workspaces: List<Workspace>,
    activeWorkspaceId: Int?,
    onActivate: (Workspace) -> Unit,
    onDeactivate: () -> Unit,
    onConfigure: (Workspace) -> Unit,
    onDelete: (Workspace) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Your Workspaces",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        workspaces.forEach { workspace ->
            WorkspaceCard(
                workspace = workspace,
                isActive = workspace.id == activeWorkspaceId,
                onActivate = { onActivate(workspace) },
                onDeactivate = onDeactivate,
                onConfigureWhitelist = { onConfigure(workspace) },
                onDelete = { onDelete(workspace) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}