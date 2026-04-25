package com.xiaohelab.guard.server.pushtoken.service;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import com.xiaohelab.guard.server.pushtoken.entity.UserPushTokenEntity;
import com.xiaohelab.guard.server.pushtoken.repository.UserPushTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 极光推送发送器（backend_handbook §19.5、§14.3）。
 * <p>查 {@code user_push_token} 表所有 ACTIVE token，按平台分组下发：
 * Android 系列（FCM/HMS/MIPUSH）→ android 通道；APNS → ios 通道。</p>
 * <p>失败/未配置：仅记日志，由站内通知做降级（HC-08）。</p>
 */
@Component
@EnableConfigurationProperties(JPushProperties.class)
public class JPushSender {

    private static final Logger log = LoggerFactory.getLogger(JPushSender.class);

    private final JPushProperties props;
    private final UserPushTokenRepository pushTokenRepository;

    @Value("${notification.jpush-enabled:false}")
    private boolean featureEnabled;

    private volatile JPushClient jpush;

    public JPushSender(JPushProperties props, UserPushTokenRepository pushTokenRepository) {
        this.props = props;
        this.pushTokenRepository = pushTokenRepository;
    }

    @PostConstruct
    void init() {
        if (!featureEnabled) {
            log.info("[JPush] notification.jpush-enabled=false，未启用极光推送");
            return;
        }
        if (!props.isEnabled()) {
            log.warn("[JPush] notification.jpush.app-key/master-secret 未配置，推送将走日志降级");
            return;
        }
        try {
            this.jpush = new JPushClient(props.getMasterSecret(), props.getAppKey());
            log.info("[JPush] 客户端初始化完成 apns-production={}", props.isApnsProduction());
        } catch (Exception e) {
            log.error("[JPush] 初始化失败：{}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return featureEnabled && props.isEnabled() && jpush != null;
    }

    /**
     * 推送给指定用户（聚合该用户全部 ACTIVE 设备 token）。
     * @return 实际下发的设备数；失败/降级返回 0
     */
    public int sendToUser(Long userId, String title, String body, Map<String, String> extras) {
        List<UserPushTokenEntity> tokens = pushTokenRepository.findByUserIdAndStatus(userId, "ACTIVE");
        if (tokens.isEmpty()) {
            log.debug("[JPush] user={} 无 ACTIVE 推送 token，跳过", userId);
            return 0;
        }
        if (!isEnabled()) {
            log.info("[JPush] 降级（未启用）：user={} title={} target_devices={}", userId, title, tokens.size());
            return 0;
        }
        List<String> registrationIds = new ArrayList<>(tokens.size());
        boolean hasIos = false;
        boolean hasAndroid = false;
        for (UserPushTokenEntity t : tokens) {
            registrationIds.add(t.getPushToken());
            if ("IOS_APNS".equals(t.getPlatform())) hasIos = true;
            else hasAndroid = true;
        }

        Notification notification = buildNotification(title, body, extras, hasAndroid, hasIos);
        Platform platform;
        if (hasAndroid && hasIos)      platform = Platform.android_ios();
        else if (hasIos)               platform = Platform.ios();
        else                           platform = Platform.android();

        PushPayload payload = PushPayload.newBuilder()
                .setPlatform(platform)
                .setAudience(Audience.registrationId(registrationIds))
                .setNotification(notification)
                .setOptions(Options.newBuilder()
                        .setApnsProduction(props.isApnsProduction())
                        .setTimeToLive(props.getLiveTime())
                        .build())
                .build();

        int attempts = Math.max(1, props.getRetryMax());
        for (int i = 1; i <= attempts; i++) {
            try {
                PushResult res = jpush.sendPush(payload);
                log.info("[JPush] sent user={} msg_id={} sendno={} devices={} attempt={}",
                        userId, res.msg_id, res.sendno, registrationIds.size(), i);
                return registrationIds.size();
            } catch (APIConnectionException e) {
                log.warn("[JPush] 网络异常 attempt={} err={}", i, e.getMessage());
            } catch (APIRequestException e) {
                log.error("[JPush] 接口拒绝 attempt={} status={} err_code={} msg={}",
                        i, e.getStatus(), e.getErrorCode(), e.getErrorMessage());
                if (e.getStatus() >= 400 && e.getStatus() < 500) break; // 4xx 不重试
            } catch (Exception e) {
                log.error("[JPush] 未知异常 attempt={} err={}", i, e.getMessage());
            }
        }
        return 0;
    }

    private Notification buildNotification(String title, String body, Map<String, String> extras,
                                           boolean hasAndroid, boolean hasIos) {
        Notification.Builder nb = Notification.newBuilder();
        if (hasAndroid) {
            AndroidNotification.Builder ab = AndroidNotification.newBuilder()
                    .setAlert(body).setTitle(title);
            if (extras != null) extras.forEach(ab::addExtra);
            nb.addPlatformNotification(ab.build());
        }
        if (hasIos) {
            IosNotification.Builder ib = IosNotification.newBuilder()
                    .setAlert(body).incrBadge(1).setSound("default");
            if (extras != null) extras.forEach(ib::addExtra);
            nb.addPlatformNotification(ib.build());
        }
        return nb.build();
    }
}
