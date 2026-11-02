package com.example.workinyourspace.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
class NotificationBlockerService : NotificationListenerService(){

    @Inject
    lateinit var sessionMan : SessionManager

    @Inject
    lateinit var workspaceRepo : WorkspaceRepository

    private val notifCheckCoroutine = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        Log.d("NotificationBlocker", "Notification posted from package -> $packageName")

        if (packageName == applicationContext.packageName) return
        if (sbn.isOngoing) return

        notifCheckCoroutine.launch {
            try {
                if (!::sessionMan.isInitialized || !::workspaceRepo.isInitialized) {
                    Log.e("NotificationBlocker", "Hilt dependencies not yet initialized")
                    return@launch
                }

                val currentWorkspaceID = sessionMan.getActiveWorkspaceID()
                if (currentWorkspaceID == null) {
                    Log.d("NotificationBlocker", "No active workspace session. Allowing notification.")
                    return@launch
                }

                val allowedApps = workspaceRepo.getWhitelistedApps(currentWorkspaceID).first()

                val isAllowed = allowedApps.any{ it.packageName == packageName }

                if (!isAllowed) {
                    cancelNotification(sbn.key)
                    Log.d("NotificationBlocker", "Successfully blocked and canceled notification from blacklisted app -> $packageName")
                } else {
                    Log.d("NotificationBlocker", "Notification from whitelisted app allowed -> $packageName")
                }
            } catch (e: Exception) {
                Log.e("NotificationBlocker", "Error handling notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notifCheckCoroutine.cancel()
    }
}
