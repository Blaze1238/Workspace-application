package com.example.workinyourspace.presentation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.components.AppDrawer
import com.example.workinyourspace.presentation.components.PermissionsPrompt
import com.example.workinyourspace.presentation.components.WorkspaceGridHome
import com.example.workinyourspace.presentation.components.WorkspacePickerCarousel
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLauncherScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()
    val isSelectingWorkspace by viewModel.isSelectingWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    var configuringWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    var selectedGridSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val activeWorkspace = remember(workspaces, activeWorkspaceId) {
        workspaces.find { it.id == activeWorkspaceId }
    }

    val activeWhitelistedApps by remember(activeWorkspaceId) {
        if (activeWorkspaceId != null) {
            viewModel.getWhitelistedApps(activeWorkspaceId!!)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val launchWorkspaceManager = {
        context.startActivity(Intent(context, WorkspaceManagerActivity::class.java))
    }

    configuringWorkspace?.let { workspace ->
        WhitelistScreen(
            workspaceId = workspace.id,
            workspaceName = workspace.name,
            viewModel = viewModel,
            onBackClick = { configuringWorkspace = null }
        )
        return
    }

    if (isSelectingWorkspace) {
        WorkspacePickerCarousel(
            workspaces = workspaces,
            activeWorkspaceId = activeWorkspaceId,
            viewModel = viewModel,
            onSelectWorkspace = { id ->
                if (id != null) {
                    viewModel.activateWorkspace(id)
                } else {
                    viewModel.deactivateWorkspace()
                    viewModel.exitWorkspacePicker()
                }
            },
            onConfigureWhitelist = { workspace ->
                viewModel.exitWorkspacePicker()
                configuringWorkspace = workspace
            },
            onCreateNewWorkspace = {
                showCreateDialog = true
            },
            onClosePicker = {
                viewModel.exitWorkspacePicker()
            }
        )
    } else {
        if (activeWorkspaceId == null) {
            DefaultDummyHomeScreen(
                onOpenConfiguration = launchWorkspaceManager,
                modifier = modifier
            )
        } else {
            WorkspaceGridHome(
                workspaceName = activeWorkspace?.name,
                wallpaperUri = activeWorkspace?.wallpaperURI,
                whitelistedApps = activeWhitelistedApps,
                installedApps = installedApps,
                viewModel = viewModel,
                onOpenConfiguration = launchWorkspaceManager,
                onSelectSlotForPlacement = { x, y ->
                    selectedGridSlot = Pair(x, y)
                },
                onRemoveWhitelistedApp = { app ->
                    viewModel.removeWhitelistedApp(app)
                },
                modifier = modifier
            )
        }
    }

    selectedGridSlot?.let { (x, y) ->
        ModalBottomSheet(
            onDismissRequest = { selectedGridSlot = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select App for Grid Position ($x, $y)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AppDrawer(
                    viewModel = viewModel,
                    onAppClick = { appInfo ->
                        activeWorkspaceId?.let { wsId ->
                            viewModel.addWhitelistedApp(
                                workspaceId = wsId,
                                packageName = appInfo.packageName,
                                gridX = x,
                                gridY = y
                            )
                        }
                        selectedGridSlot = null
                    }
                )
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
fun DefaultDummyHomeScreen(
    onOpenConfiguration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Default Home",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    text = "Select or create a workspace to customize your environment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            PermissionsPrompt(modifier = Modifier.fillMaxWidth())

            Surface(
                onClick = onOpenConfiguration,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚙️", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Workspace Manager",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to switch or manage workspaces",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
