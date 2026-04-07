package com.xiaohelab.guard.server.infrastructure.outbox;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysOutboxLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 事件调度器（Outbox Dispatcher）。
 * 轮询 sys_outbox_log 表中 PENDING 记录，抢占租约后投递到 Kafka。
 * HC-02 约束：至少投递一次（at-least-once），消费端通过 consumed_event_log 幂等去重。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final SysOutboxLogMapper sysOutboxLogMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** 每次最多处理条数，防止单批超载 */
    @Value("${guard.outbox.batch-size:50}")
    private int batchSize;

    /** 最大重试次数 */
    @Value("${guard.outbox.max-retry:5}")
    private int maxRetry;

    /** 租约持续时间（秒） */
    @Value("${guard.outbox.lease-seconds:30}")
    private int leaseSeconds;

    /** 当前实例唯一标识（多 Pod 防重复投递） */
    private final String podId = "pod-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * 每 2 秒轮询一次，将 PENDING 记录投递到 Kafka。
     * fixedDelay 保证上一轮完成后再开始下一轮，避免积压时并发。
     */
    @Scheduled(fixedDelay = 2000)
    public void dispatch() {
        // 先将到期的 RETRY 记录重新激活为 PENDING
        sysOutboxLogMapper.reactivateRetry();

        List<SysOutboxLogDO> pending = sysOutboxLogMapper.listPendingOrExpired(batchSize);
        if (pending.isEmpty()) {
            return;
        }

        for (SysOutboxLogDO outboxLog : pending) {
            // 超过最大重试次数，直接置为 DEAD
            if (outboxLog.getRetryCount() != null && outboxLog.getRetryCount() >= maxRetry) {
                sysOutboxLogMapper.markDead(outboxLog.getEventId());
                log.warn("[Outbox] 事件超过最大重试次数，移入死信. eventId={}", outboxLog.getEventId());
                continue;
            }

            // 抢占租约
            outboxLog.setLeaseOwner(podId);
            outboxLog.setLeaseUntil(Instant.now().plusSeconds(leaseSeconds));
            int acquired = sysOutboxLogMapper.tryAcquireLease(outboxLog);
            if (acquired == 0) {
                // 被其他 Pod 抢占，跳过
                continue;
            }

            // 投递到 Kafka
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(outboxLog.getTopic(), outboxLog.getPartitionKey(), outboxLog.getPayload());
                // 通过 Header 透传元信息
                record.headers().add("event_id", outboxLog.getEventId().getBytes());
                record.headers().add("trace_id", outboxLog.getTraceId() != null
                        ? outboxLog.getTraceId().getBytes() : "".getBytes());

                kafkaTemplate.send(record).get(); // 同步等待 ack，确保 at-least-once
                sysOutboxLogMapper.markSent(outboxLog.getEventId(), podId);
                log.debug("[Outbox] 事件投递成功. topic={}, eventId={}", outboxLog.getTopic(), outboxLog.getEventId());
            } catch (Exception ex) {
                log.warn("[Outbox] 事件投递失败，进入重试. eventId={}, error={}",
                        outboxLog.getEventId(), ex.getMessage());
                sysOutboxLogMapper.markRetry(outboxLog.getEventId(), podId, ex.getMessage());
            }
        }
    }
}
