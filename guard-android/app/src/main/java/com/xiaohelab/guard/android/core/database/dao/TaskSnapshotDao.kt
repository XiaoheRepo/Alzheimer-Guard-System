package com.xiaohelab.guard.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaohelab.guard.android.core.database.entity.TaskSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskSnapshotDao {

    @Query("SELECT * FROM task_snapshots ORDER BY latestEventTime DESC")
    fun observeAll(): Flow<List<TaskSnapshotEntity>>

    @Query("SELECT * FROM task_snapshots WHERE status = :status ORDER BY latestEventTime DESC")
    fun observeByStatus(status: String): Flow<List<TaskSnapshotEntity>>

    @Query("SELECT * FROM task_snapshots WHERE taskId = :taskId")
    suspend fun getById(taskId: String): TaskSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskSnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskSnapshotEntity)

    /** 版本守卫：仅当新版本 > 缓存版本时才更新（HandBook §10.1） */
    @Query("UPDATE task_snapshots SET status = :status, version = :version, latestEventTime = :eventTime WHERE taskId = :taskId AND version < :version")
    suspend fun updateStatusIfNewer(taskId: String, status: String, version: Long, eventTime: String): Int

    @Query("DELETE FROM task_snapshots WHERE taskId = :taskId")
    suspend fun delete(taskId: String)

    @Query("DELETE FROM task_snapshots")
    suspend fun deleteAll()
}
