package com.xiaohelab.guard.server.outbox.repository;

import com.xiaohelab.guard.server.outbox.entity.ConsumedEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumedEventLogRepository extends JpaRepository<ConsumedEventLogEntity, Long> {

    boolean existsByConsumerNameAndTopicAndEventId(String consumerName, String topic, String eventId);
}
