package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfileEntity, Long> {

    Optional<PatientProfileEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<PatientProfileEntity> findByDeletedAtIsNull(Pageable pageable);
}
