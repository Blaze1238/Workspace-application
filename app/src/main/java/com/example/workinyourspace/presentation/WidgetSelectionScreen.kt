package com.example.workinyourspace.presentation

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import com.example.workinyourspace.presentation.components.GridCell
import com.example.workinyourspace.presentation.components.findNearestAvailableWidgetRegion

data class AppWidgetGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: android.graphics.drawable.Drawable?,
    val providers: List<AppWidgetProviderInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSelectionScreen(
    workspaceId: Int,
    workspaceWidgets: List<WorkspaceWidget>,
    placedApps: List<WhitelistedApp>,
    whitelistedApps: List<WhitelistedApp> = emptyList(),
    gridCols: Int,
    gridRows: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    appWidgetManager: AppWidgetManager,
    viewModel: WorkspaceViewModel,
    onBackClick: () -> Unit,
    onAddSystemWidget: (AppWidgetProviderInfo, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager

    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showWidgetSpaceErrorDialog by remember { mutableStateOf(false) }

    val whitelistedPackageNames = remember(whitelistedApps) {
        whitelistedApps.map { it.packageName }.toSet()
    }

    val appGroupsState = produceState<List<AppWidgetGroup>>(initialValue = emptyList(), key1 = whitelistedPackageNames) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allProviders = appWidgetManager.installedProviders
                val filteredProviders = allProviders.filter { providerInfo ->
                    val pkg = providerInfo.provider.packageName
                    if (pkg in whitelistedPackageNames) {
                        true
                    } else {
                        try {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val isSystemApp = (appInfo.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                            val isKnownSystemWidget = pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.") || pkg.contains("clock") || pkg.contains("weather")
                            isSystemApp || isKnownSystemWidget
                        } catch (e: Exception) {
                            false
                        }
                    }
                }

                filteredProviders.groupBy { it.provider.packageName }.map { (pkg, providers) ->
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
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    val appGroups = appGroupsState.value

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

    val handleWidgetSelection: (AppWidgetProviderInfo) -> Unit = { info ->
        val isAlreadyAdded = workspaceWidgets.any { it.providerClass == info.provider.className }
        if (isAlreadyAdded) {
            Toast.makeText(context, "This widget is already added to this workspace!", Toast.LENGTH_SHORT).show()
        } else {
            val (spanX, spanY) = WidgetUtils.calculateWidgetSpan(info, gridCols, gridRows)

            val occupied = mutableSetOf<GridCell>()
            placedApps.forEach { app ->
                val c = ((app.position.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                val r = ((app.position.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)
                occupied.add(GridCell(c, r))
            }
            workspaceWidgets.forEach { w ->
                val startCol = ((w.x + cellWidthPx / 2f) / cellWidthPx).toInt().coerceIn(0, gridCols - 1)
                val startRow = ((w.y + cellHeightPx / 2f) / cellHeightPx).toInt().coerceIn(0, gridRows - 1)
                for (dx in 0 until w.spanX) {
                    for (dy in 0 until w.spanY) {
                        occupied.add(GridCell(startCol + dx, startRow + dy))
                    }
                }
            }

            val maxVisibleRows = 6
            val availableCell = findNearestAvailableWidgetRegion(0, 0, spanX, spanY, gridCols, maxVisibleRows, occupied)
            if (availableCell == null) {
                showWidgetSpaceErrorDialog = true
            } else {
                val initX = availableCell.col * cellWidthPx + 8f
                val initY = availableCell.row * cellHeightPx + 8f
                onAddSystemWidget(info, initX, initY)
                onBackClick()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedAppPackage == null) "Add Widgets" else "Select Widget") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedAppPackage != null) {
                            selectedAppPackage = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Text("←")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("🔍 Search widgets...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            AnimatedContent(
                targetState = selectedAppPackage,
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { if (targetState != null) it else -it }, animationSpec = tween(300)) + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { if (targetState != null) -it else it }, animationSpec = tween(300)) + fadeOut(tween(300)))
                },
                label = "WidgetScreenTransition",
                modifier = Modifier.weight(1f)
            ) { pkg ->
                if (pkg == null) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(filteredGroups, key = { it.packageName }) { group ->
                            val bitmap = remember(group.appIcon) {
                                try {
                                    group.appIcon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            val singleProvider = if (group.providers.size == 1) group.providers.first() else null
                            val isSingleAdded = singleProvider?.let { p -> workspaceWidgets.any { it.providerClass == p.provider.className } } ?: false

                            Card(
                                onClick = {
                                    if (singleProvider != null) {
                                        handleWidgetSelection(singleProvider)
                                    } else {
                                        selectedAppPackage = group.packageName
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSingleAdded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
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
                                            text = if (isSingleAdded) "✓ Added" else "${group.providers.size} widget(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSingleAdded) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val group = appGroups.find { it.packageName == pkg }
                    val providers = group?.providers ?: emptyList()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(providers, key = { it.provider.className }) { info ->
                            val label = remember(info) { info.loadLabel(pm) }
                            val isAlreadyAdded = remember(info, workspaceWidgets) {
                                workspaceWidgets.any { it.providerClass == info.provider.className }
                            }
                            val (spanX, spanY) = remember(info) { WidgetUtils.calculateWidgetSpan(info, 4, 6) }

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
                                onClick = {
                                    handleWidgetSelection(info)
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlreadyAdded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
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
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (isAlreadyAdded) "✓ Added" else "${spanX}×${spanY}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAlreadyAdded) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWidgetSpaceErrorDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetSpaceErrorDialog = false },
            title = { Text("Cannot Add Widget") },
            text = { Text("There is not enough free space on your home screen grid to fit this widget. Please make room by removing items or moving icons.") },
            confirmButton = {
                Button(onClick = { showWidgetSpaceErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
