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
class NotificationBlockerService :  NotificationListenerService(){

    @Inject
    lateinit var sessionMan : SessionManager

    @Inject
    lateinit var workspaceRepo : WorkspaceRepository

    private val notifCheckCoroutine  = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        if(packageName == applicationContext.packageName) return

        if(sbn.isOngoing) return

        notifCheckCoroutine.launch {
            val currentWorkspaceID = sessionMan.getActiveWorkspaceID() ?: return@launch

            val allowedApps = workspaceRepo.getWhitelistedApps(currentWorkspaceID).first()

            val isAllowed = allowedApps.any{ it.packageName == packageName }

            if(!isAllowed){
                cancelNotification(sbn.key)
                Log.d("NotificationBlocker", "Blocked notification from blacklisted app -> $packageName")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notifCheckCoroutine.cancel()
    }
}