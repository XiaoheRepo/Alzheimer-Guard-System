package com.xiaohelab.guard.server.application.clue;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.domain.clue.repository.ClueRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 线索查询与管理服务（只读 + 管理员写操作）。
 * 线索创建由 ReportClueUseCase 负责。
 */
@Service
@RequiredArgsConstructor
public class ClueService {

    private final ClueRepository clueRepository;
    private final SysLogMapper sysLogMapper;

    // ===== 查询 =====

    public List<ClueRecordEntity> listByTask(Long taskId, int pageSize, int offset) {
        return clueRepository.listByTaskId(taskId, pageSize, offset);
    }

    public long countByTask(Long taskId) {
        return clueRepository.countByTaskId(taskId);
    }

    /** 返回任务第一条线索（用于权限校验时获取 patientId）*/
    public Optional<ClueRecordEntity> firstByTask(Long taskId) {
        List<ClueRecordEntity> list = clueRepository.listByTaskId(taskId, 1, 0);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<ClueRecordEntity> listPendingByTask(Long taskId) {
        return clueRepository.listPendingByTaskId(taskId);
    }

    public ClueRecordEntity getById(Long clueId) {
        return clueRepository.findByIdOrThrow(clueId);
    }

    public List<ClueRecordEntity> listReviewQueue(int pageSize, int offset) {
        return clueRepository.listReviewQueue(pageSize, offset);
    }

    public long countReviewQueue() {
        return clueRepository.countReviewQueue();
    }

    // ===== 时间线（来自 sys_log） =====

    public List<SysLogDO> listTimeline(Long clueId, int pageSize, int offset) {
        return sysLogMapper.listByObjectId(String.valueOf(clueId), pageSize, offset);
    }

    public long countTimeline(Long clueId) {
        return sysLogMapper.countByObjectId(String.valueOf(clueId));
    }

    // ===== 管理员写操作 =====

    @Transactional
    public ClueRecordEntity assign(Long clueId, Long assigneeUserId) {
        clueRepository.findByIdOrThrow(clueId);
        int affected = clueRepository.assign(clueId, assigneeUserId);
        if (affected == 0) throw BizException.of("E_CLUE_4093");
        return clueRepository.findByIdOrThrow(clueId);
    }

    @Transactional
    public ClueRecordEntity override(Long clueId, Long overrideBy, String overrideReason) {
        clueRepository.findByIdOrThrow(clueId);
        int affected = clueRepository.override(clueId, overrideBy, overrideReason);
        if (affected == 0) throw BizException.of("E_CLUE_4093");
        return clueRepository.findByIdOrThrow(clueId);
    }

    @Transactional
    public ClueRecordEntity reject(Long clueId, Long rejectedBy, String rejectReason) {
        clueRepository.findByIdOrThrow(clueId);
        int affected = clueRepository.reject(clueId, rejectedBy, rejectReason);
        if (affected == 0) throw BizException.of("E_CLUE_4093");
        return clueRepository.findByIdOrThrow(clueId);
    }

    // ===== 管理员可疑队列 =====

    public List<ClueRecordEntity> listSuspected(String reviewStatus, Long taskId,
                                                 Long patientId, int pageSize, int offset) {
        return clueRepository.listSuspected(reviewStatus, taskId, patientId, pageSize, offset);
    }

    public long countSuspectedFiltered(String reviewStatus, Long taskId, Long patientId) {
        return clueRepository.countSuspectedFiltered(reviewStatus, taskId, patientId);
    }

    // ===== 统计指标 =====

    public long countAll(String timeFrom, String timeTo) {
        return clueRepository.countAll(timeFrom, timeTo);
    }

    public long countSuspected(String timeFrom, String timeTo) {
        return clueRepository.countSuspected(timeFrom, timeTo);
    }

    public long countOverridden(String timeFrom, String timeTo) {
        return clueRepository.countOverridden(timeFrom, timeTo);
    }

    public long countRejected(String timeFrom, String timeTo) {
        return clueRepository.countRejected(timeFrom, timeTo);
    }
}
