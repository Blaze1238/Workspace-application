package com.example.workinyourspace.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "workspace")
data class Workspace(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "workspace_id")
    val id : Int,

    @ColumnInfo(name = "workspace_name")
    val name : String,

    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault : Boolean = false,

    @ColumnInfo(name = "wallpaper_uri")
    val wallpaperURI : String? = null
)
