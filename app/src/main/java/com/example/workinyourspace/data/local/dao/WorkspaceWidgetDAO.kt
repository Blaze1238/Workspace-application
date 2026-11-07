package com.example.workinyourspace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.workinyourspace.data.local.entity.WorkspaceWidget
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceWidgetDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWidget(widget: WorkspaceWidget)

    @Delete
    suspend fun deleteWidget(widget: WorkspaceWidget)

    @Query("SELECT * FROM workspace_widgets WHERE workspace_id = :workspaceId")
    fun getWorkspaceWidgets(workspaceId: Int): Flow<List<WorkspaceWidget>>

    @Query("SELECT * FROM workspace_widgets")
    suspend fun getAllWidgets(): List<WorkspaceWidget>

    @Query("DELETE FROM workspace_widgets WHERE widget_id = :widgetId")
    suspend fun deleteWidgetById(widgetId: Int)

    @Query("UPDATE workspace_widgets SET coord_x = :x, coord_y = :y WHERE widget_id = :widgetId")
    suspend fun updateWidgetPosition(widgetId: Int, x: Float, y: Float)
}
