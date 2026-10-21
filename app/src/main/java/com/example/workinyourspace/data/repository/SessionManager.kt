package com.example.workinyourspace.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit

class SessionManager @Inject constructor(@ApplicationContext context : Context) {
    private val prefs : SharedPreferences = context.getSharedPreferences(
        "Workspace_prefs", Context.MODE_PRIVATE
    )

    fun setActiveWorkspaceID(id : Int){
        prefs.edit { putInt("ACTIVE_WORKSPACE_ID", id) }
    }

    fun getActiveWorkspaceID() : Int? {
        val id = prefs.getInt("ACTIVE_WORKSPACE_ID",-1)
        return if (id==-1) null else id
    }

    fun clearActiveWorkspace() {
        prefs.edit { remove("ACTIVE_WORKSPACE_ID") }
    }
}