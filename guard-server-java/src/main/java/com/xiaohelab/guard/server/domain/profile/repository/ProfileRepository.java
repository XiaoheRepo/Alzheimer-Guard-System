package com.xiaohelab.guard.server.domain.profile.repository;

import com.xiaohelab.guard.server.domain.patient.entity.PatientEntity;

import java.util.Optional;

/**
 * 患者档案公开查询 Repository（扫码入口视角，只读）。
 * 供公共扫码入口和紧急信息展示使用，由 PatientRepositoryImpl 同时实现此接口。
 * 不包含写操作，写操作通过 PatientRepository 进行。
 */
public interface ProfileRepository {

    /** 按短码查询（QR 码 / NFC 扫码入口） */
    Optional<PatientEntity> findByShortCode(String shortCode);

    /** 按档案编号查询（紧急信息页面） */
    Optional<PatientEntity> findByProfileNo(String profileNo);

    /** 按主键查询 */
    Optional<PatientEntity> findById(Long id);
}
