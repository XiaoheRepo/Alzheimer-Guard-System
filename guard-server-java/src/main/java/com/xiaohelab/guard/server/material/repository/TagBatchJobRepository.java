package com.xiaohelab.guard.server.material.repository;

import com.xiaohelab.guard.server.material.entity.TagBatchJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagBatchJobRepository extends JpaRepository<TagBatchJobEntity, Long> {

    Optional<TagBatchJobEntity> findByJobId(String jobId);

    Page<TagBatchJobEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
