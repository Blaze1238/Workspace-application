package com.example.workinyourspace.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.workinyourspace.data.local.entity.GridPosition
import com.example.workinyourspace.data.local.entity.WhitelistedApps
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.WorkspaceViewModel
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceGridHome(
    workspaceName: String?,
    wallpaperUri: String?,
    whitelistedApps: List<WhitelistedApps>,
    installedApps: List<AppInfo>,
    viewModel: WorkspaceViewModel,
    onOpenConfiguration: () -> Unit,
    onSelectSlotForPlacement: (gridX: Int, gridY: Int) -> Unit,
    onRemoveWhitelistedApp: (WhitelistedApps) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val columns = 4
    val rows = 5

    val appMap = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val positionMap = remember(whitelistedApps) {
        whitelistedApps.associateBy { Pair(it.position.x, it.position.y) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!wallpaperUri.isNullOrEmpty()) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Workspace Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = workspaceName ?: "Default Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                IconButton(
                    onClick = onOpenConfiguration,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                ) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until columns) {
                            val whitelistedApp = positionMap[Pair(c, r)]
                            val appInfo = whitelistedApp?.let { appMap[it.packageName] }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (whitelistedApp != null) {
                                    val displayName = appInfo?.name ?: whitelistedApp.packageName.substringAfterLast('.')

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                            .combinedClickable(
                                                onClick = {
                                                    viewModel.launchApp(whitelistedApp.packageName, context)
                                                },
                                                onLongClick = {
                                                    onRemoveWhitelistedApp(whitelistedApp)
                                                }
                                            )
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = displayName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectSlotForPlacement(c, r) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onOpenConfiguration() }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚙️", fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configuration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Workspace Grid Home Preview")
@Composable
fun WorkspaceGridHomePreview() {
    WorkInYourSpaceTheme {
        WorkspaceGridHome(
            workspaceName = "Gym Mode",
            wallpaperUri = null,
            whitelistedApps = listOf(
                WhitelistedApps(id = 1, workspaceId = 1, customIntent = null, packageName = "com.spotify.music", position = GridPosition(0, 0)),
                WhitelistedApps(id = 2, workspaceId = 1, customIntent = null, packageName = "com.google.android.youtube", position = GridPosition(1, 0))
            ),
            installedApps = emptyList(),
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            onOpenConfiguration = {},
            onSelectSlotForPlacement = { _, _ -> },
            onRemoveWhitelistedApp = {}
        )
    }
}
