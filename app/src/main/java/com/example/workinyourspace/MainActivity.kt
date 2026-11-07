package com.example.workinyourspace

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.workinyourspace.presentation.MainLauncherScreen
import com.example.workinyourspace.presentation.PermissionOnboardingScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
import com.example.workinyourspace.presentation.components.isAccessibilityServiceEnabled
import com.example.workinyourspace.presentation.components.isNotificationListenerEnabled
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme
import dagger.hilt.android.AndroidEntryPoint

const val APPWIDGET_HOST_ID = 1024

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WorkspaceViewModel by viewModels()
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetHost = AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        setContent {
            WorkInYourSpaceTheme {
                val context = applicationContext
                var hasPermissions by remember {
                    mutableStateOf(
                        isAccessibilityServiceEnabled(context) && isNotificationListenerEnabled(context)
                    )
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPermissions = isAccessibilityServiceEnabled(context) && isNotificationListenerEnabled(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (hasPermissions) {
                    MainLauncherScreen(
                        viewModel = viewModel,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PermissionOnboardingScreen(
                        onPermissionsGranted = {
                            hasPermissions = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshActiveWorkspace()
    }
}
