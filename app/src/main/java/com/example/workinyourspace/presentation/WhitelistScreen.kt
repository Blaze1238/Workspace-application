package com.example.workinyourspace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    workspaceId: Int,
    workspaceName: String,
    viewModel: WorkspaceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val whitelistedApps by viewModel.getWhitelistedApps(workspaceId).collectAsStateWithLifecycle(initialValue = emptyList())

    val whitelistedPackageNames = remember(whitelistedApps) {
        whitelistedApps.map { it.packageName }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whitelist: $workspaceName") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedApps) { app ->
                val isWhitelisted = whitelistedPackageNames.contains(app.packageName)

                WhitelistedAppRow(
                    app = app,
                    isWhitelisted = isWhitelisted,
                    onToggle = {
                        viewModel.toggleAppInWhitelist(workspaceId, app)
                    }
                )
            }
        }
    }
}

@Composable
fun WhitelistedAppRow(
    app: AppInfo,
    isWhitelisted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWhitelisted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isWhitelisted,
                onCheckedChange = onToggle
            )
        }
    }
}