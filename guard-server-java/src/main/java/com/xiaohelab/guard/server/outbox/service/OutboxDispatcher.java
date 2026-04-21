package com.xiaohelab.guard.server.outbox.service;

import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Outbox 调度器：按固定频率抢占 PENDING/RETRY 事件并发布到 Kafka，
 * 成功后置 SENT；失败则指数退避并在超限后进入 DEAD。
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxLogRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${guard.outbox.batch-size:50}")
    private int batchSize;

    @Value("${guard.outbox.max-retry:10}")
    private int maxRetry;

    public OutboxDispatcher(OutboxLogRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${guard.outbox.polling-interval-ms:1000}")
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

        for (OutboxLogEntity event : events) {
            event.setPhase("DISPATCHING");
        }
        repository.saveAll(events);

        for (OutboxLogEntity event : events) {
            dispatchOne(event);
        }
    }

    private void dispatchOne(OutboxLogEntity event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload()).get();
            event.setPhase("SENT");
            event.setSentAt(OffsetDateTime.now());
            repository.save(event);
            log.debug("[Outbox] sent topic={} eventId={}", event.getTopic(), event.getEventId());
        } catch (Exception ex) {
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
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
