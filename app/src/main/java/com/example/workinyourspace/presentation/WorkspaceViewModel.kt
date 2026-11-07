package com.example.workinyourspace.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workinyourspace.data.local.entity.FreeformPosition
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import com.example.workinyourspace.data.repository.SessionManager
import com.example.workinyourspace.data.repository.WorkspaceRepository
import com.example.workinyourspace.domain.model.AppInfo
import com.example.workinyourspace.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            Toast.makeText(context,"App not installed or present.",Toast.LENGTH_SHORT).show()
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
    val activeWorkspaceId: StateFlow<Int?> = activeWorkspaceID

    fun refreshActiveWorkspace() {
        _activeWorkspaceID.value = sessionMan.getActiveWorkspaceID()
    }

    // Default to true so app always opens into the Workspace Picker Carousel on launch
    private val _isSelectingWorkspace = MutableStateFlow(true)
    val isSelectingWorkspace = _isSelectingWorkspace.asStateFlow()

    fun enterWorkspacePicker() {
        _isSelectingWorkspace.value = true
    }

    fun exitWorkspacePicker() {
        _isSelectingWorkspace.value = false
    }

    fun toggleWorkspacePicker() {
        _isSelectingWorkspace.value = !_isSelectingWorkspace.value
    }

    fun activateWorkspace(id : Int){
        sessionMan.setActiveWorkspaceID(id)
        _activeWorkspaceID.value = id
        _isSelectingWorkspace.value = false
    }

    fun deactivateWorkspace(){
        sessionMan.clearActiveWorkspace()
        _activeWorkspaceID.value = null
    }

    fun addWorkspace(name: String, isDefault: Boolean = false, wallpaperUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val newWorkspace = Workspace(id = 0, name = name, isDefault = isDefault, wallpaperURI = wallpaperUri)
            workspaceRepo.addWorkspace(newWorkspace)
        }
    }

    fun createWorkspace(name: String, wallpaperUri: String? = null) {
        addWorkspace(name = name, isDefault = false, wallpaperUri = wallpaperUri)
    }

    fun deleteWorkspace(workspace: Workspace, appWidgetHost: android.appwidget.AppWidgetHost? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeWorkspaceID.value == workspace.id) {
                deactivateWorkspace()
            }
            if (appWidgetHost != null) {
                val widgets = workspaceRepo.getWorkspaceWidgets(workspace.id).firstOrNull() ?: emptyList()
                widgets.forEach { widget ->
                    try {
                        appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                    } catch (e: Exception) {
                        Log.e("WorkspaceViewModel", "Failed to delete widget ID ${widget.appWidgetId} from host", e)
                    }
                }
            }
            workspaceRepo.deleteWorkspace(workspace)
        }
    }

    fun checkAndCleanupOrphanedWidgets(appWidgetHost: android.appwidget.AppWidgetHost) {
        viewModelScope.launch(Dispatchers.IO) {
            val installedPackageNames = _installedApps.value.map { it.packageName }.toSet()
            if (installedPackageNames.isNotEmpty()) {
                workspaceRepo.deleteOrphanedWidgets(installedPackageNames) { appWidgetId ->
                    try {
                        appWidgetHost.deleteAppWidgetId(appWidgetId)
                    } catch (e: Exception) {
                        Log.e("WorkspaceViewModel", "Failed to delete orphaned widget ID $appWidgetId", e)
                    }
                }
            }
        }
    }

    fun getWhitelistedApps(workspaceId: Int): Flow<List<WhitelistedApp>> {
        return workspaceRepo.getWhitelistedApps(workspaceId)
    }

    fun getWorkspaceWidgets(workspaceId: Int): Flow<List<WorkspaceWidget>> {
        return workspaceRepo.getWorkspaceWidgets(workspaceId)
    }

    fun addWidget(widget: WorkspaceWidget) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.addWidget(widget)
        }
    }

    fun deleteWidgetById(widgetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.deleteWidgetById(widgetId)
        }
    }

    fun updateWidgetPosition(widgetId: Int, x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.updateWidgetPosition(widgetId, x, y)
        }
    }

    fun addWhitelistedApp(workspaceId: Int, packageName: String, x: Float, y: Float, customIntent: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = WhitelistedApp(
                id = 0,
                workspaceId = workspaceId,
                customIntent = customIntent,
                packageName = packageName,
                position = FreeformPosition(x = x, y = y)
            )
            workspaceRepo.addWhitelistedApp(app)
        }
    }

    fun removeWhitelistedApp(app: WhitelistedApp) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.deleteWhitelistedApp(app)
        }
    }

    fun updateWorkspaceWallpaper(workspaceId: Int, wallpaperUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.updateWorkspaceWallpaper(workspaceId, wallpaperUri)
        }
    }

    fun toggleAppInWhitelist(workspaceId: Int, app: AppInfo, isCurrentlyWhitelisted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isCurrentlyWhitelisted) {
                workspaceRepo.removeWhitelistedAppByPackageName(workspaceId, app.packageName)
            } else {
                val newApp = WhitelistedApp(
                    workspaceId = workspaceId,
                    packageName = app.packageName,
                    customIntent = null,
                    position = FreeformPosition(x = -1f, y = -1f)
                )
                workspaceRepo.addWhitelistedApp(newApp)
            }
        }
    }

    fun updateAppPosition(workspaceId: Int, packageName: String, x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.updateAppPosition(workspaceId, packageName, x, y)
        }
    }

    fun removeFromHomeScreen(workspaceId: Int, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            workspaceRepo.updateAppPosition(workspaceId, packageName, -1f, -1f)
        }
    }

    init {
        Log.d("Hello", "Hello Amith!")

        viewModelScope.launch(Dispatchers.IO) {
            val apps = installedAppsRepo.getInstalledApps()
            _installedApps.value = apps

            val installedPackageNames = apps.map { it.packageName }.toSet()
            if (installedPackageNames.isNotEmpty()) {
                workspaceRepo.deleteOrphanedApps(installedPackageNames)
            }

            // Ensure "Home" workspace is ALWAYS created on clean load if empty
            val allWorkspaces = workspaceRepo.getWorkspaces().firstOrNull() ?: emptyList()
            if (allWorkspaces.isEmpty()) {
                workspaceRepo.addWorkspace(Workspace(id = 0, name = "Home", isDefault = true))
            }

            val updatedWorkspaces = workspaceRepo.getWorkspaces().firstOrNull() ?: emptyList()
            val homeWs = updatedWorkspaces.find { it.name.lowercase() == "home" || it.isDefault }
            if (homeWs != null) {
                val homeApps = workspaceRepo.getWhitelistedApps(homeWs.id).firstOrNull() ?: emptyList()
                if (homeApps.isEmpty()) {
                    workspaceRepo.createDefaultHomeWorkspace(homeWs.id, installedPackageNames)
                }
            }
        }

        viewModelScope.launch {
            workspaces.collect { list -> Log.d("DatabaseTest", "Collected workspaces : $list") }
        }
    }
}
