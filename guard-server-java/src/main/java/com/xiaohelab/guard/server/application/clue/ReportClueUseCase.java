package com.xiaohelab.guard.server.application.clue;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.domain.clue.repository.ClueRepository;
import com.xiaohelab.guard.server.domain.event.EventTopics;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 线索上报用例（匿名/登录用户均可）。
 * 线索原始事件直写 Kafka（clue.reported.raw，削峰 Intake 链路），不走 Outbox。
 * 这是 LLD §1.4 中描述的"Intake 原始事件"模式。
 */
@Service
@RequiredArgsConstructor
public class ReportClueUseCase {

    private final ClueRepository clueRepository;
    private final RescueTaskRepository rescueTaskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 执行线索上报。
     *
     * @param requestId   幂等键（由调用方提供）
     * @param taskId      关联任务 ID
     * @param submitterId 提交者 ID（匿名时为 null）
     * @param locationLat 纬度（WGS84）
     * @param locationLng 经度（WGS84）
     * @param description 描述文字
     * @param photoUrl    照片地址（单张）
     */
    @Transactional
    public ClueRecordEntity execute(String requestId, Long taskId, Long submitterId,
                                    String submitterInfo, double locationLat, double locationLng,
                                    String description, String photoUrl) {
        // 1. 校验任务是否存在且 ACTIVE
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of("E_TASK_4041"));
        if (!"ACTIVE".equals(task.getStatus())) {
            throw BizException.of("E_CLUE_4091");
        }

        // 2. 写入 clue_record（初始状态 PENDING，等待 AI 研判）
        ClueRecordEntity clue = ClueRecordEntity.create(
                IdGenerator.clueNo(), taskId, task.getPatientId(),
                submitterId == null ? "SCAN" : "MANUAL",
                locationLat, locationLng, description, photoUrl);
        clueRepository.insert(clue);

        // 3. 直接发布 clue.reported.raw 到 Kafka（不走 Outbox，削峰链路）
        String eventPayload = buildPayload(clue);
        ProducerRecord<String, String> record = new ProducerRecord<>(
                EventTopics.TOPIC_CLUE_INTAKE,
                String.valueOf(task.getPatientId()),
                eventPayload);
        record.headers().add("event_type", EventTopics.CLUE_REPORTED_RAW.getBytes());
        record.headers().add("event_id", IdGenerator.eventId().getBytes());
        kafkaTemplate.send(record);  // 异步发送，不等待 ack（削峰场景允许轻微丢失）

        return clue;
    }

    private String buildPayload(ClueRecordEntity clue) {
        return String.format(
                "{\"clue_no\":\"%s\",\"task_id\":%d,\"patient_id\":%d," +
                "\"location_lat\":%f,\"location_lng\":%f,\"description\":\"%s\"}",
                clue.getClueNo(), clue.getTaskId(), clue.getPatientId(),
                clue.getLocationLat(), clue.getLocationLng(),
                clue.getDescription() == null ? "" : clue.getDescription().replace("\"", "\\\""));
    }
}
