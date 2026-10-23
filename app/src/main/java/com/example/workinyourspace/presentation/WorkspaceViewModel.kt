package com.example.workinyourspace.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.workinyourspace.data.local.entity.GridPosition
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.repository.SessionManager
import com.example.workinyourspace.data.repository.WorkspaceRepository
import com.example.workinyourspace.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.workinyourspace.domain.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlin.math.log

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepo : WorkspaceRepository,
    private val installedAppsRepo : AppRepository,
    private val sessionMan : SessionManager
) : ViewModel(){

    fun launchApp(packageName : String, context : Context){
        val packageManager = context.packageManager

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if(launchIntent != null){
            context.startActivity(launchIntent)
        }else{
            Toast.makeText(context,"App not intstalled or present.",Toast.LENGTH_SHORT).show()
        }
    }

    val workspaces : StateFlow<List<Workspace>> = workspaceRepo.getWorkspaces().stateIn(
        scope = viewModelScope,
        initialValue = emptyList<Workspace>(),
        started = SharingStarted.WhileSubscribed(5000)
    )

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _activeWorkspaceID = MutableStateFlow<Int?>(sessionMan.getActiveWorkspaceID())
    val activeWorkspaceID = _activeWorkspaceID.asStateFlow()

    fun activateWorkspace(id : Int){
        sessionMan.setActiveWorkspaceID(id)
        _activeWorkspaceID.value = id
    }

    fun deactivateWorkspace(){
        sessionMan.clearActiveWorkspace()
        _activeWorkspaceID.value = null
    }

    fun addWorkspace(newName : String){
        viewModelScope.launch(Dispatchers.IO)  {
            val newWorkspace = Workspace(name = newName)
            workspaceRepo.addWorkspace(newWorkspace)
        }
    }

    fun deleteWorkspace(workspace: Workspace){
        viewModelScope.launch(Dispatchers.IO) {
            if(sessionMan.getActiveWorkspaceID() == workspace.id)
                sessionMan.clearActiveWorkspace()
            workspaceRepo.deleteWorkspace(workspace)
        }
    }

    fun toggleAppInWhitelist(workspaceId : Int, app : AppInfo, isCurrentlyWhitelisted : Boolean){
        viewModelScope.launch(Dispatchers.IO){
            val chosenApp = WhitelistedApp(workspaceId = workspaceId, packageName = app.packageName, customIntent = null, position = GridPosition(-1,-1))  //The position is a dummy since it is allowed but not placed

            if(isCurrentlyWhitelisted){
                workspaceRepo.removeWhitelistedAppByPackageName(chosenApp)
            }else{
                workspaceRepo.addWhitelistedApp(chosenApp)
            }
        }
    }

    fun getWhitelistedApps(workspaceId : Int) : Flow<List<WhitelistedApp>> {
        return workspaceRepo.getWhitelistedApps(workspaceId)
    }

    fun updateAppPosition(workspaceId: Int, packageName: String, x: Int, y: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.updateAppPosition(workspaceId, packageName, x, y)
        }
    }

    //For now this is a test only
    init{
        Log.d("Hello","Hello Amith!")
        viewModelScope.launch {
            val ws = Workspace(id=1,name="Test Gym Workspace")
            workspaceRepo.addWorkspace(ws)
        }

        viewModelScope.launch {
            val apps = installedAppsRepo.getInstalledApps()
            _installedApps.value = apps

            //Test to check if all the apps installed on the user's phone is being returned
            installedApps.value.take(5).forEach { item -> Log.d("AppRegistryTest","AppRegistry test : ${item.name} -> ${item.packageName}") }

            val installedPackageNames = apps.map{ it.packageName }.toSet()

            workspaceRepo.deleteOrphanedApps(installedPackageNames)
        }

        viewModelScope.launch {
            workspaces.collect { list -> Log.d("DatabaseTest","Collected workspaces : $list") }
        }
    }
}