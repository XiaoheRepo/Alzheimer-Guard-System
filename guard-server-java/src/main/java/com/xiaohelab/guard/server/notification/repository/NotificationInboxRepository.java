package com.xiaohelab.guard.server.notification.repository;

import com.xiaohelab.guard.server.notification.entity.NotificationInboxEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface NotificationInboxRepository extends JpaRepository<NotificationInboxEntity, Long> {

    Page<NotificationInboxEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationInboxEntity> findByUserIdAndReadStatusOrderByCreatedAtDesc(
            Long userId, String readStatus, Pageable pageable);

    long countByUserIdAndReadStatus(Long userId, String readStatus);

    @Modifying
    @Query("update NotificationInboxEntity n set n.readStatus = 'READ', n.readAt = :at " +
            "where n.userId = :uid and n.notificationId in :ids and n.readStatus = 'UNREAD'")
    int markRead(@Param("uid") Long userId,
                 @Param("ids") java.util.Collection<Long> ids,
                 @Param("at") OffsetDateTime at);
}
