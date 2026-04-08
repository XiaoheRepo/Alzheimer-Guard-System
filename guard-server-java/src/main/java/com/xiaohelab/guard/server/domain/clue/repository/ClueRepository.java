package com.xiaohelab.guard.server.domain.clue.repository;

import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;

import java.util.List;
import java.util.Optional;

/**
 * 线索聚合根 Repository 接口（领域层定义，基础设施层实现）。
 */
public interface ClueRepository {

    Optional<ClueRecordEntity> findById(Long id);

    Optional<ClueRecordEntity> findByClueNo(String clueNo);

    List<ClueRecordEntity> listByTaskId(Long taskId, int limit, int offset);

    long countByTaskId(Long taskId);

    /** 查询任务中待复核（suspect_flag=TRUE 且 PENDING）的线索 */
    List<ClueRecordEntity> listPendingByTaskId(Long taskId);

    /** 管理员复核队列 */
    List<ClueRecordEntity> listReviewQueue(int limit, int offset);

    long countReviewQueue();

    /** 完整字段查询（含 override/reject 审核字段） */
    Optional<ClueRecordEntity> findByIdFull(Long id);

    /**
     * 查找或抛出 BizException（E_CLUE_4043），携带完整字段。
     * 供控制层便捷使用。
     */
    ClueRecordEntity findByIdOrThrow(Long id);

    void insert(ClueRecordEntity entity);

    /** 分配线索给复核员 */
    int assign(Long clueId, Long assigneeUserId);

    /** 管理员 override（强制标记 OVERRIDDEN） */
    int override(Long clueId, Long overrideBy, String overrideReason);

    /** 管理员 reject（标记 REJECTED） */
    int reject(Long clueId, Long rejectedBy, String rejectReason);
    /** 管理端可疑线索列表（suspect_flag=TRUE，带可选过滤） */
    List<ClueRecordEntity> listSuspected(String reviewStatus, Long taskId, Long patientId,
                                         int limit, int offset);

    long countSuspectedFiltered(String reviewStatus, Long taskId, Long patientId);

    /** 统计指标（支持时间范围过滤） */
    long countAll(String timeFrom, String timeTo);

    long countSuspected(String timeFrom, String timeTo);

    long countOverridden(String timeFrom, String timeTo);

    long countRejected(String timeFrom, String timeTo);}
