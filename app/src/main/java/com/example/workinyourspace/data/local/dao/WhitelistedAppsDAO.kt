package com.example.workinyourspace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workinyourspace.data.local.entity.WhitelistedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedAppsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) //This onConflict parameter is a free upsert instead of adding an update
    suspend fun addWhiteListedApp(app: WhitelistedApp)

    @Delete
    suspend fun deleteWhiteListedApp(app: WhitelistedApp)

    @Query("DELETE FROM whitelisted_apps WHERE workspace_id = :workspaceId AND package_name = :packageName")
    suspend fun removeByPackageName(workspaceId : Int, packageName : String)

    @Query("SELECT * FROM whitelisted_apps where workspace_id = :workspaceId")
    fun getWhitelistedApps(workspaceId : Int) : Flow<List<WhitelistedApp>>

    @Query("UPDATE whitelisted_apps SET grid_x = :x, grid_y = :y WHERE workspace_id = :workspaceId AND package_name = :packageName")
    suspend fun updateAppPosition(workspaceId: Int, packageName: String, x: Int, y: Int)
}