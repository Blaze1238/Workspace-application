package com.example.workinyourspace.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.workinyourspace.data.repository.SessionManager
import com.example.workinyourspace.data.repository.WorkspaceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerService : AccessibilityService(){

    @Inject
    lateinit var workspaceRepo : WorkspaceRepository

    @Inject
    lateinit var sessionMan : SessionManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if(packageName == "com.android.systemui" || packageName == this.packageName) return

        Log.d("InterceptionEngine","App trying to launch -> $packageName")

        serviceScope.launch {
            val workspaceID = sessionMan.getActiveWorkspaceID() ?: return@launch

            val whitelistedApps = workspaceRepo.getWhitelistedApps(workspaceID).first()

            val isAllowed = whitelistedApps.any{it.packageName == packageName}

            if(!isAllowed){
                Log.e("InterceptEngine","App is blacklisted. Forcefully opening home")

                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)
            }

        }
    }

    override fun onInterrupt() {
        serviceScope.cancel()
        Log.e("InterceptionEngine","Interrupted by OS")
    }
}