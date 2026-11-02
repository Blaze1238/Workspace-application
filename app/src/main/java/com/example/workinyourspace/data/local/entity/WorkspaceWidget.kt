package com.example.workinyourspace.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspace_widgets",
    foreignKeys = [
        ForeignKey(
            entity = Workspace::class,
            parentColumns = ["workspace_id"],
            childColumns = ["workspace_id"],
            onDelete = CASCADE
        )
    ],
    indices = [Index(value = ["workspace_id"])]
)
data class WorkspaceWidget(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "widget_id")
    val id: Int = 0,

    @ColumnInfo(name = "workspace_id")
    val workspaceId: Int,

    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,

    @ColumnInfo(name = "provider_package")
    val providerPackage: String,

    @ColumnInfo(name = "provider_class")
    val providerClass: String,

    @ColumnInfo(name = "coord_x")
    val x: Float,

    @ColumnInfo(name = "coord_y")
    val y: Float,

    @ColumnInfo(name = "span_x")
    val spanX: Int = 2,

    @ColumnInfo(name = "span_y")
    val spanY: Int = 2
)
