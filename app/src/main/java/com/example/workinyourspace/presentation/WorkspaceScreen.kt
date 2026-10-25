package com.example.workinyourspace.presentation

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.components.PermissionsPrompt
import com.example.workinyourspace.presentation.components.WorkspaceCard

@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    onNavigateToWhitelist: (Int, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val activeWorkspaceId by viewModel.activeWorkspaceID.collectAsStateWithLifecycle()

    var pendingActivationWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val handleActivation = { workspace: Workspace ->
        if (areRequiredPermissionsGranted(context)) {
            viewModel.activateWorkspace(workspace.id)
        } else {
            pendingActivationWorkspace = workspace
            showPermissionDialog = true
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Workspace Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!areRequiredPermissionsGranted(context)) {
                PermissionsPrompt(modifier = Modifier.padding(bottom = 16.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workspaces) { workspace ->
                    WorkspaceCard(
                        workspace = workspace,
                        isActive = workspace.id == activeWorkspaceId,
                        onActivate = { handleActivation(workspace) },
                        onDeactivate = { viewModel.deactivateWorkspace() },
                        onConfigureWhitelist = {
                            onNavigateToWhitelist(workspace.id, workspace.name)
                        },
                        onDelete = { viewModel.deleteWorkspace(workspace) }
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = {
                Column {
                    Text(
                        text = "To start a workspace session, OS-level Accessibility and Notification Access permissions must be granted to enforce app blocking.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionsPrompt()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (areRequiredPermissionsGranted(context)) {
                            showPermissionDialog = false
                            pendingActivationWorkspace?.let { viewModel.activateWorkspace(it.id) }
                        }
                    }
                ) {
                    Text("Check & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun areRequiredPermissionsGranted(context: Context): Boolean {
    return isAccessibilityServiceEnabled(context) && isNotificationListenerEnabled(context)
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityEnabled = try {
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
    } catch (e: Exception) {
        0
    }
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return settingValue.contains(context.packageName)
    }
    return false
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}
