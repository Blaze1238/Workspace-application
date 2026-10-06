package com.example.workinyourspace.hilt

import com.example.workinyourspace.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.workinyourspace.data.local.dao.WhitelistedAppsDAO
import com.example.workinyourspace.data.local.dao.WorkspaceDAO
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context : Context) : AppDatabase{
        return Room.databaseBuilder(context, AppDatabase :: class.java, "AppDatabase.db").build()
    }

    @Provides
    fun provideWorkspaceDAO(database : AppDatabase) : WorkspaceDAO{
        return database.workspaceDAO()
    }

    @Provides
    fun provideWhitelistedDAO(database : AppDatabase) : WhitelistedAppsDAO{
        return database.whitelistedAppsDAO()
    }
}