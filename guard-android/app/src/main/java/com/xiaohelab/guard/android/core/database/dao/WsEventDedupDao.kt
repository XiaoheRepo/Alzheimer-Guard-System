package com.xiaohelab.guard.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaohelab.guard.android.core.database.entity.WsEventDedupEntity

@Dao
interface WsEventDedupDao {

    @Query("SELECT COUNT(*) > 0 FROM ws_event_dedup WHERE eventId = :eventId")
    suspend fun contains(eventId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WsEventDedupEntity)

    /** 清理 24 小时前的旧记录，防止无限增长 */
    @Query("DELETE FROM ws_event_dedup WHERE consumedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
