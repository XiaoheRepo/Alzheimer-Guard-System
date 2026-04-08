package com.xiaohelab.guard.server.domain.tag.repository;

import com.xiaohelab.guard.server.domain.tag.entity.TagAssetEntity;

import java.util.List;
import java.util.Optional;

/**
 * 标签资产 Repository 接口（领域层定义，基础设施层实现）。
 * 状态变更操作使用定向 UPDATE（不做 full-save），返回受影响行数供幂等校验。
 */
public interface TagAssetRepository {

    Optional<TagAssetEntity> findById(Long id);

    Optional<TagAssetEntity> findByTagCode(String tagCode);

    Optional<TagAssetEntity> findBoundByPatientId(Long patientId);

    List<TagAssetEntity> listUnbound(int limit, int offset);

    long countUnbound();

    List<TagAssetEntity> listByFilter(String status, Long patientId, int limit, int offset);

    long countByFilter(String status, Long patientId);

    void insert(TagAssetEntity entity);

    /** UNBOUND → ALLOCATED */
    int allocate(Long id, Long applyRecordId);

    /** ALLOCATED/UNBOUND → BOUND */
    int bindToPatient(Long id, Long patientId);

    /** BOUND → LOST */
    int markLost(Long id);

    /** any → VOID */
    int voidTag(Long id, String voidReason);

    /** LOST/VOID → UNBOUND */
    int resetTag(Long id);

    /** ALLOCATED → UNBOUND（管理员释放已分配标签） */
    int releaseByTagCode(String tagCode);

    /** LOST → BOUND（管理员恢复丢失标签） */
    int recover(Long id);
}
