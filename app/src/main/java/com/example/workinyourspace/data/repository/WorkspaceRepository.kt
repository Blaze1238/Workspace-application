package com.example.workinyourspace.data.repository

import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.entity.WhitelistedApps
import com.example.workinyourspace.data.local.entity.Workspace
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkspaceRepository @Inject constructor(private val workspaceDAO : WorkspaceDAO, private val whitelistedDAO : WhitelistedAppsDAO){
    fun getWorkspaces() : Flow<List<Workspace>>{
        return workspaceDAO.getAllWorkspaces()
    }

    fun getWhitelistedApps(workspaceID : Int) : Flow<List<WhitelistedApps>>{
        return whitelistedDAO.getWhitelistedApps(workspaceID)
    }

    //Test function
    suspend fun addWorkspace(workspace: Workspace){
        workspaceDAO.addWorkspace(workspace)
    }
}