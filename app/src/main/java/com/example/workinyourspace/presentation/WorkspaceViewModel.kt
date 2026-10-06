package com.example.workinyourspace.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceViewModel @Inject constructor( private val repo : WorkspaceRepository) : ViewModel(){

    val workspaces : StateFlow<List<Workspace>> = repo.getWorkspaces().stateIn(
        scope = viewModelScope,
        initialValue = emptyList<Workspace>(),
        started = SharingStarted.WhileSubscribed(5000)
    )

    //For now this is a test only
    init{
        Log.d("Hello","Hello Amith!")
        viewModelScope.launch {
            val ws = Workspace(id=1,name="Test Gym Workspace")
            repo.addWorkspace(ws)
        }

        viewModelScope.launch {
            workspaces.collect { list -> Log.d("DatabaseTest","Collected workspaces : $list") }
        }
    }
}