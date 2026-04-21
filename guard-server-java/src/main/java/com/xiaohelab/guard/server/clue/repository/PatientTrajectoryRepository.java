package com.xiaohelab.guard.server.clue.repository;

import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface PatientTrajectoryRepository extends JpaRepository<PatientTrajectoryEntity, Long> {

    List<PatientTrajectoryEntity> findByTaskIdOrderByWindowStartAsc(Long taskId);

    List<PatientTrajectoryEntity> findByPatientIdAndWindowStartBetweenOrderByWindowStartAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);
}
