package com.example.workinyourspace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index

data class GridPosition(
    @ColumnInfo(name = "grid_x") val x : Int,
    @ColumnInfo(name = "grid_y") val y : Int,
)

@Entity(
        tableName = "whitelisted_apps",
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
data class WhitelistedApps(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "whitelistapps_id")
    val id : Int,

    @ColumnInfo(name = "workspace_id")
    val workspaceId : Int,

    @ColumnInfo(name = "custom_intent")
    val customIntent : String?,

    @ColumnInfo(name = "package_name")
    val packageName : String,

    @Embedded
    val position: GridPosition
)
