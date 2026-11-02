package com.example.workinyourspace.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperCropScreen(
    workspaceId: Int,
    imageUri: Uri,
    viewModel: WorkspaceViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    val graphicsLayer = rememberGraphicsLayer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adjust & Crop Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Text("✕")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zoom: ${(scale * 100).toInt()}%", color = Color.White)
                        TextButton(onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }) {
                            Text("Reset")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val bitmap = graphicsLayer.toImageBitmap()
                                    val wallpapersDir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
                                    wallpapersDir.listFiles()?.filter { it.name.startsWith("workspace_wallpaper_${workspaceId}_") }?.forEach { it.delete() }

                                    val destFile = File(wallpapersDir, "workspace_wallpaper_${workspaceId}_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(destFile).use { output ->
                                        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 92, output)
                                    }

                                    val savedFileUri = Uri.fromFile(destFile).toString()
                                    viewModel.updateWorkspaceWallpaper(workspaceId, savedFileUri)
                                    onComplete()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    onComplete()
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Set as Workspace Wallpaper")
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            // Aspect-ratio framing container for wallpaper (obeying 9:16 phone ratio)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    }
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Crop Wallpaper",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(transformState),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
