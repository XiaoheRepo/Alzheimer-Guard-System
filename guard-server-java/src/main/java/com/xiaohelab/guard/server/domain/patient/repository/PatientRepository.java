package com.xiaohelab.guard.server.domain.patient.repository;

import com.xiaohelab.guard.server.domain.patient.entity.PatientEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 患者档案 Repository 接口（领域层定义，基础设施层实现）。
 */
public interface PatientRepository {

    Optional<PatientEntity> findById(Long id);

    Optional<PatientEntity> findByShortCode(String shortCode);

    Optional<PatientEntity> findByProfileNo(String profileNo);

    /** 查询用户持有的所有患者档案（通过 sys_user_patient 关联） */
    List<PatientEntity> findByUserId(Long userId);

    /** 插入新档案，回填 id */
    PatientEntity insert(PatientEntity entity);

    /** 全量更新基本资料 */
    PatientEntity update(PatientEntity entity);

    /** 仅更新围栏配置 */
    PatientEntity updateFence(PatientEntity entity);

    /**
     * 事件驱动更新 lostStatus（带 anti-disorder WHERE 条件）。
     * 返回受影响行数；0 表示事件过旧被丢弃。
     */
    int updateLostStatusByEvent(Long id, String lostStatus, Instant eventTime);

    /** 查询下一个 short_code 序列值 */
    long nextShortCodeSeq();
}
