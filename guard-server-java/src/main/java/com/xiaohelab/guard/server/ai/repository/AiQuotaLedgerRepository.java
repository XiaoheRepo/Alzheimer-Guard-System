package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.AiQuotaLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiQuotaLedgerRepository extends JpaRepository<AiQuotaLedgerEntity, Long> {

    Optional<AiQuotaLedgerEntity> findByLedgerTypeAndOwnerIdAndPeriod(String ledgerType, Long ownerId, String period);
}
