package com.example.workinyourspace.domain.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.example.workinyourspace.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppRepository @Inject constructor(@ApplicationContext private val context : Context){
    suspend fun getInstalledApps() : List<AppInfo> = withContext(Dispatchers.IO){
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN,null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveList : List<ResolveInfo> = packageManager.queryIntentActivities(intent,0)

        resolveList.map{
            resolve ->
            AppInfo(
                name = resolve.loadLabel(packageManager).toString(),
                icon = resolve.loadIcon(packageManager),
                packageName = resolve.activityInfo.packageName
            )
        }.sortedBy { it.name.lowercase() }

    }
}