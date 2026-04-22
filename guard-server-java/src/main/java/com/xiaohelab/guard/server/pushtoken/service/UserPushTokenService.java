package com.xiaohelab.guard.server.pushtoken.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import com.xiaohelab.guard.server.gov.service.AuditLogger;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.pushtoken.dto.PushTokenRegisterRequest;
import com.xiaohelab.guard.server.pushtoken.entity.UserPushTokenEntity;
import com.xiaohelab.guard.server.pushtoken.repository.UserPushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 推送令牌域服务（API §3.8.5、backend_handbook §25.7）。
 * <ul>
 *   <li>注册：upsert on (user_id, device_id)；同设备换绑覆盖；Outbox 发布 user.push_token.registered。</li>
 *   <li>注销：status -> REVOKED + 审计；幂等。</li>
 * </ul>
 */
@Service
public class UserPushTokenService {

    private static final Logger log = LoggerFactory.getLogger(UserPushTokenService.class);

    private final UserPushTokenRepository repo;
    private final OutboxService outboxService;
    private final AuditLogger auditLogger;

    public UserPushTokenService(UserPushTokenRepository repo,
                                OutboxService outboxService,
                                AuditLogger auditLogger) {
        this.repo = repo;
        this.outboxService = outboxService;
        this.auditLogger = auditLogger;
    }

    /** 注册或更新推送令牌；同 (user_id, device_id) 唯一键 upsert。 */
    @Transactional(rollbackFor = Exception.class)
    public UserPushTokenEntity register(Long userId, PushTokenRegisterRequest req) {
        // 1. upsert：若同设备已存在则覆盖 token / status=ACTIVE
        UserPushTokenEntity e = repo.findByUserIdAndDeviceId(userId, req.getDeviceId())
                .orElseGet(UserPushTokenEntity::new);
        boolean isNew = e.getPushTokenId() == null;
        e.setUserId(userId);
        e.setPlatform(req.getPlatform());
        e.setDeviceId(req.getDeviceId());
        e.setPushToken(req.getPushToken());
        e.setAppVersion(req.getAppVersion());
        e.setOsVersion(req.getOsVersion());
        e.setDeviceModel(req.getDeviceModel());
        e.setLocale(req.getLocale() == null ? "zh-CN" : req.getLocale());
        e.setStatus("ACTIVE");
        e.setLastActiveAt(OffsetDateTime.now());
        e.setRevokedAt(null);
        e.setTraceId(TraceIdUtil.currentTraceId());
        e = repo.save(e);

        // 2. Outbox：通知 notification-service 更新分发表
        Map<String, Object> payload = new HashMap<>();
        payload.put("push_token_id", e.getPushTokenId());
        payload.put("user_id", userId);
        payload.put("platform", e.getPlatform());
        payload.put("device_id", e.getDeviceId());
        payload.put("app_version", e.getAppVersion());
        outboxService.publish(OutboxTopics.USER_PUSH_TOKEN_REGISTERED,
                String.valueOf(e.getPushTokenId()), String.valueOf(userId), payload);

        // 3. 审计
        auditLogger.logSuccess("GOV", isNew ? "push.token.register" : "push.token.update",
                String.valueOf(e.getPushTokenId()), "LOW", "CONFIRM_1",
                Map.of("platform", e.getPlatform(), "device_id", e.getDeviceId()));
        log.info("[PushToken] register userId={} push_token_id={} platform={} is_new={}",
                userId, e.getPushTokenId(), e.getPlatform(), isNew);
        return e;
    }

    /** 注销指定推送令牌（限本人）。幂等：已注销则直接返回当前状态。 */
    @Transactional(rollbackFor = Exception.class)
    public UserPushTokenEntity revoke(Long userId, Long pushTokenId) {
        Optional<UserPushTokenEntity> opt = repo.findByPushTokenIdAndUserId(pushTokenId, userId);
        if (opt.isEmpty()) throw BizException.of(ErrorCode.E_USR_4041);
        UserPushTokenEntity e = opt.get();
        if (!"REVOKED".equals(e.getStatus())) {
            e.setStatus("REVOKED");
            e.setRevokedAt(OffsetDateTime.now());
            e = repo.save(e);
            outboxService.publish(OutboxTopics.USER_PUSH_TOKEN_REVOKED,
                    String.valueOf(e.getPushTokenId()), String.valueOf(userId),
                    Map.of("push_token_id", e.getPushTokenId(), "user_id", userId));
            auditLogger.logSuccess("GOV", "push.token.revoke",
                    String.valueOf(e.getPushTokenId()), "LOW", "CONFIRM_1",
                    Map.of("platform", e.getPlatform()));
        }
        return e;
    }

    /** 登出联动：按 push_token_id 幂等注销（内部调用，无权限校验前置条件由外层保证）。 */
    @Transactional(rollbackFor = Exception.class)
    public void revokeByIdForUser(Long userId, Long pushTokenId) {
        if (pushTokenId == null) return;
        repo.findByPushTokenIdAndUserId(pushTokenId, userId).ifPresent(e -> {
            if (!"REVOKED".equals(e.getStatus())) {
                e.setStatus("REVOKED");
                e.setRevokedAt(OffsetDateTime.now());
                repo.save(e);
                outboxService.publish(OutboxTopics.USER_PUSH_TOKEN_REVOKED,
                        String.valueOf(e.getPushTokenId()), String.valueOf(userId),
                        Map.of("push_token_id", e.getPushTokenId(), "user_id", userId,
                                "reason", "LOGOUT_CASCADE"));
            }
        });
    }
}
