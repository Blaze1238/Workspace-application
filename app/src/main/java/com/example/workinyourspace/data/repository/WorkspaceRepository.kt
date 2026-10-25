package com.example.workinyourspace.data.repository

import android.util.Log
import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkspaceRepository @Inject constructor(private val workspaceDAO : WorkspaceDAO, private val whitelistedDAO : WhitelistedAppsDAO){
    fun getWorkspaces() : Flow<List<Workspace>>{
        return workspaceDAO.getAllWorkspaces()
    }

    fun getWhitelistedApps(workspaceID : Int) : Flow<List<WhitelistedApp>>{
        return whitelistedDAO.getWhitelistedApps(workspaceID)
    }

    //Test function
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

    suspend fun deleteOrphanedApps(installedPackageName : Set<String>) = withContext(Dispatchers.IO){
        val allAppsInAllWorkSpaces = workspaceDAO.getAllWhitelistedApps()

        val orphanedApps = allAppsInAllWorkSpaces.filter { it.packageName !in installedPackageName }

        if(orphanedApps.isNotEmpty()){
            workspaceDAO.deleteWhitelistedApps(orphanedApps)
            Log.d("Database Cleanup","All orphaned apps purged across all workspaces")
        }
    }

    suspend fun updateAppPosition(workspaceId : Int, packageName: String, x : Float, y: Float) {
        whitelistedDAO.updateAppPosition(workspaceId,packageName,x,y)
    }

    suspend fun updateWorkspaceWallpaper(workspaceId: Int, wallpaperUri: String?) {
        workspaceDAO.updateWallpaper(workspaceId, wallpaperUri)
    }
}