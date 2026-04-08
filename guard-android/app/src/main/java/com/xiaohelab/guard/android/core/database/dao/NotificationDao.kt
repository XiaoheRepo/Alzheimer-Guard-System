package com.xiaohelab.guard.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaohelab.guard.android.core.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE readStatus = 'UNREAD'")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)

    @Query("UPDATE notifications SET readStatus = 'READ' WHERE notificationId = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET readStatus = 'READ'")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE notificationId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
