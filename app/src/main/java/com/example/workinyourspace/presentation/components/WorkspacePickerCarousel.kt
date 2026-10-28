package com.example.workinyourspace.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.WorkspaceViewModel
import kotlin.math.absoluteValue

fun getWorkspaceIcon(name: String, id: Int): String {
    val lowerName = name.lowercase().trim()
    return when {
        lowerName == "home" || lowerName == "default_workspace" || lowerName == "default" -> "🏠"
        lowerName.contains("gym") || lowerName.contains("fitness") || lowerName.contains("workout") || lowerName.contains("health") -> "🏋️"
        lowerName.contains("study") || lowerName.contains("school") || lowerName.contains("learn") || lowerName.contains("read") || lowerName.contains("college") -> "📚"
        lowerName.contains("work") || lowerName.contains("office") || lowerName.contains("job") || lowerName.contains("business") -> "💼"
        lowerName.contains("code") || lowerName.contains("dev") || lowerName.contains("tech") || lowerName.contains("program") -> "💻"
        lowerName.contains("focus") || lowerName.contains("deep") || lowerName.contains("target") -> "🎯"
        lowerName.contains("game") || lowerName.contains("play") || lowerName.contains("chill") -> "🎮"
        lowerName.contains("music") || lowerName.contains("song") || lowerName.contains("audio") -> "🎵"
        lowerName.contains("art") || lowerName.contains("design") || lowerName.contains("draw") -> "🎨"
        lowerName.contains("travel") || lowerName.contains("trip") || lowerName.contains("vacation") -> "✈️"
        lowerName.contains("food") || lowerName.contains("cook") || lowerName.contains("eat") -> "🍳"
        lowerName.contains("night") || lowerName.contains("sleep") || lowerName.contains("bed") -> "🌙"
        else -> {
            val fallbackIcons = listOf("💼", "🎯", "🚀", "💡", "⚡", "🌟", "🏆", "🧭", "🎨", "🔮", "🔥", "🌈")
            fallbackIcons[(id.hashCode().coerceAtLeast(0)) % fallbackIcons.size]
        }
    }
}

fun getWorkspaceDisplayName(name: String): String {
    return if (name == "DEFAULT_WORKSPACE" || name == "Default Workspace") "Home" else name
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspacePickerCarousel(
    workspaces: List<Workspace>,
    activeWorkspaceId: Int?,
    viewModel: WorkspaceViewModel,
    onSelectWorkspace: (Int?) -> Unit,
    onConfigureWhitelist: (Workspace) -> Unit,
    onClosePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(workspaces) { workspaces }

    val initialIndex = remember(items, activeWorkspaceId) {
        val found = items.indexOfFirst { it.id == activeWorkspaceId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = if (items.isNotEmpty()) initialIndex.coerceIn(0, items.size - 1) else 0,
        pageCount = { items.size }
    )

    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    // Dialog States for Create and Delete Workspace
    var showCreateDialog by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var deletingWorkspace by remember { mutableStateOf<Workspace?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                IconButton(
                    onClick = onClosePicker,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp)
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Workspaces Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap below to create your first workspace!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 60.dp)
                ) { page ->
                    val workspaceItem = items[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

                    val baseScale = lerp(
                        start = 0.82f,
                        stop = 1.0f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )

                    val alphaFactor = lerp(
                        start = 0.5f,
                        stop = 1.0f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )

                    val isTapped = (tappedIndex == page)
                    val targetTapScale by animateFloatAsState(
                        targetValue = if (isTapped) 1.25f else baseScale,
                        animationSpec = tween(durationMillis = 300),
                        label = "TapZoomAnimation",
                        finishedListener = {
                            if (isTapped) {
                                onSelectWorkspace(workspaceItem.id)
                            }
                        }
                    )

                    val icon = getWorkspaceIcon(workspaceItem.name, workspaceItem.id)
                    val displayName = getWorkspaceDisplayName(workspaceItem.name)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.82f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = targetTapScale
                                scaleY = targetTapScale
                                alpha = alphaFactor
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (workspaceItem.id == activeWorkspaceId) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                tappedIndex = page
                            }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = icon,
                                        fontSize = 38.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                if (!workspaceItem.isDefault && displayName.lowercase() != "home") {
                                    IconButton(
                                        onClick = { deletingWorkspace = workspaceItem },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Text("🗑️", fontSize = 18.sp)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (workspaceItem.id == activeWorkspaceId) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "● ACTIVE SESSION",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Button(
                                onClick = { onConfigureWhitelist(workspaceItem) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🖼️ Whitelist & Wallpaper", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("+ Create New Workspace", fontSize = 16.sp)
                }
            }
        }

        // Create Workspace Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Workspace") },
                text = {
                    OutlinedTextField(
                        value = newWorkspaceName,
                        onValueChange = { newWorkspaceName = it },
                        label = { Text("Workspace Name (e.g. Gym, Study)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newWorkspaceName.isNotBlank()) {
                                viewModel.addWorkspace(newWorkspaceName)
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

        // Delete Workspace Confirmation Dialog
        deletingWorkspace?.let { workspace ->
            AlertDialog(
                onDismissRequest = { deletingWorkspace = null },
                title = { Text("Delete Workspace") },
                text = { Text("Are you sure you want to delete '${getWorkspaceDisplayName(workspace.name)}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWorkspace(workspace)
                            deletingWorkspace = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingWorkspace = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
