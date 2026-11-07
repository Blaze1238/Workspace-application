package com.example.workinyourspace.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.workinyourspace.presentation.components.isAccessibilityServiceEnabled
import com.example.workinyourspace.presentation.components.isNotificationListenerEnabled
import com.example.workinyourspace.presentation.components.openAccessibilitySettings
import com.example.workinyourspace.presentation.components.openNotificationAccessSettings

@Composable
fun PermissionOnboardingScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isNotificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var showExitExplanationDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = isAccessibilityServiceEnabled(context)
                isNotificationGranted = isNotificationListenerEnabled(context)
                if (isAccessibilityGranted && isNotificationGranted) {
                    onPermissionsGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121418))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🛡️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WorkInYourSpace is a dedicated focus launcher. To enforce your workspace whitelists and block distractions, OS-level Accessibility and Notification Access are mandatory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Accessibility Status Item
                PermissionRowItem(
                    title = "App Blocker (Accessibility)",
                    isGranted = isAccessibilityGranted,
                    onGrantClick = { openAccessibilitySettings(context) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notification Status Item
                PermissionRowItem(
                    title = "Notification Blocker Access",
                    isGranted = isNotificationGranted,
                    onGrantClick = { openNotificationAccessSettings(context) }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (!isAccessibilityGranted) {
                            openAccessibilitySettings(context)
                        } else if (!isNotificationGranted) {
                            openNotificationAccessSettings(context)
                        }
                    },
                    enabled = !isAccessibilityGranted || !isNotificationGranted,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (!isAccessibilityGranted) "Open Accessibility Settings" else "Open Notification Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { showExitExplanationDialog = true }
                ) {
                    Text("Exit App", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showExitExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExitExplanationDialog = false },
            title = { Text("Permissions Required") },
            text = { Text("Without Accessibility and Notification Access, WorkInYourSpace cannot block distractions or enforce your workspaces. The app cannot run without these core security permissions.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitExplanationDialog = false
                        activity?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitExplanationDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (isGranted) {
                Text(
                    text = "✓ Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}
