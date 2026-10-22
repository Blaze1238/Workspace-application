package com.example.workinyourspace.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.workinyourspace.data.local.entity.Workspace

@Composable
fun WorkspaceCard(
    workspace: Workspace,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onConfigureWhitelist: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = workspace.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (isActive) {
                        Text(
                            text = "● ACTIVE SESSION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!workspace.isDefault) {
                    IconButton(onClick = onDelete) {
                        Text("🗑️")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Session")
                    }
                } else {
                    Button(onClick = onActivate) {
                        Text("Activate")
                    }
                }

                OutlinedButton(onClick = onConfigureWhitelist) {
                    Text("Manage Whitelist")
                }
            }
        }
    }
}