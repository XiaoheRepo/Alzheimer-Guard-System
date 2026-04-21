package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSessionRepository extends JpaRepository<AiSessionEntity, Long> {

    Optional<AiSessionEntity> findBySessionId(String sessionId);

    Page<AiSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AiSessionEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Optional<AiSessionEntity> findByPatientIdAndTaskIdAndStatus(Long patientId, Long taskId, String status);
}
