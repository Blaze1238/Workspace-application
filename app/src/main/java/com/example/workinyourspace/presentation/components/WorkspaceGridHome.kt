package com.example.workinyourspace.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceGridHome(
    workspaceName: String?,
    wallpaperUri: String?,
    whitelistedApps: List<WhitelistedApp>,
    installedApps: List<AppInfo>,
    viewModel: WorkspaceViewModel,
    onOpenConfiguration: () -> Unit,
    onRemoveFromHomeScreen: (WhitelistedApp) -> Unit,
    onSwipeUp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val appMap = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    // Filter apps that have been placed on the home screen canvas (coord_x >= 0f && coord_y >= 0f)
    val placedApps = remember(whitelistedApps) {
        whitelistedApps.filter { it.position.x >= 0f && it.position.y >= 0f }
    }

    // State for tracking active home screen drag and trash bin hover
    var activeDraggingApp by remember { mutableStateOf<WhitelistedApp?>(null) }
    var isHoveringTrash by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30) {
                        onSwipeUp()
                    }
                }
            }
    ) {
        if (!wallpaperUri.isNullOrEmpty()) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Workspace Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        // Top bar with workspace name and configuration button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
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

        // Freeform canvas for draggable shortcuts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 100.dp)
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
                        .size(84.dp)
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
                                        onRemoveFromHomeScreen(whitelistedApp)
                                    } else {
                                        viewModel.updateAppPosition(whitelistedApp.workspaceId, whitelistedApp.packageName, offsetX, offsetY)
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
                                isHoveringTrash = offsetY < 120f
                            }
                        }
                        .combinedClickable(
                            onClick = {
                                viewModel.launchApp(whitelistedApp.packageName, context)
                            },
                            onLongClick = {
                                onRemoveFromHomeScreen(whitelistedApp)
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
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Sliding Trash Bin Bar at Top when dragging an app shortcut
        AnimatedVisibility(
            visible = activeDraggingApp != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            val trashBgColor by animateColorAsState(
                targetValue = if (isHoveringTrash) Color.Red.copy(alpha = 0.9f) else Color.DarkGray.copy(alpha = 0.85f),
                label = "TrashBgColor"
            )

            Surface(
                color = trashBgColor,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("🗑️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHoveringTrash) "Release to Remove" else "Drag here to Remove",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom dock with configuration app & swipe-up handle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSwipeUp() }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe up for App Drawer",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.DarkGray.copy(alpha = 0.8f)
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
                            text = "Configuration App",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
