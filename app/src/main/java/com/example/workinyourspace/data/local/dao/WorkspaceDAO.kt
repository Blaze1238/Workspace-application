package com.example.workinyourspace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workinyourspace.data.local.entity.Workspace
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) //This onConflict parameter is a free upsert instead of adding an update
    suspend fun addWorkspace(workspace: Workspace)

    @Delete
    suspend fun deleteWorkspace(workspace: Workspace)

    @Query("SELECT * FROM workspace")
    fun getAllWorkspaces() : Flow<List<Workspace>>
}