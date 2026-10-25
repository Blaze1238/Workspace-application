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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.presentation.WorkspaceViewModel
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspacePickerCarousel(
    workspaces: List<Workspace>,
    activeWorkspaceId: Int?,
    viewModel: WorkspaceViewModel,
    onSelectWorkspace: (Int?) -> Unit,
    onConfigureWhitelist: (Workspace) -> Unit,
    onCreateNewWorkspace: () -> Unit,
    onClosePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(workspaces) {
        listOf<Workspace?>(null) + workspaces
    }

    val initialIndex = remember(items, activeWorkspaceId) {
        val found = items.indexOfFirst { it?.id == activeWorkspaceId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )

    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
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
                            onSelectWorkspace(workspaceItem?.id)
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = targetTapScale
                            scaleY = targetTapScale
                            alpha = alphaFactor
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (workspaceItem?.id == activeWorkspaceId) MaterialTheme.colorScheme.primaryContainer
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
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (workspaceItem == null) "🏠" else "💼",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = workspaceItem?.name ?: "Default Home",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (workspaceItem?.id == activeWorkspaceId) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "● ACTIVE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (workspaceItem != null) {
                            Button(
                                onClick = { onConfigureWhitelist(workspaceItem) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🖼️ Whitelist & Wallpaper", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        } else {
                            Text(
                                text = "Minimal default layout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    onClick = onCreateNewWorkspace,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("+ Create New Workspace")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Workspace Picker Preview")
@Composable
fun WorkspacePickerCarouselPreview() {
    WorkInYourSpaceTheme {
        WorkspacePickerCarousel(
            workspaces = listOf(
                Workspace(id = 1, name = "Gym Mode", isDefault = false),
                Workspace(id = 2, name = "Study Mode", isDefault = false)
            ),
            activeWorkspaceId = 1,
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            onSelectWorkspace = {},
            onConfigureWhitelist = {},
            onCreateNewWorkspace = {},
            onClosePicker = {}
        )
    }
}
