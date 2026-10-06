package com.example.workinyourspace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workinyourspace.data.local.entity.WhitelistedApps
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedAppsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) //This onConflict parameter is a free upsert instead of adding an update
    suspend fun addWhiteListedApp(app: WhitelistedApps)

    @Delete
    suspend fun deleteWhiteListedApp(app: WhitelistedApps)

    @Query("SELECT * FROM whitelisted_apps where workspace_id = :workspaceId")
    fun getWhitelistedApps(workspaceId : Int) : Flow<List<WhitelistedApps>>
}