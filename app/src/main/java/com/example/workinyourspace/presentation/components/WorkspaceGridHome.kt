package com.example.workinyourspace.presentation.components

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.presentation.WallpaperCropScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
import kotlinx.coroutines.flow.flowOf
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
    onAddSystemWidget: () -> Unit = {},
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

    val workspaceWidgets by remember(workspaceId) {
        if (workspaceId != null) {
            viewModel.getWorkspaceWidgets(workspaceId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Dialog & Sheet States
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var showAssistiveMenu by remember { mutableStateOf(false) }
    var showFuntouchWidgetSheet by remember { mutableStateOf(false) }
    var pendingDeleteApp by remember { mutableStateOf<WhitelistedApp?>(null) }
    var selectedWidgetForAction by remember { mutableStateOf<WorkspaceWidget?>(null) }

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

    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181A20))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenWorkspacePicker()
                    }
                )
            }
            .nestedScroll(nestedScrollConnection)
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }

        // Default AssistiveTouch position to Top Right
        var assistiveTouchOffset by remember { mutableStateOf(Offset(canvasWidthPx - with(density) { 72.dp.toPx() }, 100f)) }

        val gridCols = 4
        val maxPlacedRow = remember(placedApps, workspaceWidgets) {
            val appMax = placedApps.maxOfOrNull { ((it.position.y + 50f) / (canvasHeightPx / 6f)).toInt() } ?: 5
            val widgetMax = workspaceWidgets.maxOfOrNull { ((it.y + 50f) / (canvasHeightPx / 6f)).toInt() + it.spanY } ?: 5
            max(appMax, widgetMax)
        }
        val gridRows = max(6, maxPlacedRow + 2)

        val cellWidthPx = canvasWidthPx / gridCols.toFloat()
        val cellHeightPx = (canvasHeightPx - with(density) { 140.dp.toPx() }) / 6f

        val iconWidthPx = with(density) { 72.dp.toPx() }
        val iconHeightPx = with(density) { 72.dp.toPx() }

        val scrollState = rememberScrollState()

        // Background Wallpaper Canvas
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
        ) {
            // Render Placed Smoothly Movable Widgets
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
                        .pointerInput(widget.id) {
                            detectDragGestures(
                                onDragStart = { isWidgetDragging = true },
                                onDragEnd = {
                                    isWidgetDragging = false
                                    val rawCol = ((widgetX + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - widget.spanX)
                                    val rawRow = ((widgetY + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - widget.spanY)

                                    val snappedX = rawCol * cellWidthPx + 8f
                                    val snappedY = rawRow * cellHeightPx + 8f

                                    widgetX = snappedX
                                    widgetY = snappedY

                                    if (workspaceId != null) {
                                        viewModel.updateWidgetPosition(widget.id, snappedX, snappedY)
                                    }
                                },
                                onDragCancel = { isWidgetDragging = false }
                            ) { change, dragAmount ->
                                change.consume()
                                widgetX += dragAmount.x
                                widgetY += dragAmount.y
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

            // Render Placed App Shortcuts
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
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val rawCol = ((offsetX + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                                    val rawRow = ((offsetY + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)

                                    val occupied = placedApps
                                        .filter { it.id != whitelistedApp.id }
                                        .map {
                                            val c = ((it.position.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                                            val r = ((it.position.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)
                                            GridCell(c, r)
                                        }.toMutableSet()

                                    workspaceWidgets.forEach { w ->
                                        val wCol = ((w.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                                        val wRow = ((w.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)
                                        for (r in wRow until (wRow + w.spanY)) {
                                            for (c in wCol until (wCol + w.spanX)) {
                                                occupied.add(GridCell(c.coerceIn(0, gridCols - 1), r.coerceIn(0, gridRows - 1)))
                                            }
                                        }
                                    }

                                    val finalCell = findNearestEmptyCell(rawCol, rawRow, gridCols, gridRows, occupied)
                                    val snappedX = finalCell.col * cellWidthPx + (cellWidthPx - iconWidthPx) / 2f
                                    val snappedY = finalCell.row * cellHeightPx + (cellHeightPx - iconHeightPx) / 2f

                                    offsetX = snappedX
                                    offsetY = snappedY

                                    if (workspaceId != null) {
                                        viewModel.updateAppPosition(workspaceId, whitelistedApp.packageName, snappedX, snappedY)
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
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
                            showFuntouchWidgetSheet = true
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
                            Text("🧩", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Widgets", style = MaterialTheme.typography.titleMedium)
                                Text("Add clock, weather, calendar, search widgets", style = MaterialTheme.typography.bodySmall)
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
                                Text("Workspace Wallpaper", style = MaterialTheme.typography.titleMedium)
                                Text("Pick custom gallery image or gradient theme", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Clubbed App-Based Widget Selector Sheet (Funtouch OS Style Two-Level UI)
        if (showFuntouchWidgetSheet && appWidgetManager != null && workspaceId != null) {
            ModalBottomSheet(
                onDismissRequest = { showFuntouchWidgetSheet = false }
            ) {
                ClubbedAppWidgetSelectorSheet(
                    appWidgetManager = appWidgetManager,
                    onSelectWidgetProvider = { providerInfo ->
                        showFuntouchWidgetSheet = false
                        onAddSystemWidget()
                    }
                )
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
                        items(DEFAULT_WALLPAPER_PRESETS) { preset ->
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

        // Delete App Confirmation Dialog
        pendingDeleteApp?.let { app ->
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

        // Delete Widget Confirmation Dialog
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

data class AppWidgetGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: android.graphics.drawable.Drawable?,
    val providers: List<AppWidgetProviderInfo>
)

@Composable
fun ClubbedAppWidgetSelectorSheet(
    appWidgetManager: AppWidgetManager,
    onSelectWidgetProvider: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val installedProviders = remember {
        try {
            appWidgetManager.installedProviders
        } catch (e: Exception) {
            emptyList<AppWidgetProviderInfo>()
        }
    }

    val appGroups = remember(installedProviders) {
        installedProviders.groupBy { it.provider.packageName }.map { (pkg, providers) ->
            val first = providers.first()
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                first.loadLabel(pm).toString()
            }
            val appIcon = try {
                pm.getApplicationIcon(pkg)
            } catch (e: Exception) {
                first.loadIcon(context, 0)
            }
            AppWidgetGroup(
                packageName = pkg,
                appLabel = appLabel,
                appIcon = appIcon,
                providers = providers
            )
        }.sortedBy { it.appLabel.lowercase() }
    }

    val filteredGroups = remember(appGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            appGroups
        } else {
            val q = searchQuery.lowercase().trim()
            appGroups.filter {
                it.appLabel.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedAppPackage == null) "Widgets" else "App Widgets",
                style = MaterialTheme.typography.titleLarge
            )
            if (selectedAppPackage != null) {
                TextButton(onClick = { selectedAppPackage = null }) {
                    Text("← Back to Apps")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("🔍 Search...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        AnimatedContent(
            targetState = selectedAppPackage,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300)))
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)))
                }
            },
            label = "WidgetLevelTransition",
            modifier = Modifier.weight(1f)
        ) { pkg ->
            if (pkg == null) {
                // Level 1: Clubbed App Cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredGroups, key = { it.packageName }) { group ->
                        val bitmap = remember(group.appIcon) {
                            try {
                                group.appIcon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Card(
                            onClick = { selectedAppPackage = group.packageName },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = group.appLabel,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(group.appLabel.take(1).uppercase())
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = group.appLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${group.providers.size} widget(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Level 2: Widgets belonging to the selected app
                val group = appGroups.find { it.packageName == pkg }
                val providers = group?.providers ?: emptyList()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(providers, key = { it.provider.className }) { info ->
                        val label = remember(info) { info.loadLabel(pm) }
                        val spanX = ((info.minWidth + 70) / 80).coerceIn(1, 4)
                        val spanY = ((info.minHeight + 70) / 80).coerceIn(1, 4)

                        val previewDrawable = remember(info) {
                            try {
                                info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val previewBitmap = remember(previewDrawable) {
                            try {
                                previewDrawable?.toBitmap(width = 120, height = 120)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Card(
                            onClick = { onSelectWidgetProvider(info) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewBitmap != null) {
                                        Image(
                                            bitmap = previewBitmap,
                                            contentDescription = label,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    } else {
                                        Text("🧩", fontSize = 32.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$spanX × $spanY",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                    val candidate = GridCell(c, r)
                    if (candidate !in occupied) {
                        return candidate
                    }
                }
            }
        }
        radius++
    }
    return GridCell(targetCol, targetRow)
}
