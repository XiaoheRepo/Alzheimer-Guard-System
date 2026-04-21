package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.VectorStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VectorStoreRepository extends JpaRepository<VectorStoreEntity, Long> {

    List<VectorStoreEntity> findByPatientIdAndValidTrueOrderByCreatedAtDesc(Long patientId);

    List<VectorStoreEntity> findBySourceTypeAndSourceIdAndValidTrue(String sourceType, String sourceId);
}
