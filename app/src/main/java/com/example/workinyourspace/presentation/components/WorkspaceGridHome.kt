package com.example.workinyourspace.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.WorkspaceViewModel
import kotlin.math.roundToInt

data class GridCell(val col: Int, val row: Int)

data class WallpaperPreset(
    val id: String,
    val name: String,
    val previewColors: List<Color>
)

val DEFAULT_WALLPAPER_PRESETS = listOf(
    WallpaperPreset("preset://midnight_navy", "Midnight Navy", listOf(Color(0xFF1E2A38), Color(0xFF111823))),
    WallpaperPreset("preset://dark_minimal", "Dark Minimal", listOf(Color(0xFF1E1E2C), Color(0xFF0F0F1A))),
    WallpaperPreset("preset://gym_red", "Gym Red", listOf(Color(0xFF3A0007), Color(0xFF0D0002))),
    WallpaperPreset("preset://ocean_blue", "Ocean Blue", listOf(Color(0xFF001F3F), Color(0xFF000814))),
    WallpaperPreset("preset://calm_green", "Calm Green", listOf(Color(0xFF0B2B1A), Color(0xFF030E08))),
    WallpaperPreset("preset://sunset_purple", "Sunset Purple", listOf(Color(0xFF2B0B2B), Color(0xFF0E030E)))
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceGridHome(
    workspaceId: Int?,
    workspaceName: String?,
    wallpaperUri: String?,
    whitelistedApps: List<WhitelistedApp>,
    installedApps: List<AppInfo>,
    viewModel: WorkspaceViewModel,
    onOpenWorkspacePicker: () -> Unit,
    onOpenConfiguration: () -> Unit,
    onRemoveFromHomeScreen: (WhitelistedApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val appMap = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val placedApps = remember(whitelistedApps) {
        whitelistedApps.filter { it.position.x >= 0f && it.position.y >= 0f }
    }

    var activeDraggingApp by remember { mutableStateOf<WhitelistedApp?>(null) }
    var isHoveringTrash by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var showAssistiveMenu by remember { mutableStateOf(false) }
    var pendingDeleteApp by remember { mutableStateOf<WhitelistedApp?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (workspaceId != null) {
                viewModel.updateWorkspaceWallpaper(workspaceId, it.toString())
            }
        }
    }

    var assistiveTouchOffset by remember { mutableStateOf(Offset(20f, 600f)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181A20))
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        val gridCols = 4
        val gridRows = 6

        val cellWidthPx = canvasWidthPx / gridCols.toFloat()
        val cellHeightPx = (canvasHeightPx - with(density) { 140.dp.toPx() }) / gridRows.toFloat()

        val iconWidthPx = with(density) { 72.dp.toPx() }
        val iconHeightPx = with(density) { 72.dp.toPx() }

        // Long click anywhere on background canvas opens Carousel!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onOpenWorkspacePicker
                )
        ) {
            WorkspaceWallpaper(wallpaperUri = wallpaperUri)
        }

        // Top Status Bar Protection Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
        )

        // Freeform canvas with 4x6 grid alignment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp, bottom = 80.dp)
        ) {
            placedApps.forEach { whitelistedApp ->
                val appInfo = appMap[whitelistedApp.packageName]
                val displayName = appInfo?.name ?: whitelistedApp.packageName.substringAfterLast('.')

                var offsetX by remember(whitelistedApp.id, whitelistedApp.position.x) { mutableStateOf(whitelistedApp.position.x) }
                var offsetY by remember(whitelistedApp.id, whitelistedApp.position.y) { mutableStateOf(whitelistedApp.position.y) }
                var isDragging by remember { mutableStateOf(false) }

                val bitmap = remember(appInfo?.icon) {
                    try {
                        appInfo?.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .size(76.dp)
                        .graphicsLayer {
                            alpha = if (isDragging) 0.6f else 1.0f
                        }
                        .pointerInput(whitelistedApp.id, whitelistedApp.position.x, whitelistedApp.position.y) {
                            detectDragGestures(
                                onDragStart = {
                                    activeDraggingApp = whitelistedApp
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (isHoveringTrash) {
                                        pendingDeleteApp = whitelistedApp
                                    } else {
                                        val rawCol = ((offsetX + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                                        val rawRow = ((offsetY + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)

                                        val occupied = placedApps
                                            .filter { it.id != whitelistedApp.id }
                                            .map {
                                                val c = ((it.position.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                                                val r = ((it.position.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)
                                                GridCell(c, r)
                                            }.toSet()

                                        val finalCell = findNearestEmptyCell(rawCol, rawRow, gridCols, gridRows, occupied)
                                        val snappedX = finalCell.col * cellWidthPx + (cellWidthPx - iconWidthPx) / 2f
                                        val snappedY = finalCell.row * cellHeightPx + (cellHeightPx - iconHeightPx) / 2f

                                        offsetX = snappedX
                                        offsetY = snappedY

                                        if (workspaceId != null) {
                                            viewModel.updateAppPosition(workspaceId, whitelistedApp.packageName, snappedX, snappedY)
                                        }
                                    }
                                    activeDraggingApp = null
                                    isHoveringTrash = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                    activeDraggingApp = null
                                    isHoveringTrash = false
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                isHoveringTrash = offsetY < 100f
                            }
                        }
                        .combinedClickable(
                            onClick = {
                                viewModel.launchApp(whitelistedApp.packageName, context)
                            },
                            onLongClick = {
                                pendingDeleteApp = whitelistedApp
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = displayName,
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
                                    text = displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Small Floating Dustbin Icon at Top Center when dragging an app shortcut
        AnimatedVisibility(
            visible = activeDraggingApp != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            val trashBgColor by animateColorAsState(
                targetValue = if (isHoveringTrash) Color.Red.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.75f),
                label = "TrashBgColor"
            )
            val trashScale by animateFloatAsState(
                targetValue = if (isHoveringTrash) 1.25f else 1.0f,
                label = "TrashScale"
            )

            Surface(
                color = trashBgColor,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = trashScale
                        scaleY = trashScale
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("🗑️", fontSize = 24.sp)
                }
            }
        }

        // AssistiveTouch Floating Button
        Box(
            modifier = Modifier
                .offset { IntOffset(assistiveTouchOffset.x.roundToInt(), assistiveTouchOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            val targetX = if (assistiveTouchOffset.x < canvasWidthPx / 2f) 20f else canvasWidthPx - with(density) { 72.dp.toPx() }
                            assistiveTouchOffset = Offset(targetX, assistiveTouchOffset.y.coerceIn(100f, canvasHeightPx - 200f))
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        assistiveTouchOffset += dragAmount
                    }
                }
        ) {
            Surface(
                onClick = { showAssistiveMenu = true },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.size(52.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Assistive Touch Quick Menu Sheet
        if (showAssistiveMenu) {
            ModalBottomSheet(
                onDismissRequest = { showAssistiveMenu = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Workspace Controls",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        onClick = {
                            showAssistiveMenu = false
                            onOpenWorkspacePicker()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("💼", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Switch / Manage Workspaces", style = MaterialTheme.typography.titleMedium)
                                Text("Return to workspace carousel picker", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            showAssistiveMenu = false
                            onOpenAppDrawer()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("📱", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Whitelisted App Drawer", style = MaterialTheme.typography.titleMedium)
                                Text("Add or drag apps to home screen", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            showAssistiveMenu = false
                            showWallpaperSheet = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("🖼️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Change Wallpaper", style = MaterialTheme.typography.titleMedium)
                                Text("Select gradient preset or gallery photo", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Change Wallpaper Sheet
        if (showWallpaperSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWallpaperSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Change Workspace Background",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                            showWallpaperSheet = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("🖼️ Choose Photo from Gallery", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Preset Color Themes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DEFAULT_WALLPAPER_PRESETS) { preset ->
                            Card(
                                onClick = {
                                    if (workspaceId != null) {
                                        viewModel.updateWorkspaceWallpaper(workspaceId, preset.id)
                                    }
                                    showWallpaperSheet = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .size(width = 100.dp, height = 120.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(colors = preset.previewColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Delete App Confirmation Dialog
        pendingDeleteApp?.let { app ->
            val appInfo = appMap[app.packageName]
            val name = appInfo?.name ?: app.packageName.substringAfterLast('.')
            AlertDialog(
                onDismissRequest = { pendingDeleteApp = null },
                title = { Text("Remove App Shortcut") },
                text = { Text("Are you sure you want to remove '$name' from this home screen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onRemoveFromHomeScreen(app)
                            pendingDeleteApp = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteApp = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun WorkspaceWallpaper(
    wallpaperUri: String?,
    modifier: Modifier = Modifier
) {
    when {
        wallpaperUri.isNullOrEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E2A38), Color(0xFF111823))
                        )
                    )
            )
        }
        wallpaperUri == "preset://midnight_navy" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E2A38), Color(0xFF111823))
                        )
                    )
            )
        }
        wallpaperUri == "preset://dark_minimal" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E2C), Color(0xFF0F0F1A))
                        )
                    )
            )
        }
        wallpaperUri == "preset://gym_red" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF3A0007), Color(0xFF0D0002))
                        )
                    )
            )
        }
        wallpaperUri == "preset://ocean_blue" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF001F3F), Color(0xFF000814))
                        )
                    )
            )
        }
        wallpaperUri == "preset://calm_green" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B2B1A), Color(0xFF030E08))
                        )
                    )
            )
        }
        wallpaperUri == "preset://sunset_purple" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2B0B2B), Color(0xFF0E030E))
                        )
                    )
            )
        }
        else -> {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Workspace Wallpaper",
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

fun findNearestEmptyCell(
    targetCol: Int,
    targetRow: Int,
    cols: Int,
    rows: Int,
    occupiedCells: Set<GridCell>
): GridCell {
    if (GridCell(targetCol, targetRow) !in occupiedCells) {
        return GridCell(targetCol, targetRow)
    }

    var bestCell = GridCell(targetCol, targetRow)
    var minDistance = Float.MAX_VALUE

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val cell = GridCell(c, r)
            if (cell !in occupiedCells) {
                val dist = (c - targetCol) * (c - targetCol) + (r - targetRow) * (r - targetRow)
                if (dist.toFloat() < minDistance) {
                    minDistance = dist.toFloat()
                    bestCell = cell
                }
            }
        }
    }
    return BestCellOrTarget(bestCell, targetCol, targetRow)
}

private fun BestCellOrTarget(best: GridCell, fallbackCol: Int, fallbackRow: Int): GridCell {
    return best
}
