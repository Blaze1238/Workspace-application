package com.example.workinyourspace.presentation.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.WallpaperCropScreen
import com.example.workinyourspace.presentation.WidgetSelectionScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    onAddSystemWidget: (AppWidgetProviderInfo, Float, Float) -> Unit = { _, _, _ -> },
    onOpenWidgetScreen: () -> Unit = {},
    onOpenWorkspacePicker: () -> Unit,
    onOpenConfiguration: () -> Unit,
    onRemoveFromHomeScreen: (WhitelistedApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val workspaceWidgets by remember(workspaceId) {
        if (workspaceId != null) {
            viewModel.getWorkspaceWidgets(workspaceId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val appMap = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val placedApps = remember(whitelistedApps) {
        whitelistedApps.filter { it.position.x >= 0f && it.position.y >= 0f }
    }

    var activeDraggingApp by remember { mutableStateOf<WhitelistedApp?>(null) }
    var activeDraggingWidget by remember { mutableStateOf<WorkspaceWidget?>(null) }
    var isHoveringTrash by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var showAssistiveMenu by remember { mutableStateOf(false) }
    var showWorkspaceEditMenu by remember { mutableStateOf(false) }
    var showWidgetSelectionScreen by remember { mutableStateOf(false) }
    var selectedWidgetForAction by remember { mutableStateOf<WorkspaceWidget?>(null) }
    var pendingDeleteApp by remember { mutableStateOf<WhitelistedApp?>(null) }

    // Wallpaper Crop & Zoom State
    var croppingImageUri by remember { mutableStateOf<Uri?>(null) }

    croppingImageUri?.let { uri ->
        if (workspaceId != null) {
            WallpaperCropScreen(
                workspaceId = workspaceId,
                imageUri = uri,
                viewModel = viewModel,
                onComplete = { croppingImageUri = null }
            )
            return
        }
    }

    var wallpaperScale by remember { mutableFloatStateOf(1.0f) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            croppingImageUri = it
            showWallpaperSheet = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -20f) {
                    onOpenAppDrawer()
                }
                return Offset.Zero
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181A20))
            .nestedScroll(nestedScrollConnection)
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        var assistiveTouchOffset by remember { mutableStateOf(Offset(canvasWidthPx - with(density) { 72.dp.toPx() }, 100f)) }

        val gridCols = 4
        val maxVisibleRows = 6

        val cellWidthPx = canvasWidthPx / gridCols.toFloat()
        val cellHeightPx = (canvasHeightPx - with(density) { 140.dp.toPx() }) / 6f

        val iconWidthPx = with(density) { 72.dp.toPx() }
        val iconHeightPx = with(density) { 72.dp.toPx() }

        if (showWidgetSelectionScreen && appWidgetManager != null && workspaceId != null) {
            WidgetSelectionScreen(
                workspaceId = workspaceId,
                workspaceWidgets = workspaceWidgets,
                placedApps = placedApps,
                gridCols = gridCols,
                gridRows = maxVisibleRows,
                cellWidthPx = cellWidthPx,
                cellHeightPx = cellHeightPx,
                appWidgetManager = appWidgetManager,
                viewModel = viewModel,
                onBackClick = { showWidgetSelectionScreen = false },
                onAddSystemWidget = { providerInfo, initX, initY ->
                    showWidgetSelectionScreen = false
                    onAddSystemWidget(providerInfo, initX, initY)
                },
                modifier = Modifier.fillMaxSize()
            )
            return@BoxWithConstraints
        }

        val scrollState = rememberScrollState()

        val haptic = LocalHapticFeedback.current
        var isLongPressingCanvas by remember { mutableStateOf(false) }
        val canvasScale by animateFloatAsState(
            targetValue = if (isLongPressingCanvas) 0.96f else 1.0f,
            animationSpec = tween(150),
            label = "CanvasLongPressScale"
        )

        val smoothCanvasScale by animateFloatAsState(
            targetValue = if (showWorkspaceEditMenu) 0.82f else canvasScale,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "SmoothCanvasScale"
        )

        // Unified Scaling Canvas (Wallpaper + Status Bar + Grid Canvas + Apps + Widgets Scale Smoothly Together)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = smoothCanvasScale
                    scaleY = smoothCanvasScale
                }
        ) {
            // Background Wallpaper Box
            Box(modifier = Modifier.fillMaxSize()) {
                WorkspaceWallpaper(
                    wallpaperUri = wallpaperUri,
                    scale = wallpaperScale
                )
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

            // Multi-Row Vertical Scrolling Grid Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 80.dp)
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (showWorkspaceEditMenu) {
                                    showWorkspaceEditMenu = false
                                } else {
                                    isLongPressingCanvas = true
                                    tryAwaitRelease()
                                    isLongPressingCanvas = false
                                }
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isLongPressingCanvas = false
                                showWorkspaceEditMenu = true
                            }
                        )
                    }
            ) {
                // Render Placed Widgets with Strict 4x6 Grid Boundary Validation & Revert on Collision
                workspaceWidgets.forEach { widget ->
                    var widgetX by remember(widget.id, widget.x) { mutableFloatStateOf(widget.x) }
                    var widgetY by remember(widget.id, widget.y) { mutableFloatStateOf(widget.y) }
                    var isWidgetDragging by remember { mutableStateOf(false) }

                    val wWidthPx = widget.spanX * cellWidthPx - 16f
                    val wHeightPx = widget.spanY * cellHeightPx - 16f

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(widgetX.roundToInt(), widgetY.roundToInt()) }
                            .size(with(density) { wWidthPx.toDp() }, with(density) { wHeightPx.toDp() })
                            .graphicsLayer { alpha = if (isWidgetDragging) 0.7f else 1.0f }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray.copy(alpha = 0.5f))
                            .pointerInput(widget.id, widget.x, widget.y) {
                                detectDragGestures(
                                    onDragStart = {
                                        activeDraggingWidget = widget
                                        isWidgetDragging = true
                                    },
                                    onDragEnd = {
                                        isWidgetDragging = false
                                        if (isHoveringTrash) {
                                            viewModel.deleteWidgetById(widget.id)
                                            appWidgetHost?.deleteAppWidgetId(widget.appWidgetId)
                                        } else {
                                            val rawCol = (widgetX / cellWidthPx).roundToInt().coerceIn(0, gridCols - widget.spanX)
                                            val rawRow = (widgetY / cellHeightPx).roundToInt().coerceIn(0, maxVisibleRows - widget.spanY)

                                            val occupied = mutableSetOf<GridCell>()
                                            placedApps.forEach { app ->
                                                val c = (app.position.x / cellWidthPx).roundToInt()
                                                val r = (app.position.y / cellHeightPx).roundToInt()
                                                if (c in 0 until gridCols && r in 0 until maxVisibleRows) {
                                                    occupied.add(GridCell(c, r))
                                                }
                                            }
                                            workspaceWidgets.filter { it.id != widget.id }.forEach { w ->
                                                val startCol = (w.x / cellWidthPx).roundToInt()
                                                val startRow = (w.y / cellHeightPx).roundToInt()
                                                for (dx in 0 until w.spanX) {
                                                    for (dy in 0 until w.spanY) {
                                                        val c = startCol + dx
                                                        val r = startRow + dy
                                                        if (c in 0 until gridCols && r in 0 until maxVisibleRows) {
                                                            occupied.add(GridCell(c, r))
                                                        }
                                                    }
                                                }
                                            }

                                            if (canFitRegion(rawCol, rawRow, widget.spanX, widget.spanY, gridCols, maxVisibleRows, occupied)) {
                                                val snappedX = rawCol * cellWidthPx + 8f
                                                val snappedY = rawRow * cellHeightPx + 8f
                                                widgetX = snappedX
                                                widgetY = snappedY
                                                viewModel.updateWidgetPosition(widget.id, snappedX, snappedY)
                                            } else {
                                                widgetX = widget.x
                                                widgetY = widget.y
                                                Toast.makeText(context, "Out of bounds or occupied! Reverting position.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        activeDraggingWidget = null
                                        isHoveringTrash = false
                                    },
                                    onDragCancel = {
                                        isWidgetDragging = false
                                        widgetX = widget.x
                                        widgetY = widget.y
                                        activeDraggingWidget = null
                                        isHoveringTrash = false
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    widgetX += dragAmount.x
                                    widgetY += dragAmount.y
                                    val trashCenterX = canvasWidthPx / 2f
                                    val itemCenterX = widgetX + wWidthPx / 2f
                                    val itemCenterY = widgetY
                                    isHoveringTrash = abs(itemCenterX - trashCenterX) < 100f && itemCenterY < 120f
                                }
                            }
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedWidgetForAction = widget
                                }
                            )
                    ) {
                        if (appWidgetHost != null && appWidgetManager != null) {
                            AndroidView(
                                factory = { ctx ->
                                    val info = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                                    if (info != null) {
                                        appWidgetHost.createView(ctx, widget.appWidgetId, info).apply {
                                            setAppWidget(widget.appWidgetId, info)
                                        }
                                    } else {
                                        android.view.View(ctx)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "🧩 Widget #${widget.appWidgetId} (${widget.spanX}×${widget.spanY})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Render Placed App Shortcuts with Strict Grid Snapping
                placedApps.forEach { whitelistedApp ->
                    val appInfo = appMap[whitelistedApp.packageName]
                    val displayName = appInfo?.name ?: whitelistedApp.packageName.substringAfterLast('.')

                    var offsetX by remember(whitelistedApp.id, whitelistedApp.position.x) { mutableFloatStateOf(whitelistedApp.position.x) }
                    var offsetY by remember(whitelistedApp.id, whitelistedApp.position.y) { mutableFloatStateOf(whitelistedApp.position.y) }
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
                                            val rawCol = (offsetX / cellWidthPx).roundToInt().coerceIn(0, gridCols - 1)
                                            val rawRow = (offsetY / cellHeightPx).roundToInt().coerceIn(0, maxVisibleRows - 1)

                                            val occupied = mutableSetOf<GridCell>()
                                            placedApps.filter { it.id != whitelistedApp.id }.forEach { app ->
                                                val c = (app.position.x / cellWidthPx).roundToInt()
                                                val r = (app.position.y / cellHeightPx).roundToInt()
                                                if (c in 0 until gridCols && r in 0 until maxVisibleRows) {
                                                    occupied.add(GridCell(c, r))
                                                }
                                            }
                                            workspaceWidgets.forEach { w ->
                                                val startCol = (w.x / cellWidthPx).roundToInt()
                                                val startRow = (w.y / cellHeightPx).roundToInt()
                                                for (dx in 0 until w.spanX) {
                                                    for (dy in 0 until w.spanY) {
                                                        val c = startCol + dx
                                                        val r = startRow + dy
                                                        if (c in 0 until gridCols && r in 0 until maxVisibleRows) {
                                                            occupied.add(GridCell(c, r))
                                                        }
                                                    }
                                                }
                                            }

                                            val finalCell = findNearestEmptyCell(rawCol, rawRow, gridCols, maxVisibleRows, occupied)
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
                                    val trashCenterX = canvasWidthPx / 2f
                                    val itemCenterX = offsetX + iconWidthPx / 2f
                                    val itemCenterY = offsetY
                                    isHoveringTrash = abs(itemCenterX - trashCenterX) < 100f && itemCenterY < 120f
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
        }

        // Workspace Edit Menu Overlay (Appears on Long Press with Smooth Slide & Scale Animation)
        AnimatedVisibility(
            visible = showWorkspaceEditMenu,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 350)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Widgets Button Card
                Surface(
                    onClick = {
                        showWorkspaceEditMenu = false
                        showWidgetSelectionScreen = true
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("🧩", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Widgets", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Workspaces Button Card
                Surface(
                    onClick = {
                        showWorkspaceEditMenu = false
                        onOpenWorkspacePicker()
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("💼", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Workspaces", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Small Floating Dustbin Icon at Top Center when dragging an app shortcut or widget
        AnimatedVisibility(
            visible = activeDraggingApp != null || activeDraggingWidget != null,
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

        // AssistiveTouch Floating Button (Defaults to Top Right)
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
                                Text("Workspace App Drawer", style = MaterialTheme.typography.titleMedium)
                                Text("Add whitelisted apps to home screen", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            showAssistiveMenu = false
                            showWidgetSelectionScreen = true
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
                            Text("🧩", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Add System Widget", style = MaterialTheme.typography.titleMedium)
                                Text("Add clock, weather, calendar widgets", style = MaterialTheme.typography.bodySmall)
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
                        color = MaterialTheme.colorScheme.tertiaryContainer,
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
                                Text("Workspace Wallpaper", style = MaterialTheme.typography.titleMedium)
                                Text("Pick custom gallery image or gradient theme", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionsPrompt(modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Wallpaper Picker & Presets Sheet
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
                        text = "Workspace Wallpaper",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("📷 Pick Image from Gallery")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Color & Gradient Presets",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DEFAULT_WALLPAPER_PRESETS, key = { it.id }) { preset ->
                            Surface(
                                onClick = {
                                    if (workspaceId != null) {
                                        viewModel.updateWorkspaceWallpaper(workspaceId, preset.id)
                                    }
                                    showWallpaperSheet = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = preset.previewColors.first(),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier.size(80.dp, 100.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.BottomCenter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(preset.previewColors)
                                        )
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirm Shortcut Delete Dialog
        pendingDeleteApp?.let { app ->
            let {
                val appInfo = appMap[app.packageName]
                val displayName = appInfo?.name ?: app.packageName.substringAfterLast('.')

                AlertDialog(
                    onDismissRequest = { pendingDeleteApp = null },
                    title = { Text("Remove Shortcut?") },
                    text = { Text("Remove '$displayName' from home screen? It will remain available in your workspace App Drawer.") },
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

        // Confirm Widget Delete Dialog
        selectedWidgetForAction?.let { widget ->
            AlertDialog(
                onDismissRequest = { selectedWidgetForAction = null },
                title = { Text("Remove Widget?") },
                text = { Text("Are you sure you want to remove this widget from your workspace?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWidgetById(widget.id)
                            appWidgetHost?.deleteAppWidgetId(widget.appWidgetId)
                            selectedWidgetForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove Widget")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedWidgetForAction = null }) {
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
    scale: Float = 1.0f
) {
    if (!wallpaperUri.isNullOrEmpty()) {
        if (wallpaperUri.startsWith("preset://")) {
            val preset = DEFAULT_WALLPAPER_PRESETS.find { it.id == wallpaperUri } ?: DEFAULT_WALLPAPER_PRESETS.first()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        Brush.verticalGradient(preset.previewColors)
                    )
            )
        } else {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Workspace Custom Wallpaper",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF181A20))
        )
    }
}

fun findNearestAvailableWidgetRegion(
    targetCol: Int,
    targetRow: Int,
    spanX: Int,
    spanY: Int,
    maxCols: Int,
    maxRows: Int,
    occupied: Set<GridCell>
): GridCell? {
    if (canFitRegion(targetCol, targetRow, spanX, spanY, maxCols, maxRows, occupied)) {
        return GridCell(targetCol, targetRow)
    }

    var radius = 1
    while (radius < maxOf(maxCols, maxRows)) {
        for (r in targetRow - radius..targetRow + radius) {
            for (c in targetCol - radius..targetCol + radius) {
                if (canFitRegion(c, r, spanX, spanY, maxCols, maxRows, occupied)) {
                    return GridCell(c, r)
                }
            }
        }
        radius++
    }
    return null
}

fun canFitRegion(
    startCol: Int,
    startRow: Int,
    spanX: Int,
    spanY: Int,
    maxCols: Int,
    maxRows: Int,
    occupied: Set<GridCell>
): Boolean {
    val maxVisibleRows = 6
    val effectiveMaxRows = minOf(6, maxRows)
    if (startCol < 0 || startRow < 0 || startCol + spanX > maxCols || startRow + spanY > effectiveMaxRows) {
        return false
    }
    for (dx in 0 until spanX) {
        for (dy in 0 until spanY) {
            if (GridCell(startCol + dx, startRow + dy) in occupied) {
                return false
            }
        }
    }
    return true
}

fun findNearestEmptyCell(
    targetCol: Int,
    targetRow: Int,
    maxCols: Int,
    maxRows: Int,
    occupied: Set<GridCell>
): GridCell {
    if (GridCell(targetCol, targetRow) !in occupied) {
        return GridCell(targetCol, targetRow)
    }

    var radius = 1
    while (radius < maxOf(maxCols, maxRows)) {
        for (r in targetRow - radius..targetRow + radius) {
            for (c in targetCol - radius..targetCol + radius) {
                if (c in 0 until maxCols && r in 0 until maxRows) {
                    val coordinate = GridCell(c, r)
                    if (coordinate !in occupied) {
                        return coordinate
                    }
                }
            }
        }
        radius++
    }
    return GridCell(targetCol, targetRow)
}
