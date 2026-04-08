package com.xiaohelab.guard.server.domain.task;

import java.util.List;
import java.util.Optional;

/**
 * 寻回任务 Repository 接口（领域层定义，基础设施层实现）。
 */
public interface RescueTaskRepository {

    /** 按 ID 加载聚合根 */
    Optional<RescueTaskEntity> findById(Long id);

    /** 按任务编号加载聚合根 */
    Optional<RescueTaskEntity> findByTaskNo(String taskNo);

    /**
     * 查询患者当前激活任务（同一患者至多 1 个 ACTIVE）。
     * HC-01 约束：创建任务前必须检查此项。
     */
    Optional<RescueTaskEntity> findActiveByPatientId(Long patientId);

    /**
     * 持久化聚合根（新建或更新）。
     * 应用服务在同一事务中调用，写完后必须写 Outbox（HC-02）。
     */
    RescueTaskEntity save(RescueTaskEntity entity);

    /**
     * 条件关闭（WHERE status='ACTIVE' AND event_version=#{version}）。
     * 返回受影响行数；0 表示并发冲突或状态已变更。
     */
    int closeConditionally(RescueTaskEntity entity);

    /** 分页查询患者历史任务 */
    List<RescueTaskEntity> listByPatientId(Long patientId, int limit, int offset);

    /** 统计患者任务总数 */
    long countByPatientId(Long patientId);

    /**
     * 按状态统计任务数（含可选时间窗口过滤）。
     * status 为 null 时统计全部。
     */
    long countByStatus(String status, String timeFrom, String timeTo);

    /** 管理端分页列表全量任务（支持 status/source 过滤） */
    List<RescueTaskEntity> listAll(String status, String source, int limit, int offset);

    /** 管理端全量任务计数 */
    long countAll(String status, String source);

    /**
     * 管理员强制关闭（不使用乐观锁）。
     * 返回受影响行数；0 表示任务不存在或已关闭。
     */
    int forceClose(Long id, String closeReason, String remark);
}
