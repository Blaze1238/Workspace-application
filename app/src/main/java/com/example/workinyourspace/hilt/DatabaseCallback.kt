package com.example.workinyourspace.hilt

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // Creates the default starting workspace named 'Home'
        db.execSQL(
            "INSERT INTO workspace (workspace_name, is_default) VALUES ('Home', 1)"
        )
    }
}
