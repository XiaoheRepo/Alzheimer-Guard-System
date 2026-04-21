package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.PatientMemoryNoteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientMemoryNoteRepository extends JpaRepository<PatientMemoryNoteEntity, Long> {

    Page<PatientMemoryNoteEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Optional<PatientMemoryNoteEntity> findByNoteId(String noteId);
}
