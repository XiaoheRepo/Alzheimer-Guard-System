package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.AiIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiIntentRepository extends JpaRepository<AiIntentEntity, Long> {

    Optional<AiIntentEntity> findByIntentId(String intentId);
}
