package com.xiaohelab.guard.android.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xiaohelab.guard.android.core.database.dao.NotificationDao
import com.xiaohelab.guard.android.core.database.dao.TaskSnapshotDao
import com.xiaohelab.guard.android.core.database.dao.WsEventDedupDao
import com.xiaohelab.guard.android.core.database.entity.NotificationEntity
import com.xiaohelab.guard.android.core.database.entity.TaskSnapshotEntity
import com.xiaohelab.guard.android.core.database.entity.WsEventDedupEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        NotificationEntity::class,
        TaskSnapshotEntity::class,
        WsEventDedupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun taskSnapshotDao(): TaskSnapshotDao
    abstract fun wsEventDedupDao(): WsEventDedupDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "guard_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideTaskSnapshotDao(db: AppDatabase): TaskSnapshotDao = db.taskSnapshotDao()

    @Provides
    fun provideWsEventDedupDao(db: AppDatabase): WsEventDedupDao = db.wsEventDedupDao()
}
