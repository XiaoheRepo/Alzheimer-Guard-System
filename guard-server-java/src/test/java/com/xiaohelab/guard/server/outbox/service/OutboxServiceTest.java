package com.xiaohelab.guard.server.outbox.service;

import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link OutboxService} 单元测试：
 * <ul>
 *   <li>验证事件落库的字段映射；</li>
 *   <li>payload 被包裹为 envelope 并正确 JSON 序列化；</li>
 *   <li>partitionKey 为 null 时回退到 aggregateId。</li>
 * </ul>
 * 注：{@code @Transactional(propagation=MANDATORY)} 的传播行为由 Spring 代理保证，
 * 本测试仅对纯业务逻辑进行断言；传播行为应交由集成测试层（Testcontainers）覆盖。
 */
@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock OutboxLogRepository outboxRepository;
    @InjectMocks OutboxService outboxService;

    @Test
    void publish_should_persist_with_envelope_and_event_id() {
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutboxLogEntity e = outboxService.publish(
                "task.created", "T20250101000001", "P7",
                Map.of("task_id", 100L, "patient_id", 7L));

        assertThat(e.getEventId()).startsWith("E");
        assertThat(e.getTopic()).isEqualTo("task.created");
        assertThat(e.getAggregateId()).isEqualTo("T20250101000001");
        assertThat(e.getPartitionKey()).isEqualTo("P7");
        assertThat(e.getPhase()).isEqualTo("PENDING");
        assertThat(e.getPayload())
                .contains("\"topic\":\"task.created\"")
                .contains("\"task_id\":100")
                .contains("\"event_id\":\"" + e.getEventId() + "\"");
    }

    @Test
    void publish_should_fallback_partition_key_to_aggregate_id_when_null() {
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutboxLogEntity e = outboxService.publish("profile.created", "P7", null, Map.of("x", 1));

        assertThat(e.getPartitionKey()).isEqualTo("P7");
    }

    @Test
    void publish_should_persist_via_repository_save() {
        ArgumentCaptor<OutboxLogEntity> captor = ArgumentCaptor.forClass(OutboxLogEntity.class);
        when(outboxRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        outboxService.publish("tag.bound", "TAG-001", "P7", Map.of("tag", "TAG-001"));

        OutboxLogEntity saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("tag.bound");
        assertThat(saved.getPayload()).contains("\"tag\":\"TAG-001\"");
    }
}
