package com.example.workinyourspace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import com.example.workinyourspace.data.local.dao.WorkspaceWidgetDAO
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import com.example.workinyourspace.data.local.entity.WorkspaceWidget

@Database(entities = [Workspace::class, WhitelistedApp::class, WorkspaceWidget::class], version = 3)
abstract class AppDatabase : RoomDatabase(){
    abstract fun workspaceDAO() : WorkspaceDAO
    abstract fun whitelistedAppsDAO() : WhitelistedAppsDAO
    abstract fun workspaceWidgetDAO() : WorkspaceWidgetDAO
}
