package com.example.workinyourspace.presentation

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.components.PermissionsPrompt
import com.example.workinyourspace.presentation.components.WorkspaceGridHome
import com.example.workinyourspace.presentation.components.WorkspacePickerCarousel
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLauncherScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconRadiusPx = with(density) { 42.dp.toPx() }

    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()
    val isSelectingWorkspace by viewModel.isSelectingWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    var configuringWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    var showAppDrawer by remember { mutableStateOf(false) }
    var drawerAlpha by remember { mutableStateOf(1f) }

    // Drag and Drop Ghost state for app drawer drag-to-home
    var draggingAppInfo by remember { mutableStateOf<AppInfo?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

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

    Box(modifier = modifier.fillMaxSize()) {
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
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.5f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)),
                    exit = scaleOut(targetScale = 0.5f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    WorkspaceGridHome(
                        workspaceName = activeWorkspace?.name,
                        wallpaperUri = activeWorkspace?.wallpaperURI,
                        whitelistedApps = activeWhitelistedApps,
                        installedApps = installedApps,
                        viewModel = viewModel,
                        onOpenConfiguration = launchWorkspaceManager,
                        onRemoveFromHomeScreen = { app ->
                            viewModel.removeFromHomeScreen(app.workspaceId, app.packageName)
                        },
                        onSwipeUp = {
                            drawerAlpha = 1f
                            showAppDrawer = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // App Drawer Bottom Sheet
        if (showAppDrawer) {
            ModalBottomSheet(
                onDismissRequest = {
                    drawerAlpha = 1f
                    showAppDrawer = false
                },
                modifier = Modifier.graphicsLayer { alpha = drawerAlpha }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "App Drawer (Hold & Drag to Home)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val appMap = remember(installedApps) {
                        installedApps.associateBy { it.packageName }
                    }
                    val trayApps = remember(activeWhitelistedApps, appMap) {
                        activeWhitelistedApps.mapNotNull { appMap[it.packageName] }
                    }

                    if (trayApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No whitelisted apps in this workspace yet.\nWhitelist apps via Configuration first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = trayApps, key = { it.packageName }) { app ->
                                var itemPosition by remember { mutableStateOf(Offset.Zero) }
                                val bitmap = remember(app.icon) {
                                    try {
                                        app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            itemPosition = coordinates.positionInWindow()
                                        }
                                        .pointerInput(app.packageName) {
                                            detectDragGestures(
                                                onDragStart = { localOffset ->
                                                    val absoluteTouchOffset = itemPosition + localOffset
                                                    draggingAppInfo = app
                                                    dragPosition = absoluteTouchOffset
                                                    drawerAlpha = 0f // Make drawer invisible while keeping pointer input active!
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragPosition = (dragPosition ?: Offset.Zero) + dragAmount
                                                },
                                                onDragEnd = {
                                                    val finalPos = dragPosition
                                                    if (finalPos != null && activeWorkspaceId != null) {
                                                        viewModel.updateAppPosition(
                                                            activeWorkspaceId!!,
                                                            app.packageName,
                                                            finalPos.x - iconRadiusPx,
                                                            finalPos.y - iconRadiusPx
                                                        )
                                                    }
                                                    draggingAppInfo = null
                                                    dragPosition = null
                                                    drawerAlpha = 1f
                                                    showAppDrawer = false
                                                },
                                                onDragCancel = {
                                                    draggingAppInfo = null
                                                    dragPosition = null
                                                    drawerAlpha = 1f
                                                    showAppDrawer = false
                                                }
                                            )
                                        }
                                        .clickable {
                                            activeWorkspaceId?.let { wsId ->
                                                viewModel.updateAppPosition(wsId, app.packageName, 120f, 220f)
                                            }
                                            showAppDrawer = false
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = app.name,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Semi-Transparent Floating Drag Ghost centered right under finger
        if (draggingAppInfo != null && dragPosition != null) {
            val ghostApp = draggingAppInfo!!
            val ghostPos = dragPosition!!
            val ghostBitmap = remember(ghostApp.icon) {
                try {
                    ghostApp.icon.toBitmap(width = 96, height = 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.65f },
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (ghostPos.x - iconRadiusPx).roundToInt(),
                                (ghostPos.y - iconRadiusPx).roundToInt()
                            )
                        }
                        .size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (ghostBitmap != null) {
                            Image(
                                bitmap = ghostBitmap,
                                contentDescription = ghostApp.name,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ghostApp.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ghostApp.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DefaultDummyHomeScreen(
    onOpenConfiguration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Render device system launcher wallpaper using AndroidView
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    try {
                        val wm = android.app.WallpaperManager.getInstance(ctx)
                        setImageDrawable(wm.fastDrawable ?: wm.drawable)
                    } catch (e: Exception) {
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Configuration App icon in the top-right corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = onOpenConfiguration,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("⚙️", fontSize = 32.sp)
                }
            }
        }

        // Permissions Prompt at the bottom of the Dummy Home Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PermissionsPrompt(modifier = Modifier.fillMaxWidth())
        }
    }
}
