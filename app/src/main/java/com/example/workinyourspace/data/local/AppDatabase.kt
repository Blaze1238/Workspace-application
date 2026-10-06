package com.example.workinyourspace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import com.example.workinyourspace.data.local.entity.Workspace
import com.example.workinyourspace.data.local.entity.WhitelistedApps

@Database(entities = [Workspace::class, WhitelistedApps::class],version=1)
abstract class AppDatabase : RoomDatabase(){
    abstract fun workspaceDAO() : WorkspaceDAO
    abstract fun whitelistedAppsDAO() : WhitelistedAppsDAO
}