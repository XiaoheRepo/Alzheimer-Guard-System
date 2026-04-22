package com.xiaohelab.guard.server.pushtoken.repository;

import com.xiaohelab.guard.server.pushtoken.entity.UserPushTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPushTokenRepository extends JpaRepository<UserPushTokenEntity, Long> {

    Optional<UserPushTokenEntity> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserPushTokenEntity> findByPushTokenIdAndUserId(Long pushTokenId, Long userId);
}
