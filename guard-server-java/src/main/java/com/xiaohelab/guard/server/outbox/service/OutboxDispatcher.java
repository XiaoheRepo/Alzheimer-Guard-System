package com.xiaohelab.guard.server.outbox.service;

import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;

/**
 * Outbox 调度器：按固定频率抢占 PENDING/RETRY 事件并发布到 Redis Streams。
 * 依据 BDD §3.3 ADR-006：事件总线使用 Redis Streams（Lettuce），替代 Kafka。
 *
 * Stream Key 格式：stream:{topic}，例如 stream:tag.batch.generated
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    /** Redis Stream Key 前缀，所有 Outbox 事件写入对应 stream */
    private static final String STREAM_KEY_PREFIX = "stream:";

    private final OutboxLogRepository repository;
    private final StringRedisTemplate redisTemplate;

    @Value("${guard.outbox.batch-size:50}")
    private int batchSize;

    @Value("${guard.outbox.max-retry:10}")
    private int maxRetry;

    public OutboxDispatcher(OutboxLogRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelayString = "${guard.outbox.polling-interval-ms:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void poll() {
        List<OutboxLogEntity> events;
        try {
            events = repository.claimPending(batchSize);
        } catch (Exception e) {
            log.debug("[Outbox] claim skipped: {}", e.getMessage());
            return;
        }
        if (events.isEmpty()) return;

        // 1. 批量标记为 DISPATCHING，防止并发抢占
        for (OutboxLogEntity event : events) {
            event.setPhase("DISPATCHING");
        }
        repository.saveAll(events);

        // 2. 逐条发布到 Redis Stream
        for (OutboxLogEntity event : events) {
            dispatchOne(event);
        }
    }

    private void dispatchOne(OutboxLogEntity event) {
        try {
            String streamKey = STREAM_KEY_PREFIX + event.getTopic();
            // Redis Stream 消息体：event_id 作为幂等键，payload 为 JSON 字符串
            Map<String, String> body = Map.of(
                    "event_id", event.getEventId(),
                    "topic", event.getTopic(),
                    "partition_key", event.getPartitionKey(),
                    "payload", event.getPayload()
            );
            MapRecord<String, String, String> record = StreamRecords
                    .newRecord()
                    .in(streamKey)
                    .ofMap(body);
            // 同步写入 Redis Stream；Redis 本地部署，延迟 < 1ms，无需异步
            redisTemplate.opsForStream().add(record);
            event.setPhase("SENT");
            event.setSentAt(OffsetDateTime.now());
            repository.save(event);
            log.debug("[Outbox] sent to stream={} eventId={}", streamKey, event.getEventId());
        } catch (Exception ex) {
            handleDispatchFailure(event, ex);
        }
    }

    private void handleDispatchFailure(OutboxLogEntity event, Throwable ex) {
        int retry = event.getRetryCount() == null ? 0 : event.getRetryCount();
        retry++;
        event.setRetryCount(retry);
        event.setLastError(truncate(ex.getMessage(), 500));
        if (retry >= maxRetry) {
            event.setPhase("DEAD");
        } else {
            event.setPhase("RETRY");
            long backoff = Math.min(60, (long) Math.pow(2, Math.min(retry, 6)));
            event.setNextRetryAt(OffsetDateTime.now().plus(Duration.ofSeconds(backoff)));
        }
        repository.save(event);
        log.warn("[Outbox] dispatch failed topic={} eventId={} retry={} phase={}",
                event.getTopic(), event.getEventId(), retry, event.getPhase());
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
