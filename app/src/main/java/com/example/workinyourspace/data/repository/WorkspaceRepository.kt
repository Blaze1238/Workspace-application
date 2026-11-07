package com.example.workinyourspace.data.repository

import android.content.Context
import android.util.Log
import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.dao.WorkspaceWidgetDAO
import com.example.workinyourspace.data.local.entity.FreeformPosition
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkspaceRepository @Inject constructor(
    private val workspaceDAO : WorkspaceDAO,
    private val whitelistedDAO : WhitelistedAppsDAO,
    private val widgetDAO : WorkspaceWidgetDAO,
    @ApplicationContext private val context: Context
){
    fun getWorkspaces() : Flow<List<Workspace>>{
        return workspaceDAO.getAllWorkspaces()
    }

    fun getWhitelistedApps(workspaceID : Int) : Flow<List<WhitelistedApp>>{
        return whitelistedDAO.getWhitelistedApps(workspaceID)
    }

    fun getWorkspaceWidgets(workspaceID: Int) : Flow<List<WorkspaceWidget>> {
        return widgetDAO.getWorkspaceWidgets(workspaceID)
    }

    suspend fun addWidget(widget: WorkspaceWidget) {
        widgetDAO.addWidget(widget)
    }

    suspend fun deleteWidgetById(widgetId: Int) {
        widgetDAO.deleteWidgetById(widgetId)
    }

    suspend fun updateWidgetPosition(widgetId: Int, x: Float, y: Float) {
        widgetDAO.updateWidgetPosition(widgetId, x, y)
    }

    suspend fun addWorkspace(workspace: Workspace){
        workspaceDAO.addWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspace : Workspace){
        workspaceDAO.deleteWorkspace(workspace)
    }

    suspend fun removeWhitelistedApp(whitelistedApp : WhitelistedApp){
        whitelistedDAO.deleteWhiteListedApp(whitelistedApp)
    }

    suspend fun removeWhitelistedAppByPackageName(workspaceID: Int, packageName: String){
        whitelistedDAO.removeByPackageName(workspaceID,packageName)
    }

    suspend fun addWhitelistedApp(whitelistedApp : WhitelistedApp){
        whitelistedDAO.addWhiteListedApp(whitelistedApp)
    }

    suspend fun deleteWhitelistedApp(whitelistedApp : WhitelistedApp){
        whitelistedDAO.deleteWhiteListedApp(whitelistedApp)
    }

    suspend fun getAllWidgets(): List<WorkspaceWidget> = withContext(Dispatchers.IO) {
        widgetDAO.getAllWidgets()
    }

    suspend fun createDefaultHomeWorkspace(workspaceId: Int, installedPackageNames: Set<String>) = withContext(Dispatchers.IO) {
        val defaultPackages = listOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.android.vending",
            "com.google.android.dialer",
            "com.google.android.deskclock",
            "com.google.android.camera"
        )

        val metrics = context.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels.toFloat()
        val screenHeightPx = metrics.heightPixels.toFloat()
        val gridCols = 4
        val cellWidthPx = screenWidthPx / gridCols.toFloat()
        val cellHeightPx = (screenHeightPx - 140f * metrics.density) / 6f
        val iconWidthPx = 72f * metrics.density
        val iconHeightPx = 72f * metrics.density

        var cellIndex = 0
        installedPackageNames.forEach { pkg ->
            if (defaultPackages.any { pkg.contains(it) || it.contains(pkg) } || cellIndex < 4) {
                if (cellIndex < 8) {
                    val c = cellIndex % gridCols
                    val r = cellIndex / gridCols
                    val x = c * cellWidthPx + (cellWidthPx - iconWidthPx) / 2f
                    val y = r * cellHeightPx + (cellHeightPx - iconHeightPx) / 2f
                    whitelistedDAO.addWhiteListedApp(
                        WhitelistedApp(
                            workspaceId = workspaceId,
                            packageName = pkg,
                            customIntent = null,
                            position = FreeformPosition(x = x, y = y)
                        )
                    )
                    cellIndex++
                }
            }
        }
    }

    suspend fun deleteOrphanedApps(installedPackageName : Set<String>) = withContext(Dispatchers.IO){
        val allAppsInAllWorkSpaces = workspaceDAO.getAllWhitelistedApps()

        val orphanedApps = allAppsInAllWorkSpaces.filter { it.packageName !in installedPackageName }

        if(orphanedApps.isNotEmpty()){
            workspaceDAO.deleteWhitelistedApps(orphanedApps)
            Log.d("Database Cleanup","All orphaned apps purged across all workspaces")
        }
    }

    suspend fun deleteOrphanedWidgets(installedPackageNames: Set<String>, deleteWidgetIdFromHost: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val allWidgets = widgetDAO.getAllWidgets()
        val orphanedWidgets = allWidgets.filter { it.providerPackage !in installedPackageNames }
        if (orphanedWidgets.isNotEmpty()) {
            orphanedWidgets.forEach { widget ->
                deleteWidgetIdFromHost(widget.appWidgetId)
                widgetDAO.deleteWidgetById(widget.id)
            }
            Log.d("Database Cleanup", "Purged ${orphanedWidgets.size} orphaned widgets across workspaces")
        }
    }

    suspend fun updateAppPosition(workspaceId : Int, packageName: String, x : Float, y: Float) {
        whitelistedDAO.updateAppPosition(workspaceId,packageName,x,y)
    }

    suspend fun updateWorkspaceWallpaper(workspaceId: Int, wallpaperUri: String?) {
        workspaceDAO.updateWallpaper(workspaceId, wallpaperUri)
    }
}
