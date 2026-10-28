package com.example.workinyourspace.presentation

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.components.GridCell
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

    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()
    val isSelectingWorkspace by viewModel.isSelectingWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    var configuringWorkspace by remember { mutableStateOf<Workspace?>(null) }
    var showAppDrawer by remember { mutableStateOf(false) }

    val selectedPackageNames = remember { mutableStateListOf<String>() }

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
            onBackClick = {
                configuringWorkspace = null
                viewModel.enterWorkspacePicker()
            }
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        val cols = 4
        val rows = 6
        val cellWidthPx = canvasWidthPx / cols.toFloat()
        val cellHeightPx = (canvasHeightPx - with(density) { 140.dp.toPx() }) / rows.toFloat()
        val iconWidthPx = with(density) { 72.dp.toPx() }
        val iconHeightPx = with(density) { 72.dp.toPx() }

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
                        workspaceId = activeWorkspace?.id,
                        workspaceName = activeWorkspace?.name,
                        wallpaperUri = activeWorkspace?.wallpaperURI,
                        whitelistedApps = activeWhitelistedApps,
                        installedApps = installedApps,
                        viewModel = viewModel,
                        onOpenWorkspacePicker = {
                            viewModel.enterWorkspacePicker()
                        },
                        onOpenConfiguration = launchWorkspaceManager,
                        onRemoveFromHomeScreen = { app ->
                            viewModel.removeFromHomeScreen(app.workspaceId, app.packageName)
                        },
                        onOpenAppDrawer = {
                            selectedPackageNames.clear()
                            showAppDrawer = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Multi-Select Whitelisted App Drawer Bottom Sheet
        if (showAppDrawer) {
            ModalBottomSheet(
                onDismissRequest = {
                    selectedPackageNames.clear()
                    showAppDrawer = false
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Add Whitelisted Apps to Workspace",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Select one or multiple apps to place on your home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val appMap = remember(installedApps) {
                        installedApps.associateBy { it.packageName }
                    }
                    val trayApps = remember(activeWhitelistedApps, appMap) {
                        activeWhitelistedApps.mapNotNull { appMap[it.packageName] }
                    }

                    val placedPackageNames = remember(activeWhitelistedApps) {
                        activeWhitelistedApps
                            .filter { it.position.x >= 0f && it.position.y >= 0f }
                            .map { it.packageName }
                            .toSet()
                    }

                    if (trayApps.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No whitelisted apps in this workspace yet.\nWhitelist apps via Whitelist & Wallpaper settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(items = trayApps, key = { it.packageName }) { app ->
                                val isPlaced = app.packageName in placedPackageNames
                                val isSelected = app.packageName in selectedPackageNames

                                val bitmap = remember(app.icon) {
                                    try {
                                        app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = when {
                                        isPlaced -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .clickable(enabled = !isPlaced) {
                                            if (isSelected) {
                                                selectedPackageNames.remove(app.packageName)
                                            } else {
                                                selectedPackageNames.add(app.packageName)
                                            }
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Box {
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
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                            }

                                            if (isPlaced) {
                                                Surface(
                                                    color = Color.DarkGray.copy(alpha = 0.9f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.align(Alignment.BottomEnd)
                                                ) {
                                                    Text(
                                                        text = "✓ On Screen",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isSelected) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                ) {
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (activeWorkspaceId != null) {
                                    var currentPlaced = activeWhitelistedApps
                                    selectedPackageNames.forEach { pkg ->
                                        val targetPos = findFirstAvailableCellPos(
                                            currentPlaced,
                                            cellWidthPx,
                                            cellHeightPx,
                                            iconWidthPx,
                                            iconHeightPx
                                        )
                                        viewModel.updateAppPosition(
                                            activeWorkspaceId!!,
                                            pkg,
                                            targetPos.x,
                                            targetPos.y
                                        )
                                        // Update simulated placed list for sequential placement
                                        currentPlaced = currentPlaced + WhitelistedApp(
                                            workspaceId = activeWorkspaceId!!,
                                            packageName = pkg,
                                            customIntent = null,
                                            position = com.example.workinyourspace.data.local.entity.FreeformPosition(targetPos.x, targetPos.y)
                                        )
                                    }
                                }
                                selectedPackageNames.clear()
                                showAppDrawer = false
                            },
                            enabled = selectedPackageNames.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = if (selectedPackageNames.isEmpty()) "Select Apps Above" else "Add ${selectedPackageNames.size} App(s) to Workspace",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun findFirstAvailableCellPos(
    placedApps: List<WhitelistedApp>,
    cellWidthPx: Float,
    cellHeightPx: Float,
    iconWidthPx: Float,
    iconHeightPx: Float
): Offset {
    val cols = 4
    val rows = 6
    val occupied = placedApps.filter { it.position.x >= 0f && it.position.y >= 0f }.map {
        val c = ((it.position.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, cols - 1)
        val r = ((it.position.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, rows - 1)
        GridCell(c, r)
    }.toSet()

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (GridCell(c, r) !in occupied) {
                val x = c * cellWidthPx + (cellWidthPx - iconWidthPx) / 2f
                val y = r * cellHeightPx + (cellHeightPx - iconHeightPx) / 2f
                return Offset(x, y)
            }
        }
    }
    return Offset(20f, 100f)
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
