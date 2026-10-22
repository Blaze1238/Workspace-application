package com.example.workinyourspace.hilt

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        //This creates the starting default workspace on boot
        db.execSQL(
            "INSERT INTO workspace (workspace_name, is_default) VALUES ('DEFAULT_WORKSPACE',1)"
        )
    }
}