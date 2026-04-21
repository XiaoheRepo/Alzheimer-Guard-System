package com.xiaohelab.guard.server.ai.service;

import com.xiaohelab.guard.server.ai.entity.AiQuotaLedgerEntity;
import com.xiaohelab.guard.server.ai.repository.AiQuotaLedgerRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * AI 配额账本：USER + PATIENT 双维度。reserve→commit/rollback 语义。
 * 自动建账：缺失时按配置额度初始化。
 */
@Service
public class AiQuotaService {

    private final AiQuotaLedgerRepository ledgerRepository;

    @Value("${guard.ai.user-daily-quota:50}")
    private int userDailyQuota;

    @Value("${guard.ai.patient-daily-quota:100}")
    private int patientDailyQuota;

    public AiQuotaService(AiQuotaLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reserve(Long userId, Long patientId) {
        String period = LocalDate.now().toString();
        AiQuotaLedgerEntity ul = getOrCreate("USER", userId, period, userDailyQuota);
        if (ul.getUsed() + ul.getReserved() + 1 > ul.getQuotaLimit()) {
            throw BizException.of(ErrorCode.E_AI_4292);
        }
        AiQuotaLedgerEntity pl = getOrCreate("PATIENT", patientId, period, patientDailyQuota);
        if (pl.getUsed() + pl.getReserved() + 1 > pl.getQuotaLimit()) {
            throw BizException.of(ErrorCode.E_AI_4293);
        }
        ul.setReserved(ul.getReserved() + 1);
        pl.setReserved(pl.getReserved() + 1);
        ledgerRepository.save(ul);
        ledgerRepository.save(pl);
    }

    @Transactional(rollbackFor = Exception.class)
    public void commit(Long userId, Long patientId) {
        String period = LocalDate.now().toString();
        applyDelta("USER", userId, period, -1, 1);
        applyDelta("PATIENT", patientId, period, -1, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long userId, Long patientId) {
        String period = LocalDate.now().toString();
        applyDelta("USER", userId, period, -1, 0);
        applyDelta("PATIENT", patientId, period, -1, 0);
    }

    private void applyDelta(String type, Long ownerId, String period, int reservedDelta, int usedDelta) {
        AiQuotaLedgerEntity l = ledgerRepository.findByLedgerTypeAndOwnerIdAndPeriod(type, ownerId, period)
                .orElseThrow(() -> BizException.of(ErrorCode.E_SYS_5000, "quota ledger missing"));
        l.setReserved(Math.max(0, l.getReserved() + reservedDelta));
        l.setUsed(l.getUsed() + usedDelta);
        ledgerRepository.save(l);
    }

    private AiQuotaLedgerEntity getOrCreate(String type, Long ownerId, String period, int quotaLimit) {
        return ledgerRepository.findByLedgerTypeAndOwnerIdAndPeriod(type, ownerId, period)
                .orElseGet(() -> {
                    AiQuotaLedgerEntity l = new AiQuotaLedgerEntity();
                    l.setLedgerType(type);
                    l.setOwnerId(ownerId);
                    l.setPeriod(period);
                    l.setQuotaLimit(quotaLimit);
                    return ledgerRepository.save(l);
                });
    }
}
